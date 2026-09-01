package com.rajamohan.mindmingle.presentation.profilesetup.component.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.core.media.MAX_PROFILE_PHOTOS
import com.rajamohan.mindmingle.core.media.decodeToImageBitmapOrNull
import com.rajamohan.mindmingle.core.media.fetchImageBytes
import com.rajamohan.mindmingle.domain.model.CountryCode
import com.rajamohan.mindmingle.domain.model.PhoneNumberRules
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.ChevronDownIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.profilesetup.viewmodel.ProfilePhoto

/** Segmented step progress bar: filled up to and including the current step. */
@Composable
fun WizardProgressBar(currentStep: Int, totalSteps: Int, stepTitles: List<String>) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(totalSteps) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (index <= currentStep) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Step ${currentStep + 1} of $totalSteps",
                style = typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stepTitles.getOrNull(currentStep).orEmpty(),
                style = typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

/** Back / Next / Submit navigation row pinned under a wizard step's content. */
@Composable
fun WizardNavRow(
    isFirstStep: Boolean,
    isLastStep: Boolean,
    isNextEnabled: Boolean,
    isSaving: Boolean,
    submitLabel: String,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (!isFirstStep) {
            Surface(
                onClick = onBack,
                shape = RoundedCornerShape(50),
                color = colors.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(56.dp).width(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }

        // A blocked step (photo still uploading, required field empty) has to *look* blocked —
        // an always-primary button reads as tappable even while it ignores taps.
        val isActionEnabled = if (isLastStep) !isSaving else isNextEnabled

        Surface(
            onClick = if (isLastStep) onSubmit else onNext,
            enabled = isActionEnabled,
            shape = RoundedCornerShape(50),
            color = if (isActionEnabled) colors.primary else colors.outlineVariant.copy(alpha = 0.4f),
            shadowElevation = if (isActionEnabled) 6.dp else 0.dp,
            modifier = Modifier.weight(1f).height(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isLastStep && isSaving) {
                    CircularProgressIndicator(color = colors.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (isLastStep) submitLabel else "Next",
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isActionEnabled) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Up-to-[MAX_PROFILE_PHOTOS]-photo row: existing thumbnails (local bytes or fetched-by-url) plus a
 * trailing "add" tile.
 *
 * It scrolls horizontally, and has to: five 84dp tiles and the add tile come to over 550dp, so on
 * any phone the last two sat off the edge of a plain Row with no way to reach them — a photo could
 * be uploaded and then be impossible to remove. The scroll is edge-to-edge with the padding moved
 * inside it, so a thumbnail can sit under the screen margin while scrolling instead of being
 * clipped by it.
 */
@Composable
fun PhotoPickerRow(
    photos: List<ProfilePhoto>,
    isPicking: Boolean,
    onAddClick: () -> Unit,
    onRemove: (index: Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "${photos.size} of $MAX_PROFILE_PHOTOS added",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            photos.forEachIndexed { index, photo ->
                Box(modifier = Modifier.size(84.dp)) {
                    PhotoThumbnail(photo = photo)
                    if (!photo.isUploading) {
                        Surface(
                            onClick = { onRemove(index) },
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.size(22.dp).align(Alignment.TopEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CrossIcon(color = Color.White, modifier = Modifier.size(10.dp))
                            }
                        }
                    } else {
                        Box(modifier = Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            if (photos.size < MAX_PROFILE_PHOTOS) {
                Surface(
                    onClick = onAddClick,
                    enabled = !isPicking,
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isPicking) {
                            CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        } else {
                            Text(text = "+", style = MaterialTheme.typography.headlineMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: ProfilePhoto) {
    val colors = MaterialTheme.colorScheme
    var remoteBytes by remember(photo.url) { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(photo.url, photo.localBytes) {
        if (photo.localBytes == null && photo.url != null) {
            remoteBytes = fetchImageBytes(photo.url)
        }
    }

    val bytes = photo.localBytes ?: remoteBytes
    val bitmap = bytes?.decodeToImageBitmapOrNull()

    Box(
        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(84.dp))
        } else {
            PersonIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(28.dp))
        }
    }
}

/** Search-to-select occupation field: shows a default shortlist when empty, filtered results while typing. */
@Composable
fun OccupationPickerField(
    query: String,
    selected: String,
    results: List<String>,
    onQueryChanged: (String) -> Unit,
    onSelect: (String) -> Unit,
    placeholder: String = "Search occupation"
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var isFocused by remember { mutableStateOf(false) }

    Column {
        if (selected.isNotBlank() && !isFocused) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.primaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, colors.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(text = selected, style = typography.bodyMedium, fontWeight = FontWeight.Bold, color = colors.primary, modifier = Modifier.weight(1f))
                    Surface(onClick = { isFocused = true; onQueryChanged("") }, shape = CircleShape, color = Color.Transparent) {
                        Text(text = "Change", style = typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary, modifier = Modifier.padding(4.dp))
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { onQueryChanged(it); isFocused = true },
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(text = placeholder, style = typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(shape = RoundedCornerShape(16.dp), color = colors.surface, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.height((results.size.coerceAtMost(10) * 44).dp)) {
                    items(results.take(10)) { occupation ->
                        Surface(
                            onClick = { onSelect(occupation); isFocused = false },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = occupation,
                                style = typography.bodyMedium,
                                color = colors.onSurface,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Country-code + national-number field: digit entry is capped to the selected country's expected length, with a searchable country picker dialog. Styled to match [OutlinedField] so it reads as part of the same form, not a bolted-on widget. */
@Composable
fun PhoneNumberField(
    phone: String,
    selectedCountry: CountryCode,
    countries: List<CountryCode>,
    onPhoneChanged: (String) -> Unit,
    onCountrySelected: (CountryCode) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var isPickerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val expectedRange = remember(selectedCountry.code) { PhoneNumberRules.expectedLength(selectedCountry.code) }
    val isIncomplete = phone.isNotEmpty() && phone.length !in expectedRange

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isPickerOpen = true }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "${selectedCountry.flagEmoji} ${selectedCountry.dialCode}",
                    style = typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                ChevronDownIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(10.dp))
            }

            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.width(1.dp).height(22.dp).background(colors.outline.copy(alpha = 0.3f)))
            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = phone,
                onValueChange = { input ->
                    if (input.length <= expectedRange.last && input.all(Char::isDigit)) onPhoneChanged(input)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (phone.isEmpty()) {
                        Text(text = "98765 43210", style = typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    inner()
                }
            )
        }

        if (isIncomplete) {
            Spacer(modifier = Modifier.height(6.dp))
            val hint = if (expectedRange.first == expectedRange.last) {
                "Enter a ${expectedRange.first}-digit ${selectedCountry.name} number"
            } else {
                "Enter a ${expectedRange.first}-${expectedRange.last} digit ${selectedCountry.name} number"
            }
            Text(text = hint, style = typography.labelSmall, color = colors.error)
        }
    }

    if (isPickerOpen) {
        val filtered = remember(searchQuery, countries) {
            if (searchQuery.isBlank()) {
                countries
            } else {
                countries.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.dialCode.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        Dialog(onDismissRequest = { isPickerOpen = false; searchQuery = "" }) {
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                shape = RoundedCornerShape(20.dp),
                color = colors.surface,
                shadowElevation = 10.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(text = "Select Country", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text(text = "Search country", style = typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                inner()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                        items(filtered) { country ->
                            Surface(
                                onClick = { onCountrySelected(country); isPickerOpen = false; searchQuery = "" },
                                color = Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = country.displayString,
                                    style = typography.bodyMedium,
                                    color = colors.onSurface,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Up to 5 URL fields (GitHub, portfolio site, LinkedIn, etc.) with add/remove. */
@Composable
fun PortfolioLinksSection(
    links: List<String>,
    onLinkChanged: (index: Int, value: String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (index: Int) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEachIndexed { index, link ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = link,
                        onValueChange = { onLinkChanged(index, it) },
                        singleLine = true,
                        textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                        modifier = Modifier.fillMaxWidth(),
                        decorationBox = { inner ->
                            if (link.isEmpty()) {
                                Text(
                                    text = if (index == 0) "github.com/yourhandle" else "Portfolio, LinkedIn, website…",
                                    style = typography.bodyMedium,
                                    color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                }
                if (links.size > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(onClick = { onRemove(index) }, shape = CircleShape, color = colors.surfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            CrossIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }

        if (links.size < 5) {
            Surface(onClick = onAdd, shape = RoundedCornerShape(12.dp), color = Color.Transparent) {
                Text(text = "+ Add another link", style = typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary, modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}
