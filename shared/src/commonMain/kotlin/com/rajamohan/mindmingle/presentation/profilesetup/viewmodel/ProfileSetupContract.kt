package com.rajamohan.mindmingle.presentation.profilesetup.viewmodel

import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.CountryCodeRepository
import com.rajamohan.mindmingle.domain.model.OccupationRepository
import com.rajamohan.mindmingle.domain.model.PhoneNumberRules
import com.rajamohan.mindmingle.domain.model.ProfileOptionsRepository
import com.rajamohan.mindmingle.domain.model.ProfileSection

internal sealed class ProfileSetupEvent {
    data class LoadExisting(val uid: String) : ProfileSetupEvent()
    data class NameChanged(val name: String) : ProfileSetupEvent()
    data class PhoneChanged(val phone: String) : ProfileSetupEvent()
    data class CountryCodeSelected(val country: CountryCode) : ProfileSetupEvent()
    data class PrefillPhone(val rawPhoneNumber: String) : ProfileSetupEvent()
    data class BioChanged(val bio: String) : ProfileSetupEvent()
    data class OccupationQueryChanged(val query: String) : ProfileSetupEvent()
    data class OccupationSelected(val occupation: String) : ProfileSetupEvent()
    data class DetailChanged(val key: String, val value: String) : ProfileSetupEvent()
    data class ToggleSelection(val key: String, val value: String) : ProfileSetupEvent()
    data class ExperienceSelected(val level: String) : ProfileSetupEvent()
    data class LookingForQueryChanged(val query: String) : ProfileSetupEvent()
    data class LookingForSelected(val option: String) : ProfileSetupEvent()
    data class PortfolioLinkChanged(val index: Int, val url: String) : ProfileSetupEvent()
    data object AddPortfolioLink : ProfileSetupEvent()
    data class RemovePortfolioLink(val index: Int) : ProfileSetupEvent()
    data object PickPhotos : ProfileSetupEvent()
    data class RemovePhoto(val index: Int) : ProfileSetupEvent()
    data object DetectLocation : ProfileSetupEvent()
    data class LocationChanged(val location: String) : ProfileSetupEvent()
    data object NextStep : ProfileSetupEvent()
    data object PreviousStep : ProfileSetupEvent()
    data class Submit(val uid: String, val email: String) : ProfileSetupEvent()
}

// Public (not internal): rendered by the shared PhotoPickerRow widget in another package.
data class ProfilePhoto(
    val localBytes: ByteArray? = null,
    val url: String? = null,
    val isUploading: Boolean = false
)

/** Field keys from profile_options.json that render inside the Basics step instead of the catch-all Details step. */
private val basicsSectionKeys = setOf("dateOfBirth", "gender", "pronouns", "languages")

/** Field keys from profile_options.json that render inside the Work step instead of the catch-all Details step. */
private val workSectionKeys = setOf("company", "education", "college")

/** Field keys from profile_options.json that render inside the Preferences step instead of the catch-all Details step. */
private val preferencesSectionKeys = setOf(
    "interestedIn", "preferredMinAge", "preferredMaxAge", "preferredDistanceKm",
    "preferredLocation", "relationshipIntention", "openToLongDistance", "openToRelocation"
)

