package com.rajamohan.mindmingle.presentation.admin.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminDeletionRequestsViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.BoltIcon
import com.rajamohan.mindmingle.presentation.common.icon.CheckBadgeIcon
import com.rajamohan.mindmingle.presentation.common.icon.CheckIcon
import com.rajamohan.mindmingle.presentation.common.icon.CrossIcon
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.common.icon.RefreshIcon
import com.rajamohan.mindmingle.presentation.common.icon.SearchIcon
import com.rajamohan.mindmingle.presentation.common.icon.TrashIcon
import com.rajamohan.mindmingle.presentation.home.component.mobile.avatarGradientFor
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private enum class DeletionFilter(val label: String) {
    PENDING("Pending"),
    COMPLETED("Purged"),
    ALL("All Requests")
}

private enum class DeletionSortOrder(val label: String) {
    OLDEST_FIRST("Oldest First"),
    NEWEST_FIRST("Newest First")
}

/**
 * Redesigned Admin Deletion Requests Screen for Desktop.
 * Provides live queue KPIs, search/filter controls, detailed confirmation dialog,
 * on-screen feedback banner, and successful deletion dialog.
 */
@Composable
fun AdminDeletionRequestsScreen(onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AdminDeletionRequestsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    var confirmFor by remember { mutableStateOf<DeletionRequest?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(DeletionFilter.PENDING) }
    var sortOrder by remember { mutableStateOf(DeletionSortOrder.OLDEST_FIRST) }

    val now = remember { nowMillis() }

    // Live KPI stats
    val pendingCount = uiState.pending.size
    val completedCount = uiState.completed.size
    val totalCount = uiState.requests.size
    val oldestWaitingDays = uiState.pending.maxOfOrNull { it.waitingDaysAt(now) } ?: 0

    // Filter and search computation
    val baseList = when (selectedFilter) {
        DeletionFilter.PENDING -> uiState.pending
        DeletionFilter.COMPLETED -> uiState.completed
        DeletionFilter.ALL -> uiState.requests
    }

    val trimmedQuery = searchQuery.trim().lowercase()
    val filteredList = if (trimmedQuery.isEmpty()) {
        baseList
    } else {
        baseList.filter { request ->
            request.name.lowercase().contains(trimmedQuery) ||
                request.email.lowercase().contains(trimmedQuery) ||
                request.phoneNumber.lowercase().contains(trimmedQuery) ||
                request.uid.lowercase().contains(trimmedQuery)
        }
    }

    val displayedRequests = when (sortOrder) {
        DeletionSortOrder.OLDEST_FIRST -> {
            filteredList.sortedWith(
                compareByDescending<DeletionRequest> { it.isPending }
                    .thenBy { if (it.requestedAt > 0L) it.requestedAt else Long.MAX_VALUE }
            )
        }
        DeletionSortOrder.NEWEST_FIRST -> {
            filteredList.sortedWith(
                compareByDescending<DeletionRequest> { it.isPending }
                    .thenByDescending { it.requestedAt }
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
                    .safeDrawingPadding()
                    .padding(Spacing.desktopScreenPadding)
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = colors.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Deletion Requests",
                                style = typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onBackground
                            )
                            Text(
                                text = "Review user account closure requests and permanently purge records & data",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    // Reload Button
                    Surface(
                        onClick = { viewModel.load() },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            RefreshIcon(color = colors.onSurface, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Refresh",
                                style = typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // KPI Stat Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    StatCard(
                        title = "Pending Purge",
                        value = pendingCount.toString(),
                        badgeText = if (pendingCount > 0) "$pendingCount awaiting" else "All clear",
                        isAlert = pendingCount > 0,
                        icon = { TrashIcon(color = if (pendingCount > 0) colors.error else colors.primary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Oldest in Queue",
                        value = if (pendingCount > 0) "${oldestWaitingDays}d" else "0d",
                        badgeText = when {
                            pendingCount == 0 -> "No backlog"
                            oldestWaitingDays >= 7 -> "Urgent SLA"
                            else -> "Within SLA"
                        },
                        isAlert = oldestWaitingDays >= 7,
                        icon = { BoltIcon(color = colors.tertiary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Purged Accounts",
                        value = completedCount.toString(),
                        badgeText = "Data wiped",
                        isAlert = false,
                        icon = { CheckBadgeIcon(color = colors.primary, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Total Processed",
                        value = totalCount.toString(),
                        badgeText = "All time",
                        isAlert = false,
                        icon = { PersonIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Search & Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Input
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SearchIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = typography.bodyMedium.copy(color = colors.onSurface),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search by user name, email, phone, or UID…",
                                            style = typography.bodyMedium,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    inner()
                                }
                            )

                            if (searchQuery.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = "" }
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CrossIcon(color = colors.onSurface, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    // Sort Order Toggle Chip
                    Surface(
                        onClick = {
                            sortOrder = if (sortOrder == DeletionSortOrder.OLDEST_FIRST) {
                                DeletionSortOrder.NEWEST_FIRST
                            } else {
                                DeletionSortOrder.OLDEST_FIRST
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Sort: ${sortOrder.label}",
                                style = typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Segmented Filter Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeletionFilter.entries.forEach { filter ->
                        val count = when (filter) {
                            DeletionFilter.PENDING -> pendingCount
                            DeletionFilter.COMPLETED -> completedCount
                            DeletionFilter.ALL -> totalCount
                        }
                        val isSelected = selectedFilter == filter

                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                if (filter == DeletionFilter.PENDING && count > 0) colors.error else colors.primary
                            } else {
                                colors.surfaceVariant.copy(alpha = 0.45f)
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    style = typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (filter == DeletionFilter.PENDING && count > 0) colors.onError else colors.onPrimary
                                    } else {
                                        colors.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) {
                                        Color.White.copy(alpha = 0.25f)
                                    } else {
                                        colors.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = "$count",
                                        style = typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            if (filter == DeletionFilter.PENDING && count > 0) colors.onError else colors.onPrimary
                                        } else {
                                            colors.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // On-Screen Feedback Banners
                AnimatedVisibility(
                    visible = uiState.message.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    StatusBanner(
                        message = uiState.message,
                        isError = false,
                        onDismiss = viewModel::dismissMessage
                    )
                }

                AnimatedVisibility(
                    visible = uiState.error.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    StatusBanner(
                        message = uiState.error,
                        isError = true,
                        onDismiss = viewModel::dismissMessage
                    )
                }

                // Main Content List or Empty State
                when {
                    uiState.isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }

                    displayedRequests.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (searchQuery.isNotEmpty()) {
                                EmptyState(
                                    icon = { SearchIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(36.dp)) },
                                    title = "No Matching Requests",
                                    subtitle = "No deletion requests match \"$searchQuery\". Try adjusting your search term.",
                                    actionLabel = "Clear Search",
                                    onAction = { searchQuery = "" }
                                )
                            } else {
                                EmptyState(
                                    icon = { CheckBadgeIcon(color = colors.primary, modifier = Modifier.size(36.dp)) },
                                    title = if (selectedFilter == DeletionFilter.PENDING) "All Caught Up!" else "No Requests Found",
                                    subtitle = if (selectedFilter == DeletionFilter.PENDING) {
                                        "There are no pending account deletion requests waiting for admin review."
                                    } else {
                                        "No records found in this category."
                                    }
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedRequests, key = { it.uid }) { request ->
                                DeletionRequestCard(
                                    request = request,
                                    waitingDays = request.waitingDaysAt(now),
                                    isPurging = uiState.purgingUid == request.uid,
                                    isBusy = uiState.purgingUid.isNotBlank(),
                                    onDeleteClick = { confirmFor = request }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before deletion
    confirmFor?.let { request ->
        PurgeConfirmDialog(
            request = request,
            onConfirm = {
                confirmFor = null
                viewModel.purge(request.uid)
            },
            onDismiss = { confirmFor = null }
        )
    }

    // Success Dialog after completed purge
    uiState.successDialog?.let { dialogState ->
        AdminSuccessDialog(
            title = dialogState.title,
            message = dialogState.message,
            onDismiss = viewModel::dismissSuccessDialog
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    badgeText: String,
    isAlert: Boolean,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isAlert) colors.errorContainer.copy(alpha = 0.5f) else colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        icon()
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAlert) colors.errorContainer else colors.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = badgeText,
                        style = typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlert) colors.onErrorContainer else colors.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) colors.error else colors.onSurface
            )

            Text(
                text = title,
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) colors.errorContainer else colors.primaryContainer,
        border = BorderStroke(
            1.dp,
            if (isError) colors.error.copy(alpha = 0.4f) else colors.primary.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isError) colors.error.copy(alpha = 0.2f) else colors.primary.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isError) {
                        CrossIcon(color = colors.error, modifier = Modifier.size(12.dp))
                    } else {
                        CheckIcon(color = colors.primary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                CrossIcon(
                    color = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun DeletionRequestCard(
    request: DeletionRequest,
    waitingDays: Int,
    isPurging: Boolean,
    isBusy: Boolean,
    onDeleteClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val isPending = request.isPending
    val displayName = request.name.ifBlank { "Unnamed User" }
    val initialChar = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surface,
        shadowElevation = if (isPending) 2.dp else 0.dp,
        border = BorderStroke(
            1.dp,
            if (isPending) colors.outlineVariant.copy(alpha = 0.4f) else colors.outlineVariant.copy(alpha = 0.2f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isPending) Modifier.alpha(0.65f) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar badge
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPending) {
                            Brush.linearGradient(avatarGradientFor(request.uid))
                        } else {
                            Brush.linearGradient(listOf(colors.surfaceVariant, colors.surfaceVariant.copy(alpha = 0.7f)))
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isPending) {
                    Text(
                        text = initialChar,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                } else {
                    TrashIcon(
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPending) colors.errorContainer.copy(alpha = 0.8f) else colors.surfaceVariant
                    ) {
                        Text(
                            text = if (isPending) "Awaiting Purge" else "Purged & Banned",
                            style = typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isPending) colors.error else colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // SLA urgency pill if pending
                    if (isPending) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                waitingDays >= 7 -> colors.errorContainer
                                waitingDays >= 3 -> colors.tertiaryContainer.copy(alpha = 0.7f)
                                else -> colors.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ) {
                            Text(
                                text = when {
                                    waitingDays >= 7 -> "Urgent · ${waitingDays}d waiting"
                                    waitingDays > 0 -> "${waitingDays}d in queue"
                                    else -> "Requested today"
                                },
                                style = typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    waitingDays >= 7 -> colors.error
                                    waitingDays >= 3 -> colors.tertiary
                                    else -> colors.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Contact details row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (request.email.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            EnvelopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = request.email,
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    if (request.phoneNumber.isNotBlank()) {
                        Text(
                            text = "·",
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text(
                            text = request.phoneNumber,
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "·",
                        style = typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.4f)
                    )

                    Text(
                        text = "UID: ${request.uid.take(12)}…",
                        style = typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timeline row
                Text(
                    text = if (isPending) {
                        "Requested on ${request.requestedLabel}"
                    } else {
                        "Requested: ${request.requestedLabel} · Purged: ${request.deletedLabel.ifBlank { "Yes" }}"
                    },
                    style = typography.labelSmall,
                    color = colors.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Action area
            if (isPending) {
                Surface(
                    onClick = onDeleteClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    color = colors.error,
                    modifier = Modifier.height(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 18.dp)
                    ) {
                        if (isPurging) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = colors.onError,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Purging…",
                                    style = typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onError
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TrashIcon(color = colors.onError, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Purge Account",
                                    style = typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onError
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        CheckBadgeIcon(color = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Purged",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))

            Surface(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                color = colors.primary,
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PurgeConfirmDialog(
    request: DeletionRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 500.dp).padding(20.dp)
        ) {
            Column(modifier = Modifier.padding(26.dp)) {
                // Warning Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = colors.errorContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TrashIcon(color = colors.error, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Permanently Purge Account?",
                            style = typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "This action is irreversible and permanent.",
                            style = typography.bodySmall,
                            color = colors.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Account summary pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = request.name.ifBlank { "(No Name Provided)" },
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        if (request.email.isNotBlank()) {
                            Text(
                                text = request.email,
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "UID: ${request.uid}",
                            style = typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cascade wipe details callout
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.errorContainer.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "The cascade purge will permanently remove:",
                            style = typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.error
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CascadeItem("Profile document and Cloud Storage photos")
                        CascadeItem("All mutual matches and chat message history")
                        CascadeItem("All likes sent and received")
                        CascadeItem("Active subscription records and support threads")
                        CascadeItem("Records UID in banned registry to block re-registration")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Cancel",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurface
                            )
                        }
                    }

                    Surface(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(14.dp),
                        color = colors.error,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Yes, Purge Forever",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.onError
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CascadeItem(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            text = "•",
            style = typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = colors.error,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            style = typography.bodySmall,
            color = colors.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AdminSuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 440.dp).padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary.copy(alpha = 0.12f),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(30.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.22f, h * 0.52f)
                                lineTo(w * 0.42f, h * 0.72f)
                                lineTo(w * 0.78f, h * 0.28f)
                            }
                            drawPath(
                                path = path,
                                color = colors.primary,
                                style = Stroke(
                                    width = w * 0.13f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title.ifBlank { "Account Purged Successfully" },
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    color = colors.primary,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "OK",
                            style = typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}
