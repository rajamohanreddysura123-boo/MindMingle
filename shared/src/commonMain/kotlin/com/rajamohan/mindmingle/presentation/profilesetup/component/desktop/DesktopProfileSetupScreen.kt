package com.rajamohan.mindmingle.presentation.profilesetup.component.desktop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.core.media.MAX_PROFILE_PHOTOS
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.home.component.mobile.avatarGradientFor
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.OccupationPickerField
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.PhoneNumberField
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.ProfileDetailSections
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.PhotoPickerRow
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.PortfolioLinksSection
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.WizardNavRow
import com.rajamohan.mindmingle.presentation.profilesetup.component.shared.WizardProgressBar
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupEvent
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupOptions
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupSteps
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupUiState
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfileSetupViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/** Two-column desktop layout: a step wizard on the left, a live Discover-card preview on the right that updates across every step. */
@Composable
fun DesktopProfileSetupScreen(
    uid: String,
    email: String,
    prefillName: String = "",
    isEditMode: Boolean = false,
    onBack: () -> Unit = {},
    onProfileSaved: (name: String) -> Unit
) {
    val viewModel: ProfileSetupViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    LaunchedEffect(Unit) {
        viewModel.onEvent(ProfileSetupEvent.LoadExisting(uid))
        if (!isEditMode) {
            if (prefillName.isNotBlank()) viewModel.onEvent(ProfileSetupEvent.NameChanged(prefillName))
            viewModel.onEvent(ProfileSetupEvent.DetectLocation)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onProfileSaved(uiState.name)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.desktopScreenPadding, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isEditMode) {
                    Surface(onClick = onBack, shape = CircleShape, color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
                Text(
                    text = if (isEditMode) "Edit Your Profile" else "Build Your Profile",
                    style = typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 1100.dp)
                    .padding(horizontal = Spacing.desktopScreenPadding),
                horizontalArrangement = Arrangement.spacedBy(48.dp)
            ) {
                // Left: step wizard
                Column(modifier = Modifier.weight(1.3f).fillMaxSize()) {
                    WizardProgressBar(
                        currentStep = uiState.currentStep,
                        totalSteps = ProfileSetupSteps.COUNT,
                        stepTitles = ProfileSetupSteps.titles
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    AnimatedContent(
                        targetState = uiState.currentStep,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(tween(220)) { it / 3 }) togetherWith (slideOutHorizontally(tween(220)) { -it / 3 })
                            } else {
                                (slideInHorizontally(tween(220)) { -it / 3 }) togetherWith (slideOutHorizontally(tween(220)) { it / 3 })
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { step ->
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            when (step) {
                                ProfileSetupSteps.PHOTOS -> DesktopPhotosStep(uiState, viewModel)
                                ProfileSetupSteps.BASICS -> DesktopBasicsStep(uiState, viewModel)
                                ProfileSetupSteps.WORK -> DesktopWorkStep(uiState, viewModel)
                                ProfileSetupSteps.PREFERENCES -> DesktopPreferencesStep(uiState, viewModel)
                                ProfileSetupSteps.DETAILS -> DesktopDetailsStep(uiState, viewModel)
                                else -> DesktopFinishStep(uiState, viewModel)
                            }
                        }
                    }

                    if (uiState.error.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = uiState.error, style = typography.bodySmall, color = colors.error)
                    }

                    // Why Next is greyed out. The required fields are often scrolled off the top
                    // by the time someone reaches the button.
                    val missingHint = uiState.missingHintFor(uiState.currentStep)
                    if (missingHint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = missingHint, style = typography.bodySmall, color = colors.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    WizardNavRow(
                        isFirstStep = uiState.currentStep == 0,
                        isLastStep = uiState.currentStep == ProfileSetupSteps.FINISH,
                        isNextEnabled = uiState.isStepValid(uiState.currentStep),
                        isSaving = uiState.isSaving,
                        submitLabel = if (isEditMode) "Save Changes" else "Complete Profile",
                        onBack = { viewModel.onEvent(ProfileSetupEvent.PreviousStep) },
                        onNext = { viewModel.onEvent(ProfileSetupEvent.NextStep) },
                        onSubmit = { viewModel.onEvent(ProfileSetupEvent.Submit(uid = uid, email = email)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Right: live preview of how this profile looks in Discover, visible through every step
                Column(modifier = Modifier.weight(0.9f).widthIn(min = 280.dp)) {
                    Text(text = "Preview", style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            // The live preview is only worth having if it shows the photo that was
                            // just uploaded — otherwise it previews everything except the part
                            // people actually judge.
                            RemoteProfileImage(
                                url = uiState.photos.firstOrNull()?.url.orEmpty(),
                                uid = uid,
                                contentDescription = "Preview photo",
                                placeholderIconSize = 34.dp,
                                modifier = Modifier.fillMaxWidth().height(140.dp)
                            )
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(text = uiState.name.ifBlank { "Your name" }, style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                                Text(
                                    text = listOf(uiState.selectedOccupation.ifBlank { "Occupation" }, uiState.age.takeIf { it.isNotBlank() })
                                        .filterNotNull().joinToString(" • "),
                                    style = typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                                if (uiState.location.isNotBlank()) {
                                    Text(text = uiState.location, style = typography.labelSmall, color = colors.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = uiState.bio.ifBlank { "Your bio will show up here." },
                                    style = typography.bodySmall,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopPhotosStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    DesktopStepHeading(title = "Add Your Photos", subtitle = "At least one photo is required — up to $MAX_PROFILE_PHOTOS. Each one must clearly show your face.")
    Spacer(modifier = Modifier.height(20.dp))
    PhotoPickerRow(
        photos = uiState.photos,
        isPicking = uiState.isPickingPhotos,
        onAddClick = { viewModel.onEvent(ProfileSetupEvent.PickPhotos) },
        onRemove = { index -> viewModel.onEvent(ProfileSetupEvent.RemovePhoto(index)) }
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "We check every upload for a visible face — photos without one are rejected automatically.",
        style = typography.bodySmall,
        color = colors.onSurfaceVariant,
        lineHeight = 18.sp
    )
}

@Composable
private fun DesktopBasicsStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    DesktopStepHeading(title = "The Basics", subtitle = "How should people recognize and reach you?")
    Spacer(modifier = Modifier.height(20.dp))

    DesktopSectionLabel("Your Name")
    DesktopOutlinedField(value = uiState.name, placeholder = "Full name", onValueChange = { viewModel.onEvent(ProfileSetupEvent.NameChanged(it)) })

    Spacer(modifier = Modifier.height(20.dp))

    DesktopSectionLabel("Mobile Number")
    PhoneNumberField(
        phone = uiState.phone,
        selectedCountry = uiState.selectedCountry,
        countries = uiState.countryCodes,
        onPhoneChanged = { viewModel.onEvent(ProfileSetupEvent.PhoneChanged(it)) },
        onCountrySelected = { viewModel.onEvent(ProfileSetupEvent.CountryCodeSelected(it)) }
    )

    Spacer(modifier = Modifier.height(20.dp))

    ProfileDetailSections(
        sections = uiState.basicsSections,
        details = uiState.details,
        selections = uiState.selections,
        onDetailChanged = { key, value -> viewModel.onEvent(ProfileSetupEvent.DetailChanged(key, value)) },
        onToggleSelection = { key, value -> viewModel.onEvent(ProfileSetupEvent.ToggleSelection(key, value)) }
    )
}

@Composable
private fun DesktopWorkStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    DesktopStepHeading(title = "What You Do", subtitle = "Your occupation is what we match you on.")
    Spacer(modifier = Modifier.height(20.dp))

    DesktopSectionLabel("Occupation")
    Spacer(modifier = Modifier.height(10.dp))
    OccupationPickerField(
        query = uiState.occupationQuery,
        selected = uiState.selectedOccupation,
        results = uiState.occupationResults,
        onQueryChanged = { viewModel.onEvent(ProfileSetupEvent.OccupationQueryChanged(it)) },
        onSelect = { viewModel.onEvent(ProfileSetupEvent.OccupationSelected(it)) }
    )

    Spacer(modifier = Modifier.height(22.dp))

    ProfileDetailSections(
        sections = uiState.workSections,
        details = uiState.details,
        selections = uiState.selections,
        onDetailChanged = { key, value -> viewModel.onEvent(ProfileSetupEvent.DetailChanged(key, value)) },
        onToggleSelection = { key, value -> viewModel.onEvent(ProfileSetupEvent.ToggleSelection(key, value)) }
    )
}

@Composable
private fun DesktopPreferencesStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    DesktopStepHeading(title = "Your Preferences", subtitle = "This is what we match you on.")
    Spacer(modifier = Modifier.height(20.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            DesktopSectionLabel("Experience Level")
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileSetupOptions.experienceLevels.forEach { level ->
                    DesktopSelectableRow(label = level, isSelected = uiState.experienceLevel == level) {
                        viewModel.onEvent(ProfileSetupEvent.ExperienceSelected(level))
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            DesktopSectionLabel("Looking For")
            Spacer(modifier = Modifier.height(10.dp))
            OccupationPickerField(
                query = uiState.lookingForQuery,
                selected = uiState.lookingFor,
                results = uiState.lookingForResults,
                onQueryChanged = { viewModel.onEvent(ProfileSetupEvent.LookingForQueryChanged(it)) },
                onSelect = { viewModel.onEvent(ProfileSetupEvent.LookingForSelected(it)) },
                placeholder = "Search occupation"
            )
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    ProfileDetailSections(
        sections = uiState.preferencesSections,
        details = uiState.details,
        selections = uiState.selections,
        onDetailChanged = { key, value -> viewModel.onEvent(ProfileSetupEvent.DetailChanged(key, value)) },
        onToggleSelection = { key, value -> viewModel.onEvent(ProfileSetupEvent.ToggleSelection(key, value)) }
    )
}

@Composable
private fun DesktopDetailsStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    DesktopStepHeading(title = "About You", subtitle = "Basics, lifestyle, what you're looking for, interests and values.")
    Spacer(modifier = Modifier.height(20.dp))

    ProfileDetailSections(
        sections = uiState.detailsSections,
        details = uiState.details,
        selections = uiState.selections,
        onDetailChanged = { key, value -> viewModel.onEvent(ProfileSetupEvent.DetailChanged(key, value)) },
        onToggleSelection = { key, value -> viewModel.onEvent(ProfileSetupEvent.ToggleSelection(key, value)) }
    )
}

@Composable
private fun DesktopFinishStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    DesktopStepHeading(title = "Almost Done", subtitle = "Where you're based, a short bio, and any links worth sharing.")
    Spacer(modifier = Modifier.height(20.dp))

    DesktopSectionLabel("Location")
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            DesktopOutlinedField(value = uiState.location, placeholder = "City, Country", onValueChange = { viewModel.onEvent(ProfileSetupEvent.LocationChanged(it)) })
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            onClick = { viewModel.onEvent(ProfileSetupEvent.DetectLocation) },
            enabled = !uiState.isDetectingLocation,
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.height(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                if (uiState.isDetectingLocation) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(text = "Detect", style = typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    DesktopSectionLabel("Short Bio (optional)")
    DesktopOutlinedField(
        value = uiState.bio,
        placeholder = "What excites you? What are you looking to build or share?",
        minHeight = 90.dp,
        onValueChange = { viewModel.onEvent(ProfileSetupEvent.BioChanged(it)) }
    )

    Spacer(modifier = Modifier.height(22.dp))

    DesktopSectionLabel("Portfolio Links (optional)")
    Spacer(modifier = Modifier.height(10.dp))
    PortfolioLinksSection(
        links = uiState.portfolioLinks,
        onLinkChanged = { index, url -> viewModel.onEvent(ProfileSetupEvent.PortfolioLinkChanged(index, url)) },
        onAdd = { viewModel.onEvent(ProfileSetupEvent.AddPortfolioLink) },
        onRemove = { index -> viewModel.onEvent(ProfileSetupEvent.RemovePortfolioLink(index)) }
    )
}

@Composable
private fun DesktopStepHeading(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(text = title, style = typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = subtitle, style = typography.bodyMedium, color = colors.onSurfaceVariant, lineHeight = 20.sp)
}

@Composable
private fun DesktopSectionLabel(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(text = text, style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
}

@Composable
private fun DesktopOutlinedField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    minHeight: Dp = 52.dp,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(text = placeholder, style = typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                }
                inner()
            }
        )
    }
}

@Composable
private fun DesktopSelectableRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colors.primaryContainer.copy(alpha = 0.6f) else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Box(modifier = Modifier.height(20.dp).widthIn(min = 20.dp).clip(CircleShape).background(if (isSelected) colors.primary else colors.surfaceVariant))
        }
    }
}
