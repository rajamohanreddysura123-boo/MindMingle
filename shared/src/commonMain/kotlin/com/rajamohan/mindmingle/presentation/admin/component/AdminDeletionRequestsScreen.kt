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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.domain.model.DeletionRequest
import com.rajamohan.mindmingle.domain.model.nowMillis
import com.rajamohan.mindmingle.presentation.admin.viewmodel.AdminDeletionRequestsViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import com.rajamohan.mindmingle.presentation.common.icon.PersonIcon
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Admin deletion queue. Each row is a user who asked to be deleted from Account Settings; their
 * own client already cleared what it could reach and they are locked out. Delete here finishes
 * the job: matches, messages, mirrored likes, support thread, subscription and photos, then the
 * bannedUids tombstone that keeps their leftover Firebase Auth record shut out for good.
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
                Text(
                    text = "Deletion Requests",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.error.isNotBlank()) {
                Text(text = uiState.error, style = typography.bodySmall, color = colors.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.message.isNotBlank()) {
                Text(text = uiState.message, style = typography.bodySmall, color = colors.primary)
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                uiState.requests.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PersonIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No deletion requests",
                                style = typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    val now = nowMillis()
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.pending.isNotEmpty()) {
                            item(key = "pending-header") {
                                SectionHeader(text = "Pending (${uiState.pending.size})")
                            }
                        }
                        items(uiState.pending, key = { "pending-${it.uid}" }) { request ->
                            DeletionRequestRow(
                                request = request,
                                waitingDays = request.waitingDaysAt(now),
                                isPurging = uiState.purgingUid == request.uid,
                                isBusy = uiState.purgingUid.isNotBlank(),
                                onDeleteClick = { confirmFor = request }
                            )
                        }

                        if (uiState.completed.isNotEmpty()) {
                            item(key = "done-header") {
                                SectionHeader(text = "Deleted (${uiState.completed.size})")
                            }
                        }
                        items(uiState.completed, key = { "done-${it.uid}" }) { request ->
                            DeletionRequestRow(
                                request = request,
                                waitingDays = request.waitingDaysAt(now),
                                isPurging = false,
                                isBusy = true,
                                onDeleteClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

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
}

@Composable
private fun SectionHeader(text: String) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Text(
        text = text,
        style = typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun DeletionRequestRow(
    request: DeletionRequest,
    waitingDays: Int,
    isPurging: Boolean,
    isBusy: Boolean,
    onDeleteClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
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
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.errorContainer.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                PersonIcon(color = colors.error, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = request.name.ifBlank { "(no name)" },
                    style = typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = request.email.ifBlank { request.phoneNumber.ifBlank { request.uid } },
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = if (request.isPending) {
                        "Requested ${request.requestedLabel} · waiting $waitingDays d"
                    } else {
                        "Deleted — requested ${request.requestedLabel}"
                    },
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (request.isPending) {
                Surface(
                    onClick = onDeleteClick,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(50),
                    color = colors.error,
                    modifier = Modifier.height(42.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize()
                    ) {
                        if (isPurging) {
                            CircularProgressIndicator(
                                color = colors.onError,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = "Delete",
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
private fun PurgeConfirmDialog(
    request: DeletionRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 8.dp) {
            Column(modifier = Modifier.widthIn(max = 460.dp).padding(24.dp)) {
                Text(
                    text = "Delete ${request.name.ifBlank { "this account" }}?",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Removes their profile, photos, likes on both sides, matches and " +
                        "messages, support thread and subscription records, then blocks the uid " +
                        "permanently. This cannot be undone.",
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
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
                        shape = RoundedCornerShape(50),
                        color = colors.error,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Delete forever",
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
