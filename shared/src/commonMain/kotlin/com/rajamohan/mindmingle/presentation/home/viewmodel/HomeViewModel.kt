package com.rajamohan.mindmingle.presentation.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.core.ads.AdsPlatform
import com.rajamohan.mindmingle.domain.model.DiscoverFilterSeed
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.model.bannerUnitIdForPlatform
import com.rajamohan.mindmingle.domain.model.interstitialUnitIdForPlatform
import com.rajamohan.mindmingle.domain.model.isPremiumNow
import com.rajamohan.mindmingle.domain.model.isServableHere
import com.rajamohan.mindmingle.domain.usecase.GetAdConfigUseCase
import com.rajamohan.mindmingle.domain.usecase.GetAdminAreasUseCase
import com.rajamohan.mindmingle.domain.usecase.GetDiscoverProfilesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.LikeUserUseCase
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.usecase.ObserveSubscriptionUseCase
import com.rajamohan.mindmingle.domain.usecase.RefreshMyLocationUseCase
import com.rajamohan.mindmingle.domain.usecase.SetPremiumFlagUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class HomeViewModel(
    private val getDiscoverProfilesUseCase: GetDiscoverProfilesUseCase,
    private val likeUserUseCase: LikeUserUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getAdConfigUseCase: GetAdConfigUseCase,
    private val observeSubscriptionUseCase: ObserveSubscriptionUseCase,
    private val setPremiumFlagUseCase: SetPremiumFlagUseCase,
    private val refreshMyLocationUseCase: RefreshMyLocationUseCase,
    private val getAdminAreasUseCase: GetAdminAreasUseCase,
    private val mindMingleLocalRepository: MindMingleLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Profiles swiped since the last ad slot — drives the "every Nth profile" cadence. */
    private var swipesSinceAd = 0

    /** Where the server-side Discover scan left off; blank once it has reached the end. */
    private var discoverCursor: String = ""

    private var isLoadingMoreProfiles = false

    /** Ad slots served this session; every Nth one is upgraded to an interstitial. */
    private var adSlotsServed = 0

    private var adCountdownJob: Job? = null

    private var subscriptionJob: Job? = null

    /**
     * The two halves of the premium badge, each filled in by whichever load finishes first:
     * what our own profile doc currently advertises, and what the subscription doc actually says.
     * The mirror runs when they disagree — see [mirrorPremiumFlag].
     */
    private var advertisedPremium: Boolean? = null
    private var actualPremium: Boolean? = null

    /** Set on every LoadProfiles — filter apply/reset re-query the same uid without the caller repeating it. */
    private var currentUid: String = ""

    /**
     * Every uid the deck has loaded this session. It is what separates "new person" from "person
     * we are showing again": a re-query at the end of the deck only counts as new arrivals the
     * profiles that are not in here.
     */
    private val seenUids = mutableSetOf<String>()

    /**
     * Uids liked this session. These are never recycled — the conversation is already open, so
     * putting them back in the deck would only invite a second, pointless like.
     */
    private val connectedUids = mutableSetOf<String>()

    private companion object {
        /** Cards left in the deck before the next page is fetched. */
        const val DECK_REFILL_THRESHOLD = 5
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadProfiles -> loadProfiles(event.uid)
            is HomeEvent.Connect -> connect(event.fromUid, event.toUid)
            is HomeEvent.Pass -> advance()
            is HomeEvent.ApplyFilters -> {
                // Filters now apply when the sheet closes rather than on a button press, so this
                // fires on every close — including the ones that changed nothing. Re-querying then
                // would spend reads to rebuild the identical deck.
                if (event.filters != _uiState.value.filters) {
                    _uiState.update { it.copy(filters = event.filters, currentIndex = 0, isRecycledDeck = false) }
                    persistFilters(event.filters)
                    fetchDiscoverProfiles(currentUid, event.filters)
                }
            }
            is HomeEvent.ResetFilters -> {
                _uiState.update { it.copy(filters = DiscoverFilters(), currentIndex = 0, isRecycledDeck = false) }
                // Stored as an explicit empty set, not cleared — a deliberate reset must not
                // get re-seeded from the user's profile preferences on the next launch.
                persistFilters(DiscoverFilters())
                fetchDiscoverProfiles(currentUid, DiscoverFilters())
            }
            is HomeEvent.DismissAd -> clearAdGate()
        }
    }

    private fun loadProfiles(uid: String) {
        currentUid = uid
        viewModelScope.launch {
            fetchDiscoverProfiles(uid, resolveStartingFilters(uid))
        }

        // MindMingle+ is the ad kill switch. It streams rather than loads once, so a live upgrade
        // clears the deck's ads without a restart. The plan doc is server-written only
        // (firestore.rules), so this cannot be faked client-side.
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            observeSubscriptionUseCase(uid).collect { subscription ->
                val isPremium = subscription.isPremiumNow()
                _uiState.update { it.copy(isPremium = isPremium) }
                actualPremium = isPremium
                mirrorPremiumFlag(uid)
                if (isPremium) {
                    clearAdGate()
                } else {
                    dropPremiumFilters(uid)
                }
            }
        }

        // Ad settings load on their own coroutine so a slow/absent config doc never delays Discover.
        viewModelScope.launch {
            val config = getAdConfigUseCase()
            _uiState.update { it.copy(adConfig = config) }
            if (config.isServableHere() && AdsPlatform.isSupported && !_uiState.value.isPremium) {
                AdsPlatform.initialize()
            }
        }
    }

    /**
     * The filter set the deck opens with: whatever was last applied on this device, or — on a
     * first run — one seeded from the user's own Dating Preferences answers.
     */
    private suspend fun resolveStartingFilters(uid: String): DiscoverFilters {
        val stored = mindMingleLocalRepository.getDiscoverFilters()
        if (stored != null) {
            val filters = stored.toFilters()
            _uiState.update { it.copy(filters = filters) }
            return filters
        }

        val me = getUserProfileUseCase(uid) ?: return _uiState.value.filters
        advertisedPremium = me.isPremium
        mirrorPremiumFlag(uid)
        val seeded = DiscoverFilterSeed.from(me).toFilters()
        _uiState.update { it.copy(filters = seeded) }
        return seeded
    }

    /**
     * Publishes our own MindMingle+ state onto our profile doc, which is where everyone else's
     * client reads the badge from: subscriptions/{uid} is owner-only, so a viewer has no other way
     * to know. It writes only on a real disagreement — an unconditional write would cost one
     * update per launch — and both inputs must be known first, since a null is "not loaded yet",
     * not "false", and acting on one would clear a paying user's badge on every cold start.
     */
    private fun mirrorPremiumFlag(uid: String) {
        val advertised = advertisedPremium ?: return
        val actual = actualPremium ?: return
        if (advertised == actual) return

        viewModelScope.launch {
            setPremiumFlagUseCase(uid, actual).onSuccess { advertisedPremium = actual }
        }
    }

    /**
     * District names for one country, for the filter sheet.
     *
     * Cached per country for the life of the view model: the sheet reloads them every time it
     * opens, and a list of district names does not change between two openings of a dialog.
     */
    suspend fun districtsFor(countryCode: String): List<String> {
        districtCache[countryCode]?.let { return it }
        val areas = getAdminAreasUseCase(countryCode).districts
        districtCache[countryCode] = areas
        return areas
    }

    private val districtCache = mutableMapOf<String, List<String>>()

    /**
     * A lapsed or revoked subscription must not leave premium filters silently narrowing the deck —
     * the sheet stops offering them, so the user would have no way to clear them by hand.
     */
    private fun dropPremiumFilters(uid: String) {
        val current = _uiState.value.filters
        val downgraded = current.withoutPremiumFilters()
        if (downgraded == current) return

        _uiState.update { it.copy(filters = downgraded, currentIndex = 0) }
        persistFilters(downgraded)
        fetchDiscoverProfiles(uid, downgraded)
    }

    private fun persistFilters(filters: DiscoverFilters) {
        viewModelScope.launch {
            mindMingleLocalRepository.saveDiscoverFilters(filters.toCriteria())
        }
    }

    /** Runs [filters] against the `filterDiscoverProfiles` cloud function — matching happens server-side, not on-device. */
    private fun fetchDiscoverProfiles(uid: String, filters: DiscoverFilters) {
        if (uid.isBlank()) return
        _uiState.update { it.copy(isLoading = true, error = "") }
        viewModelScope.launch {
            val page = getDiscoverProfilesUseCase(uid, filters.toCriteria())
            // Own coordinates — needed to evaluate the distance filter against candidates.
            val me = getUserProfileUseCase(uid)
            if (me != null) {
                advertisedPremium = me.isPremium
                mirrorPremiumFlag(uid)
            }

            // Where this user is, refreshed from the device if the stored fix has gone stale or
            // they have moved. It runs off the profile just read, so it costs no extra read, and
            // it writes only when something displayed would actually change.
            val myLocation = refreshMyLocationUseCase(me)

            discoverCursor = page.cursor
            seenUids.clear()
            page.profiles.mapTo(seenUids) { profile -> profile.uid }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    allProfiles = page.profiles,
                    isRecycledDeck = false,
                    myLatitude = myLocation?.first,
                    myLongitude = myLocation?.second,
                    currentIndex = 0
                )
            }
        }
    }

    /**
     * Pulls the next page as the deck runs low. The feed arrives a deck at a time so that one
     * Discover session costs a bounded number of reads instead of one per user in the app; this
     * is what keeps swiping continuous despite that.
     */
    private fun loadMoreProfilesIfNeeded() {
        val state = _uiState.value
        val remaining = state.allProfiles.size - state.currentIndex
        if (remaining > DECK_REFILL_THRESHOLD || isLoadingMoreProfiles) return
        // A blank cursor means the scan reached the end — the next fresh fetch starts over.
        if (discoverCursor.isBlank()) return

        val uid = currentUid
        if (uid.isBlank()) return

        isLoadingMoreProfiles = true
        viewModelScope.launch {
            val page = getDiscoverProfilesUseCase(
                excludeUid = uid,
                filters = _uiState.value.filters.toCriteria(),
                cursor = discoverCursor
            )
            discoverCursor = page.cursor

            // Someone already in the deck can come back on a later page after a wrap-around.
            val known = _uiState.value.allProfiles.mapTo(mutableSetOf()) { it.uid }
            val fresh = page.profiles.filterNot { it.uid in known }
            fresh.mapTo(seenUids) { profile -> profile.uid }

            _uiState.update { it.copy(allProfiles = it.allProfiles + fresh) }
            isLoadingMoreProfiles = false

            // The page may have been the last one and may have been empty; if the user is already
            // sitting at the end of the deck, that is the moment to start over.
            recycleIfExhausted()
        }
    }

    /**
     * What happens when the deck runs out.
     *
     * New people always win: while the server still has pages, or a re-query turns up anyone this
     * session has not already loaded, those are shown and nothing is repeated. Only once the
     * server has genuinely nothing unseen left does the deck start over from the people already
     * shown — an empty deck that stays empty is worse than a second look at the same profiles.
     *
     * Anyone already connected with is dropped from the recycled deck, and the order is reshuffled
     * so the second pass does not replay the first in sequence.
     */
    private fun recycleIfExhausted() {
        val state = _uiState.value
        if (state.currentIndex < state.allProfiles.size) return
        if (isLoadingMoreProfiles) return

        // Still pages to scan — loadMoreProfilesIfNeeded will bring them, and they are new people.
        if (discoverCursor.isNotBlank()) return

        val uid = currentUid
        if (uid.isBlank()) return

        isLoadingMoreProfiles = true
        viewModelScope.launch {
            // One last look from the top of the scan: someone may have joined, or completed their
            // profile, since this session started. They are new and take priority over a repeat.
            val page = getDiscoverProfilesUseCase(uid, _uiState.value.filters.toCriteria())
            discoverCursor = page.cursor

            val arrivals = page.profiles.filterNot { it.uid in seenUids }
            if (arrivals.isNotEmpty()) {
                arrivals.mapTo(seenUids) { profile -> profile.uid }
                _uiState.update { it.copy(allProfiles = it.allProfiles + arrivals) }
                isLoadingMoreProfiles = false
                return@launch
            }

            val recyclable = _uiState.value.allProfiles.filterNot { it.uid in connectedUids }
            if (recyclable.isEmpty()) {
                // Everyone in the deck was connected with, so there is nothing honest to show.
                isLoadingMoreProfiles = false
                return@launch
            }

            _uiState.update {
                it.copy(allProfiles = recyclable.shuffled(), currentIndex = 0, isRecycledDeck = true)
            }
            isLoadingMoreProfiles = false
        }
    }

    /**
     * A like opens the conversation straight away — the other side does not have to like back.
     * The deck moves on without interrupting the user.
     */
    private fun connect(fromUid: String, toUid: String) {
        connectedUids += toUid
        viewModelScope.launch {
            likeUserUseCase(fromUid, toUid)
        }
        advance()
    }

    private fun advance() {
        _uiState.update { it.copy(currentIndex = it.currentIndex + 1) }
        loadMoreProfilesIfNeeded()
        recycleIfExhausted()

        swipesSinceAd++
        if (!isAdDue()) return

        armAdSlot()
    }

    private fun isAdDue(): Boolean {
        val state = _uiState.value
        if (state.adGate != null) return false
        // Paying for MindMingle+ buys an ad-free deck; no slot is ever armed while the plan is active.
        if (state.isPremium) return false
        if (!state.adConfig.isServableHere() || !AdsPlatform.isSupported) return false
        // Nothing left to gate — never strand the user on an ad at the end of the deck.
        if (!state.hasMoreProfiles) return false
        return swipesSinceAd >= state.adConfig.swipesPerAd
    }

    /**
     * Puts an ad in the deck's card slot. Banner slots hold for [AdConfig.gateSeconds] and then
     * unlock a Continue button; every Nth slot is a full-screen interstitial instead, which
     * unlocks as soon as the SDK returns.
     */
    private fun armAdSlot() {
        val config = _uiState.value.adConfig
        swipesSinceAd = 0
        adSlotsServed++

        val isInterstitial = adSlotsServed % config.adSlotsPerInterstitial == 0

        if (isInterstitial) {
            val unitId = config.interstitialUnitIdForPlatform(AdsPlatform.isIos)
            _uiState.update {
                it.copy(adGate = AdGate(kind = AdSlotKind.INTERSTITIAL, unitId = unitId))
            }
            adCountdownJob?.cancel()
            adCountdownJob = viewModelScope.launch {
                val shown = AdsPlatform.showInterstitial(unitId)
                // Either way the user regains the deck: dismissed ad, or no fill at all.
                if (!shown) {
                    _uiState.update { state ->
                        state.copy(adGate = state.adGate?.copy(failed = true))
                    }
                }
                clearAdGate()
            }
            return
        }

        val unitId = config.bannerUnitIdForPlatform(AdsPlatform.isIos)
        _uiState.update {
            it.copy(
                adGate = AdGate(
                    kind = AdSlotKind.BANNER,
                    unitId = unitId,
                    secondsLeft = config.gateSeconds
                )
            )
        }

        adCountdownJob?.cancel()
        adCountdownJob = viewModelScope.launch {
            var remaining = config.gateSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _uiState.update { state ->
                    val gate = state.adGate ?: return@update state
                    state.copy(adGate = gate.copy(secondsLeft = remaining))
                }
            }
        }
    }

    private fun clearAdGate() {
        adCountdownJob?.cancel()
        adCountdownJob = null
        _uiState.update { it.copy(adGate = null) }
    }

    override fun onCleared() {
        adCountdownJob?.cancel()
        subscriptionJob?.cancel()
        super.onCleared()
    }
}
