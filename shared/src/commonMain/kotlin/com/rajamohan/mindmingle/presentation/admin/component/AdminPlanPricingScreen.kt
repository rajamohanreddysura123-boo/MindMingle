package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminPlanPricingViewModel
import com.rajamohan.mindmingle.presentation.admin.viewmodel.PricingRow
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Super-admin price list editor. Every row is one market: what it costs there and in which
 * currency. Saving goes through the savePlanPricing Cloud Function, which re-validates each
 * row before it becomes a live charge.
 */
@Composable
fun AdminPlanPricingScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AdminPlanPricingViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .widthIn(max = 900.dp)
                .padding(Spacing.desktopScreenPadding)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Plan Pricing",
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "MindMingle+ price per country. Users are billed in their own currency.",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    singleLine = true,
                    textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (uiState.query.isEmpty()) {
                            Text(
                                text = "Search by country or currency code…",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = viewModel::save,
                    enabled = uiState.canSave,
                    shape = RoundedCornerShape(50),
                    color = if (uiState.canSave) colors.primary else colors.surfaceVariant,
                    modifier = Modifier.height(44.dp).weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                color = colors.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "Save pricing",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.canSave) colors.onPrimary else colors.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    onClick = viewModel::restoreDefaults,
                    enabled = !uiState.isSaving,
                    shape = RoundedCornerShape(50),
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(44.dp).weight(1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Restore defaults",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                    }
                }
            }

            if (uiState.error.isNotBlank() || uiState.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.error.ifBlank { uiState.message },
                    style = typography.bodySmall,
                    color = if (uiState.error.isNotBlank()) colors.error else colors.tertiary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
                return@Column
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(uiState.filteredRows, key = { it.countryCode }) { row ->
                    PricingRowCard(
                        row = row,
                        isDefaultMarket = row.countryCode == uiState.defaultCountry,
                        onMonthlyChanged = { viewModel.onMonthlyChanged(row.countryCode, it) },
                        onAnnualChanged = { viewModel.onAnnualChanged(row.countryCode, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PricingRowCard(
    row: PricingRow,
    isDefaultMarket: Boolean,
    onMonthlyChanged: (String) -> Unit,
    onAnnualChanged: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.width(96.dp)) {
                Text(
                    text = row.countryCode,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = if (isDefaultMarket) "${row.currency} · default" else row.currency,
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            AmountField(
                label = "Monthly",
                value = row.monthlyInput,
                symbol = row.symbol,
                isValid = (row.monthlyMinor ?: 0L) > 0L,
                onValueChange = onMonthlyChanged,
                modifier = Modifier.weight(1f)
            )

            AmountField(
                label = "Annual",
                value = row.annualInput,
                symbol = row.symbol,
                isValid = (row.annualMinor ?: 0L) > 0L,
                onValueChange = onAnnualChanged,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AmountField(
    label: String,
    value: String,
    symbol: String,
    isValid: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(modifier = modifier) {
        Text(text = label, style = typography.labelSmall, color = colors.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isValid) colors.surfaceVariant.copy(alpha = 0.4f)
                    else colors.errorContainer.copy(alpha = 0.4f)
                )
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbol,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
