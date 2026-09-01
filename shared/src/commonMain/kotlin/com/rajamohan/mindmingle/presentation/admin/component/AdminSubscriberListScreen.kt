package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.rajamohan.mindmingle.domain.model.PremiumPlan
import com.rajamohan.mindmingle.domain.model.Subscriber
import com.rajamohan.mindmingle.domain.model.SubscriberStatusFilter
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminSubscriberListViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Everyone who has ever paid, current and lapsed.
 *
 * Filtering and paging both happen in the Cloud Function, so a search here covers the whole
 * collection rather than only the rows already on screen — see AdminSubscriberListViewModel for
 * why that differs from the user list.
 */
@Composable
fun AdminSubscriberListScreen(
    onSubscriberClick: (uid: String) -> Unit,
    onBack: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AdminSubscriberListViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subscribers",
                        style = typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "${uiState.stats.active} active · ${uiState.stats.expired} expired · " +
                            "${uiState.stats.total} total",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                if (uiState.hasFilters) {
                    Surface(
                        onClick = viewModel::clearFilters,
                        shape = RoundedCornerShape(10.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Clear",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
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
                                text = "Search by name, email, or uid…",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status filters. A horizontal scroll rather than a wrap so the row height stays
            // fixed however many filters end up here.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                SubscriberStatusFilter.entries.forEach { filter ->
                    FilterChip(
                        label = filter.label,
                        selected = uiState.status == filter,
                        onClick = { viewModel.onStatusChanged(filter) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    label = "All plans",
                    selected = uiState.planId.isBlank(),
                    onClick = { viewModel.onPlanChanged(null) }
                )
                PremiumPlan.entries.forEach { plan ->
                    FilterChip(
                        label = plan.label,
                        selected = uiState.planId == plan.id,
                        onClick = { viewModel.onPlanChanged(plan) }
                    )
                }

                // Countries are drawn from what has loaded, so this row grows as pages arrive
                // rather than listing all 60 markets most of which have no subscriber.
                if (uiState.country.isNotBlank() || uiState.loadedCountries.size > 1) {
                    FilterChip(
                        label = "All countries",
                        selected = uiState.country.isBlank(),
                        onClick = { viewModel.onCountryChanged("") }
                    )
                    uiState.loadedCountries.forEach { code ->
                        FilterChip(
                            label = code,
                            selected = uiState.country == code,
                            onClick = { viewModel.onCountryChanged(code) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                uiState.error.isNotBlank() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = uiState.error, style = typography.bodyMedium, color = colors.error)
                    }
                }
                uiState.isEmpty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.hasFilters) {
                                "No subscribers match these filters"
                            } else {
                                "Nobody has subscribed yet"
                            },
                            style = typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.subscribers, key = { it.uid }) { subscriber ->
                            SubscriberRow(
                                subscriber = subscriber,
                                onClick = { onSubscriberClick(subscriber.uid) }
                            )
                        }

                        if (uiState.hasMore) {
                            item {
                                // Paging is explicit rather than triggered by scroll position:
                                // a page here can cost up to SCAN_CAP document reads, and that
                                // should be a decision, not a side effect of flicking a list.
                                Surface(
                                    onClick = viewModel::loadMore,
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(
                                                color = colors.primary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Load more",
                                                style = typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriberRow(subscriber: Subscriber, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                DeveloperAvatarIcon(color = colors.primary, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscriber.displayName,
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = "${subscriber.planLabel} · ${subscriber.periodLabel}",
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (subscriber.billingCountry.isNotBlank() || subscriber.isComped) {
                    Text(
                        text = listOfNotNull(
                            subscriber.billingCountry.takeIf { it.isNotBlank() },
                            "Comped".takeIf { subscriber.isComped }
                        ).joinToString(" · "),
                        style = typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            StateChip(label = subscriber.stateLabel, isActive = subscriber.isActive)
        }
    }
}

@Composable
private fun StateChip(label: String, isActive: Boolean) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) {
            colors.tertiaryContainer.copy(alpha = 0.6f)
        } else {
            colors.surfaceVariant.copy(alpha = 0.6f)
        }
    ) {
        Text(
            text = label,
            style = typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isActive) colors.tertiary else colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) colors.primary else colors.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Text(
            text = label,
            style = typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
