package com.rajamohan.mindmingle.presentation.profilesetup.component.mobile

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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.core.media.MAX_PROFILE_PHOTOS
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.home.component.mobile.DesktopBreakpoint
import com.rajamohan.mindmingle.presentation.profilesetup.component.desktop.DesktopProfileSetupScreen
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

/**
 * Profile form shared by first-run setup and later edits: a guided step wizard.
 * Photos are optional (up to 5); matching is driven by occupation,
 * experience level and what you're looking for.
 */
@Composable
fun ProfileSetupScreen(
    uid: String,
    email: String,
    prefillName: String = "",
    isEditMode: Boolean = false,
    onBack: () -> Unit = {},
    onProfileSaved: (name: String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= DesktopBreakpoint) {
            DesktopProfileSetupScreen(
                uid = uid,
                email = email,
                prefillName = prefillName,
                isEditMode = isEditMode,
                onBack = onBack,
                onProfileSaved = onProfileSaved
            )
        } else {
            MobileProfileSetupScreen(
                uid = uid,
                email = email,
                prefillName = prefillName,
                isEditMode = isEditMode,
                onBack = onBack,
                onProfileSaved = onProfileSaved
            )
        }
    }
}

@Composable
private fun MobileProfileSetupScreen(
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
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = { if (uiState.currentStep == 0) onBack() else viewModel.onEvent(ProfileSetupEvent.PreviousStep) },
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = if (isEditMode) "Edit Your Profile" else "Build Your Profile",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            WizardProgressBar(
                currentStep = uiState.currentStep,
                totalSteps = ProfileSetupSteps.COUNT,
                stepTitles = ProfileSetupSteps.titles
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                        ProfileSetupSteps.PHOTOS -> PhotosStep(uiState, viewModel)
                        ProfileSetupSteps.BASICS -> BasicsStep(uiState, viewModel)
                        ProfileSetupSteps.WORK -> WorkStep(uiState, viewModel)
                        ProfileSetupSteps.PREFERENCES -> PreferencesStep(uiState, viewModel)
                        ProfileSetupSteps.DETAILS -> DetailsStep(uiState, viewModel)
                        else -> FinishStep(uiState, viewModel)
                    }
                }
            }

            if (uiState.error.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = uiState.error, style = typography.bodySmall, color = colors.error)
            }

            // Why Next is greyed out — the required fields are usually scrolled off the top.
            val missingHint = uiState.missingHintFor(uiState.currentStep)
            if (missingHint.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = missingHint, style = typography.bodySmall, color = colors.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

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
        }
    }
}

@Composable
private fun PhotosStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    StepHeading(title = "Add Your Photos", subtitle = "At least one photo is required — up to $MAX_PROFILE_PHOTOS. Each one must clearly show your face.")
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
private fun BasicsStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    StepHeading(title = "The Basics", subtitle = "How should people recognize and reach you?")
    Spacer(modifier = Modifier.height(20.dp))

    SectionLabel("Your Name")
    OutlinedField(value = uiState.name, placeholder = "Full name", onValueChange = { viewModel.onEvent(ProfileSetupEvent.NameChanged(it)) })

    Spacer(modifier = Modifier.height(20.dp))

    SectionLabel("Mobile Number")
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
private fun WorkStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    StepHeading(title = "What You Do", subtitle = "Your occupation is what we match you on.")
    Spacer(modifier = Modifier.height(20.dp))

    SectionLabel("Occupation")
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
private fun PreferencesStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    StepHeading(title = "Your Preferences", subtitle = "This is what we match you on.")
    Spacer(modifier = Modifier.height(20.dp))

    SectionLabel("Experience Level")
    Spacer(modifier = Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileSetupOptions.experienceLevels.forEach { level ->
            SelectableRow(label = level, isSelected = uiState.experienceLevel == level, onClick = { viewModel.onEvent(ProfileSetupEvent.ExperienceSelected(level)) })
        }
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionLabel("Looking For")
    Spacer(modifier = Modifier.height(10.dp))
    OccupationPickerField(
        query = uiState.lookingForQuery,
        selected = uiState.lookingFor,
        results = uiState.lookingForResults,
        onQueryChanged = { viewModel.onEvent(ProfileSetupEvent.LookingForQueryChanged(it)) },
        onSelect = { viewModel.onEvent(ProfileSetupEvent.LookingForSelected(it)) },
        placeholder = "Search occupation"
    )

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
private fun DetailsStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    StepHeading(title = "About You", subtitle = "Basics, lifestyle, what you're looking for, interests and values.")
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
private fun FinishStep(uiState: ProfileSetupUiState, viewModel: ProfileSetupViewModel) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    StepHeading(title = "Almost Done", subtitle = "Where you're based, a short bio, and any links worth sharing.")
    Spacer(modifier = Modifier.height(20.dp))

    SectionLabel("Location")
    Spacer(modifier = Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedField(value = uiState.location, placeholder = "City, Country", onValueChange = { viewModel.onEvent(ProfileSetupEvent.LocationChanged(it)) })
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

    if (uiState.locationError.isNotBlank()) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = uiState.locationError, style = typography.bodySmall, color = colors.error)
    } else if (uiState.latitude != null && uiState.longitude != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Precise location detected — used for distance filtering", style = typography.bodySmall, color = colors.tertiary)
    }

    Spacer(modifier = Modifier.height(22.dp))

    SectionLabel("Short Bio (optional)")
    OutlinedField(
        value = uiState.bio,
        placeholder = "What excites you? What are you looking to build or share?",
        minHeight = 90.dp,
        onValueChange = { viewModel.onEvent(ProfileSetupEvent.BioChanged(it)) }
    )

    Spacer(modifier = Modifier.height(22.dp))

    SectionLabel("Portfolio Links (optional)")
    Spacer(modifier = Modifier.height(10.dp))
    PortfolioLinksSection(
        links = uiState.portfolioLinks,
        onLinkChanged = { index, url -> viewModel.onEvent(ProfileSetupEvent.PortfolioLinkChanged(index, url)) },
        onAdd = { viewModel.onEvent(ProfileSetupEvent.AddPortfolioLink) },
        onRemove = { index -> viewModel.onEvent(ProfileSetupEvent.RemovePortfolioLink(index)) }
    )
}

@Composable
private fun StepHeading(title: String, subtitle: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(text = title, style = typography.headlineSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = subtitle, style = typography.bodyMedium, color = colors.onSurfaceVariant, lineHeight = 20.sp)
}

@Composable
private fun SectionLabel(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Text(
        text = text,
        style = typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = colors.onBackground
    )
}

@Composable
private fun OutlinedField(
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
                    Text(
                        text = placeholder,
                        style = typography.bodyMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                inner()
            }
        )
    }
}

@Composable
private fun SelectableRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) colors.primaryContainer.copy(alpha = 0.6f) else colors.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Box(
                modifier = Modifier
                    .height(20.dp)
                    .widthIn(min = 20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.primary else colors.surfaceVariant)
            )
        }
    }
}
