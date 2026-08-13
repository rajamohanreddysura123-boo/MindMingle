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
import com.rajamohan.mindmingle.domain.usecase.GetDiscoverProfilesUseCase
import com.rajamohan.mindmingle.domain.usecase.GetUserProfileUseCase
import com.rajamohan.mindmingle.domain.usecase.LikeUserUseCase
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.usecase.ObserveSubscriptionUseCase
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
    private val mindMingleLocalRepository: MindMingleLocalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** Profiles swiped since the last ad slot — drives the "every Nth profile" cadence. */
    private var swipesSinceAd = 0

    /** Ad slots served this session; every Nth one is upgraded to an interstitial. */
    private var adSlotsServed = 0

    /** An ad owed to the user but held back until the match dialog is dismissed. */
    private var adPendingAfterMatch = false

    private var adCountdownJob: Job? = null

    private var subscriptionJob: Job? = null

    /** Set on every LoadProfiles — filter apply/reset re-query the same uid without the caller repeating it. */
    private var currentUid: String = ""

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadProfiles -> loadProfiles(event.uid)
            is HomeEvent.Connect -> connect(event.fromUid, event.toUid)
            is HomeEvent.Pass -> advance()
            is HomeEvent.DismissMatch -> {
                _uiState.update { it.copy(matchedUser = null) }
                // Two full-screen surfaces at once reads as a broken app; the ad waits its turn.
                if (adPendingAfterMatch) {
                    adPendingAfterMatch = false
                    armAdSlot()
                }
            }
            is HomeEvent.ApplyFilters -> {
                _uiState.update { it.copy(filters = event.filters, currentIndex = 0, matchedUser = null) }
                persistFilters(event.filters)
                fetchDiscoverProfiles(currentUid, event.filters)
            }
            is HomeEvent.ResetFilters -> {
                _uiState.update { it.copy(filters = DiscoverFilters(), currentIndex = 0, matchedUser = null) }
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
        val seeded = DiscoverFilterSeed.from(me).toFilters()
        _uiState.update { it.copy(filters = seeded) }
        return seeded
    }

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
            val profiles = getDiscoverProfilesUseCase(uid, filters.toCriteria())
            // Own coordinates — needed to evaluate the distance filter against candidates.
            val me = getUserProfileUseCase(uid)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    allProfiles = profiles,
                    myLatitude = me?.latitude,
                    myLongitude = me?.longitude,
                    currentIndex = 0
                )
            }
        }
    }

    private fun connect(fromUid: String, toUid: String) {
        viewModelScope.launch {
            val isMatch = likeUserUseCase(fromUid, toUid)
            val matched = if (isMatch) _uiState.value.currentProfile else null
            advance(matchedUser = matched)
        }
    }

    private fun advance(matchedUser: User? = null) {
        _uiState.update { it.copy(currentIndex = it.currentIndex + 1, matchedUser = matchedUser) }

        swipesSinceAd++
        if (!isAdDue()) return

        if (matchedUser != null) {
            adPendingAfterMatch = true
        } else {
            armAdSlot()
        }
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
