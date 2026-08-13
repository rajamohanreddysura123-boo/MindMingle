package com.rajamohan.mindmingle.presentation.profilesetup.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rajamohan.mindmingle.core.location.LocationService
import com.rajamohan.mindmingle.core.media.FaceDetector
import com.rajamohan.mindmingle.core.media.ImagePicker
import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.OccupationRepository
import com.rajamohan.mindmingle.domain.model.PhoneNumberRules
import com.rajamohan.mindmingle.domain.model.ProfileOptionsRepository
import com.rajamohan.mindmingle.domain.model.User
import com.rajamohan.mindmingle.domain.repository.MindMingleLocalRepository
import com.rajamohan.mindmingle.domain.repository.MindMingleRemoteRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch

internal class ProfileSetupViewModel(
    private val mindMingleLocalRepository: MindMingleLocalRepository,
    private val mindMingleRemoteRepository: MindMingleRemoteRepository
) : ViewModel() {

    private companion object {
        const val TAG = "ProfileSetupViewModel"
    }

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    /** Fields on the existing doc that this form never edits — preserved across a save so an edit never wipes them. */
    private var preservedAvatarUrl: String = ""
    private var preservedIsDisabled: Boolean = false
    private var preservedCreatedAt: Long = 0L
    private var pendingUid: String = ""

    init {
        viewModelScope.launch {
            val occupations = OccupationRepository.getOccupations()
            _uiState.update { it.copy(allOccupations = occupations) }
        }
        viewModelScope.launch {
            val sections = ProfileOptionsRepository.getSections()
            _uiState.update { it.copy(sections = sections) }
        }
        viewModelScope.launch {
            val countries = CountryCodeRepository.getCountryCodes()
            _uiState.update { it.copy(countryCodes = countries) }
        }
    }

    fun onEvent(event: ProfileSetupEvent) {
        when (event) {
            is ProfileSetupEvent.LoadExisting -> loadExisting(event.uid)
            is ProfileSetupEvent.NameChanged -> _uiState.update { it.copy(name = event.name, error = "") }
            is ProfileSetupEvent.PhoneChanged -> _uiState.update { state ->
                val maxLen = PhoneNumberRules.expectedLength(state.selectedCountry.code).last
                if (event.phone.length <= maxLen && event.phone.all(Char::isDigit)) {
                    state.copy(phone = event.phone, error = "")
                } else {
                    state
                }
            }
            is ProfileSetupEvent.CountryCodeSelected -> _uiState.update { state ->
                val maxLen = PhoneNumberRules.expectedLength(event.country.code).last
                state.copy(selectedCountry = event.country, phone = state.phone.take(maxLen), error = "")
            }
            is ProfileSetupEvent.PrefillPhone -> {
                viewModelScope.launch {
                    val countries = _uiState.value.countryCodes.ifEmpty { CountryCodeRepository.getCountryCodes() }
                    val (country, digits) = PhoneNumberRules.parse(event.rawPhoneNumber, countries)
                    _uiState.update { it.copy(selectedCountry = country, phone = digits, error = "") }
                }
            }
            is ProfileSetupEvent.BioChanged -> _uiState.update { it.copy(bio = event.bio) }
            is ProfileSetupEvent.OccupationQueryChanged -> _uiState.update { it.copy(occupationQuery = event.query) }
            is ProfileSetupEvent.OccupationSelected -> _uiState.update {
                it.copy(selectedOccupation = event.occupation, occupationQuery = "", error = "")
            }
            is ProfileSetupEvent.DetailChanged -> {
                _uiState.update { state ->
                    val details = state.details + (event.key to event.value)
                    val derivedAge = if (event.key == ProfileOptionsRepository.DATE_OF_BIRTH_KEY) {
                        ageFromDateOfBirth(event.value)?.toString()
                    } else {
                        null
                    }
                    state.copy(details = details, age = derivedAge ?: state.age, error = "")
                }
            }
            is ProfileSetupEvent.ToggleSelection -> {
                _uiState.update { state ->
                    val current = state.selections[event.key].orEmpty()
                    val field = state.sections.flatMap { it.fields }.firstOrNull { it.key == event.key }
                    val updated = when {
                        current.contains(event.value) -> current - event.value
                        field != null && field.maxSelect > 0 && current.size >= field.maxSelect -> current
                        else -> current + event.value
                    }
                    state.copy(selections = state.selections + (event.key to updated))
                }
            }
            is ProfileSetupEvent.ExperienceSelected -> _uiState.update { it.copy(experienceLevel = event.level, error = "") }
            is ProfileSetupEvent.LookingForQueryChanged -> _uiState.update { it.copy(lookingForQuery = event.query) }
            is ProfileSetupEvent.LookingForSelected -> _uiState.update {
                it.copy(lookingFor = event.option, lookingForQuery = "", error = "")
            }
            is ProfileSetupEvent.PortfolioLinkChanged -> {
                _uiState.update { state ->
                    val updated = state.portfolioLinks.toMutableList()
                    if (event.index < updated.size) updated[event.index] = event.url
                    state.copy(portfolioLinks = updated)
                }
            }
            ProfileSetupEvent.AddPortfolioLink -> {
                _uiState.update { state ->
                    if (state.portfolioLinks.size >= 5) state else state.copy(portfolioLinks = state.portfolioLinks + "")
                }
            }
            is ProfileSetupEvent.RemovePortfolioLink -> {
                _uiState.update { state ->
                    val updated = state.portfolioLinks.toMutableList()
                    if (updated.size > 1) updated.removeAt(event.index) else if (updated.isNotEmpty()) updated[0] = ""
                    state.copy(portfolioLinks = updated)
                }
            }
            ProfileSetupEvent.PickPhotos -> pickPhotos()
            is ProfileSetupEvent.RemovePhoto -> removePhoto(event.index)
            ProfileSetupEvent.DetectLocation -> detectLocation()
            is ProfileSetupEvent.LocationChanged -> _uiState.update {
                // Manual edit invalidates any previously detected coordinates — they'd no longer match.
                it.copy(location = event.location, latitude = null, longitude = null, locationError = "")
            }
            ProfileSetupEvent.NextStep -> _uiState.update {
                if (it.isStepValid(it.currentStep)) {
                    it.copy(currentStep = (it.currentStep + 1).coerceAtMost(ProfileSetupSteps.COUNT - 1))
                } else {
                    it.copy(error = "Please fill in the required fields to continue")
                }
            }
            ProfileSetupEvent.PreviousStep -> _uiState.update {
                it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0), error = "")
            }
            is ProfileSetupEvent.Submit -> submitProfile(uid = event.uid, email = event.email)
        }
    }

    private fun loadExisting(uid: String) {
        pendingUid = uid
        _uiState.update { it.copy(isLoadingExisting = true) }
        viewModelScope.launch {
            val user = mindMingleRemoteRepository.getUser(uid)
            if (user != null) {
                preservedAvatarUrl = user.avatarUrl
                preservedIsDisabled = user.isDisabled
                preservedCreatedAt = user.createdAt
                val links = (listOf(user.githubUrl) + user.portfolioLinks).filter { it.isNotBlank() }
                val countries = _uiState.value.countryCodes.ifEmpty { CountryCodeRepository.getCountryCodes() }
                val (parsedCountry, parsedPhone) = PhoneNumberRules.parse(user.phoneNumber, countries)
                _uiState.update {
                    it.copy(
                        isLoadingExisting = false,
                        name = user.name,
                        phone = parsedPhone,
                        selectedCountry = parsedCountry,
                        age = if (user.age > 0) user.age.toString() else "",
                        bio = user.bio,
                        selectedOccupation = user.occupation,
                        details = user.details,
                        selections = user.selections.ifEmpty {
                            mapOf(ProfileOptionsRepository.INTERESTS_KEY to user.interests)
                        },
                        experienceLevel = user.experienceLevel,
                        lookingFor = user.lookingFor,
                        portfolioLinks = links.ifEmpty { listOf("") },
                        location = user.location,
                        latitude = user.latitude,
                        longitude = user.longitude,
                        photos = user.photoUrls.map { url -> ProfilePhoto(url = url) }
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingExisting = false) }
            }
        }
    }

    private fun detectLocation() {
        _uiState.update { it.copy(isDetectingLocation = true, locationError = "") }
        viewModelScope.launch {
            val result = LocationService.getCurrentLocation()
            val display = result?.displayString?.takeIf { it.isNotBlank() }
            _uiState.update {
                if (display != null) {
                    it.copy(
                        isDetectingLocation = false,
                        location = display,
                        latitude = result.latitude,
                        longitude = result.longitude,
                        locationError = ""
                    )
                } else {
                    it.copy(
                        isDetectingLocation = false,
                        locationError = "Couldn't detect your location automatically — enter it below"
                    )
                }
            }
        }
    }

    private fun pickPhotos() {
        val remaining = 5 - _uiState.value.photos.size
        if (remaining <= 0 || pendingUid.isBlank()) return

        // Picking again is the user's answer to whatever the last attempt complained about —
        // drop the stale message now so a good photo doesn't land under an old face error.
        _uiState.update { it.copy(isPickingPhotos = true, error = "") }
        viewModelScope.launch {
            try {
                val pickedBytesList = ImagePicker.pickImages(remaining)
                if (pickedBytesList.isEmpty()) {
                    _uiState.update { it.copy(isPickingPhotos = false) }
                    return@launch
                }

                // Only photos with a detectable face are allowed on a profile (FaceDetector.android.kt).
                val withFace = pickedBytesList.filter { FaceDetector.containsFace(it) }
                val rejectedCount = pickedBytesList.size - withFace.size

                _uiState.update {
                    it.copy(
                        isPickingPhotos = false,
                        error = if (rejectedCount > 0) {
                            "Couldn't detect a face in $rejectedCount photo${if (rejectedCount > 1) "s" else ""} — only photos with a visible face are allowed"
                        } else {
                            ""
                        }
                    )
                }
                if (withFace.isEmpty()) return@launch

                val startIndex = _uiState.value.photos.size
                _uiState.update { it.copy(photos = it.photos + withFace.map { bytes -> ProfilePhoto(localBytes = bytes, isUploading = true) }) }

                withFace.forEachIndexed { i, bytes ->
                    val targetIndex = startIndex + i
                    val result = mindMingleRemoteRepository.uploadUserPhoto(pendingUid, bytes)
                    _uiState.update { state ->
                        val updated = state.photos.toMutableList()
                        if (targetIndex < updated.size) {
                            result.onSuccess { url ->
                                updated[targetIndex] = ProfilePhoto(url = url, isUploading = false)
                            }.onFailure {
                                updated.removeAt(targetIndex)
                            }
                        }
                        state.copy(
                            photos = updated,
                            error = if (result.isFailure) "Couldn't upload one of the photos" else state.error
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.e(throwable = e, tag = TAG) { "pickPhotos failed" }
                // Leave no photo stuck mid-upload, or the Next button stays disabled forever.
                _uiState.update { state ->
                    state.copy(
                        isPickingPhotos = false,
                        photos = state.photos.filterNot { it.isUploading },
                        error = e.message ?: "Couldn't add the photo"
                    )
                }
            }
        }
    }

    private fun removePhoto(index: Int) {
        val photo = _uiState.value.photos.getOrNull(index) ?: return
        _uiState.update { state -> state.copy(photos = state.photos.filterIndexed { i, _ -> i != index }) }
        photo.url?.let { url ->
            viewModelScope.launch {
                mindMingleRemoteRepository.deleteUserPhoto(url)
            }
        }
    }

    private fun submitProfile(uid: String, email: String) {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update {
                it.copy(error = "Add at least one photo, and fill your name, phone, age, occupation, experience level and what you're looking for")
            }
            return
        }
        if (state.photos.any { it.isUploading }) {
            _uiState.update { it.copy(error = "Please wait for your photos to finish uploading") }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = "") }

        viewModelScope.launch {
            val links = state.portfolioLinks.map { it.trim() }.filter { it.isNotBlank() }
            val user = User(
                uid = uid,
                phoneNumber = "${state.selectedCountry.dialCode} ${state.phone}",
                name = state.name,
                email = email,
                avatarUrl = preservedAvatarUrl,
                photoUrls = state.photos.mapNotNull { it.url },
                bio = state.bio,
                occupation = state.selectedOccupation,
                interests = state.selectedInterests.toList(),
                details = state.details.filterValues { it.isNotBlank() },
                selections = state.selections.filterValues { it.isNotEmpty() },
                experienceLevel = state.experienceLevel,
                lookingFor = state.lookingFor,
                githubUrl = links.firstOrNull().orEmpty(),
                portfolioLinks = links.drop(1),
                age = state.ageValue ?: 0,
                location = state.location,
                latitude = state.latitude,
                longitude = state.longitude,
                isProfileComplete = true,
                isDisabled = preservedIsDisabled,
                createdAt = preservedCreatedAt
            )

            val result = mindMingleRemoteRepository.saveUser(user)
            result.onSuccess {
                mindMingleLocalRepository.saveUserSession(user)
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = "Could not save your profile: ${throwable.message ?: "unknown error"}"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
internal fun ageFromDateOfBirth(dateOfBirth: String): Int? {
    val parts = dateOfBirth.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null

    val days = Clock.System.now().toEpochMilliseconds().floorDiv(86_400_000L)
    val (todayYear, todayMonth, todayDay) = civilFromDays(days)

    var age = todayYear - year
    if (todayMonth < month || (todayMonth == month && todayDay < day)) age--
    return age.takeIf { it in 0..120 }
}

private fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    return Triple((if (m <= 2) y + 1 else y).toInt(), m.toInt(), d.toInt())
}
