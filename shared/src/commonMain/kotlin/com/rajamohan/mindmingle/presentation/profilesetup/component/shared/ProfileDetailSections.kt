package com.rajamohan.mindmingle.presentation.profilesetup.component.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.core.location.LocationService
import com.rajamohan.mindmingle.domain.model.ProfileFieldOption
import com.rajamohan.mindmingle.domain.model.ProfileSection
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.launch

@Composable
fun ProfileDetailSections(
    sections: List<ProfileSection>,
    details: Map<String, String>,
    selections: Map<String, List<String>>,
    onDetailChanged: (key: String, value: String) -> Unit,
    onToggleSelection: (key: String, value: String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    if (sections.isEmpty()) {
        Text(
            text = "Loading profile options…",
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
        return
    }

    sections.forEachIndexed { index, section ->
        if (index > 0) Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = section.title,
            style = typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        if (section.subtitle.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = section.subtitle,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        section.fields.forEach { field ->
            Spacer(modifier = Modifier.height(16.dp))
            ProfileField(
                field = field,
                value = details[field.key].orEmpty(),
                selected = selections[field.key].orEmpty(),
                onValueChanged = { onDetailChanged(field.key, it) },
                onToggle = { onToggleSelection(field.key, it) }
            )
        }
    }
}

@Composable
private fun ProfileField(
    field: ProfileFieldOption,
    value: String,
    selected: List<String>,
    onValueChanged: (String) -> Unit,
    onToggle: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val label = if (field.optional) "${field.label} (optional)" else field.label
    Text(text = label, style = typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onBackground)

    if (field.key == "preferredLocation") {
        Spacer(modifier = Modifier.height(10.dp))
        PreferredLocationField(value = value, placeholder = field.placeholder.ifBlank { field.label }, onValueChange = onValueChanged)
        return
    }

    when (field.type) {
        "single" -> {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                field.options.forEach { option ->
                    OptionChip(
                        label = option,
                        isSelected = value == option,
                        onClick = { onValueChanged(if (value == option) "" else option) }
                    )
                }
            }
        }

        "multi" -> {
            if (field.maxSelect > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${selected.size} / ${field.maxSelect} selected",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                field.options.forEach { option ->
                    OptionChip(
                        label = option,
                        isSelected = selected.contains(option),
                        onClick = { onToggle(option) }
                    )
                }
            }
        }

        "date" -> {
            Spacer(modifier = Modifier.height(10.dp))
            DatePickerField(value = value, onValueChange = onValueChanged)
        }

        else -> {
            Spacer(modifier = Modifier.height(10.dp))
            DetailTextField(
                value = value,
                placeholder = field.placeholder.ifBlank { field.label },
                keyboardType = if (field.type == "number") KeyboardType.Number else KeyboardType.Text,
                onValueChange = onValueChanged
            )
        }
    }
}

/** Preferred-location text field with a trailing "Detect" button that fills it via IP geolocation, independent of the profile's own home Location field. */
@Composable
private fun PreferredLocationField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val scope = rememberCoroutineScope()
    var isDetecting by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.weight(1f)) {
            DetailTextField(value = value, placeholder = placeholder, keyboardType = KeyboardType.Text, onValueChange = onValueChange)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            onClick = {
                if (!isDetecting) {
                    isDetecting = true
                    scope.launch {
                        val display = LocationService.getCurrentLocation()?.displayString?.takeIf { it.isNotBlank() }
                        if (display != null) onValueChange(display)
                        isDetecting = false
                    }
                }
            },
            enabled = !isDetecting,
            shape = RoundedCornerShape(14.dp),
            color = colors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.height(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                if (isDetecting) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text(text = "Detect", style = typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.primary)
                }
            }
        }
    }
}

@Composable
private fun DetailTextField(
    value: String,
    placeholder: String,
    keyboardType: KeyboardType,
    onValueChange: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typography.bodyMedium.copy(color = colors.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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

/** Tap-to-open calendar for picking a date of birth; stores/reads "YYYY-MM-DD" like the rest of the form. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
private fun DatePickerField(value: String, onValueChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    var isPickerOpen by remember { mutableStateOf(false) }
    val todayMillis = remember { Clock.System.now().toEpochMilliseconds() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable { isPickerOpen = true }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = value.ifBlank { "Select date of birth" },
            style = typography.bodyMedium,
            color = if (value.isBlank()) colors.onSurfaceVariant.copy(alpha = 0.6f) else colors.onSurface
        )
    }

    if (isPickerOpen) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToEpochMillis(value) ?: todayMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { isPickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onValueChange(epochMillisToIsoDate(it)) }
                    isPickerOpen = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { isPickerOpen = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun epochMillisToIsoDate(millis: Long): String {
    val (year, month, day) = civilFromDays(millis.floorDiv(86_400_000L))
    return "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}

private fun isoDateToEpochMillis(value: String): Long? {
    val parts = value.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    return daysFromCivil(year, month, day) * 86_400_000L
}

private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = (y - era * 400).toLong()
    val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097L + doe - 719468
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

@Composable
private fun OptionChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colors.primary else colors.surface,
        border = BorderStroke(1.dp, if (isSelected) colors.primary else colors.outline.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) colors.onPrimary else colors.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