internal data class ProfileSetupUiState(
    val name: String = "",
    val phone: String = "",
    val selectedCountry: CountryCode = CountryCode.defaultIndia,
    val countryCodes: List<CountryCode> = CountryCodeRepository.fallbackList(),
    val age: String = "",
    val bio: String = "",
    val occupationQuery: String = "",
    val selectedOccupation: String = "",
    val allOccupations: List<String> = emptyList(),
    val sections: List<ProfileSection> = emptyList(),
    val details: Map<String, String> = emptyMap(),
    val selections: Map<String, List<String>> = emptyMap(),
    val experienceLevel: String = "",
    val lookingForQuery: String = "",
    val lookingFor: String = "",
    val portfolioLinks: List<String> = listOf(""),
    val location: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDetectingLocation: Boolean = false,
    val locationError: String = "",
    val photos: List<ProfilePhoto> = emptyList(),
    val isPickingPhotos: Boolean = false,
    val isLoadingExisting: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String = "",
    val currentStep: Int = 0
) {
    val occupationResults: List<String>
        get() = if (occupationQuery.isBlank()) {
            OccupationRepository.defaultShortlist
        } else {
            allOccupations.filter { it.contains(occupationQuery, ignoreCase = true) }.take(30)
        }

    val lookingForResults: List<String>
        get() = if (lookingForQuery.isBlank()) {
            OccupationRepository.defaultShortlist
        } else {
            allOccupations.filter { it.contains(lookingForQuery, ignoreCase = true) }.take(30)
        }

    val selectedInterests: Set<String>
        get() = selections[ProfileOptionsRepository.INTERESTS_KEY].orEmpty().toSet()

    val basicsSections: List<ProfileSection> get() = sectionsMatching(basicsSectionKeys)
    val workSections: List<ProfileSection> get() = sectionsMatching(workSectionKeys)
    val preferencesSections: List<ProfileSection> get() = sectionsMatching(preferencesSectionKeys)
    val detailsSections: List<ProfileSection>
        get() {
            val claimed = basicsSectionKeys + workSectionKeys + preferencesSectionKeys
            return sections.mapNotNull { section ->
                val remaining = section.fields.filterNot { it.key in claimed }
                if (remaining.isEmpty()) null else section.copy(fields = remaining)
            }
        }

    private fun sectionsMatching(keys: Set<String>): List<ProfileSection> =
        sections.mapNotNull { section ->
            val matched = section.fields.filter { it.key in keys }
            if (matched.isEmpty()) null else section.copy(fields = matched)
        }

    val ageValue: Int? get() = age.toIntOrNull()

    val isBasicsStepValid: Boolean
        get() = name.isNotBlank() &&
            PhoneNumberRules.isValidLength(selectedCountry.code, phone.length) &&
            (ageValue != null && ageValue!! > 0)

    val isWorkStepValid: Boolean
        get() = selectedOccupation.isNotBlank()

    val isPreferencesStepValid: Boolean
        get() = experienceLevel.isNotBlank() && lookingFor.isNotBlank()

    val isPhotosStepValid: Boolean
        get() = photos.isNotEmpty() && !isPickingPhotos && photos.none { it.isUploading }

    fun isStepValid(step: Int): Boolean = when (step) {
        ProfileSetupSteps.PHOTOS -> isPhotosStepValid
        ProfileSetupSteps.BASICS -> isBasicsStepValid
        ProfileSetupSteps.WORK -> isWorkStepValid
        ProfileSetupSteps.PREFERENCES -> isPreferencesStepValid
        else -> true
    }

    val isValid: Boolean
        get() = isPhotosStepValid && isBasicsStepValid && isWorkStepValid && isPreferencesStepValid
}

internal object ProfileSetupSteps {
    const val PHOTOS = 0
    const val BASICS = 1
    const val WORK = 2
    const val PREFERENCES = 3
    const val DETAILS = 4
    const val FINISH = 5
    const val COUNT = 6
    val titles = listOf("Photos", "Basics", "Work", "Preferences", "About You", "Finish")
}

internal object ProfileSetupOptions {
    val experienceLevels = listOf("Junior (0-2 yrs)", "Mid (2-5 yrs)", "Senior (5-8 yrs)", "Staff+ (8+ yrs)")
    val lookingForOptions = listOf("Collaborator", "Co-founder", "Mentor / Mentee", "Networking", "Relationship")
    val interests = listOf(
        "Travel", "Music", "Gaming", "Fitness", "Reading", "Photography",
        "Cooking", "Hiking", "Movies & TV", "Art", "Coffee", "Pets",
        "Yoga", "Startups", "Open Source", "Board Games", "Cycling", "Anime"
    )
}
