package com.rajamohan.mindmingle.presentation.account.component

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rajamohan.mindmingle.presentation.account.viewmodel.AccountSettingsViewModel
import com.rajamohan.mindmingle.presentation.common.icon.BackArrowIcon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import com.rajamohan.mindmingle.domain.model.NotificationCategory
import com.rajamohan.mindmingle.domain.model.NotificationPrefs
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profile → Account Settings. The only place either account-ending action lives.
 *
 * Deactivation hides the account for a fixed window with no way back in before it ends; deletion
 * clears out everything the user owns straight away and queues the rest for an admin. Both end
 * the session, so [onAccountClosed] takes the app back to Onboarding.
 */
@Composable
fun AccountSettingsScreen(
    onBack: () -> Unit,
    onAccountClosed: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: AccountSettingsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = colors.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        BackArrowIcon(color = colors.onSurface, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = "Account Settings",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.widthIn(max = 640.dp)) {
                NotificationPrefsCard(
                    prefs = uiState.notificationPrefs,
                    isLoading = uiState.isLoadingPrefs,
                    error = uiState.prefsError,
                    onToggle = viewModel::setNotificationCategory
                )

                Spacer(modifier = Modifier.height(20.dp))

                DeactivateCard(
                    dayOptions = uiState.dayOptions,
                    selectedDays = uiState.selectedDays,
                    returnDateLabel = uiState.returnDateLabel,
                    isBusy = uiState.isBusy,
                    isDeactivating = uiState.isDeactivating,
                    error = uiState.deactivateError,
                    onSelectDays = viewModel::selectDays,
                    onDeactivateClick = { showDeactivateConfirm = true }
                )

                Spacer(modifier = Modifier.height(20.dp))

                DeleteCard(
                    isBusy = uiState.isBusy,
                    isDeleting = uiState.isDeleting,
                    error = uiState.deleteError,
                    onDeleteClick = { showDeleteConfirm = true }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeactivateConfirm) {
        AccountActionDialog(
            title = "Deactivate for ${uiState.selectedDays} days?",
            body = "Your profile leaves Discover and nobody can message you. You will not be able " +
                "to sign in, and this cannot be cancelled early — your account comes back on " +
                "${uiState.returnDateLabel}, when you sign in again.",
            confirmLabel = "Deactivate",
            onConfirm = {
                showDeactivateConfirm = false
                viewModel.deactivate()
            },
            onDismiss = { showDeactivateConfirm = false }
        )
    }

    if (showDeleteConfirm) {
        AccountActionDialog(
            title = "Delete your account?",
            body = "This removes your profile, photos, matches, messages, likes and order history. " +
                "It cannot be undone, any time left on a MindMingle+ plan is lost, and you will " +
                "not be able to sign in again.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    // Both outcomes are terminal: the session is already gone by the time these show.
    if (uiState.isDeactivated) {
        AccountClosedDialog(
            title = "Account deactivated",
            body = "You're signed out. Sign in again on ${uiState.returnDateLabel} and everything " +
                "will be right where you left it.",
            onDismiss = onAccountClosed
        )
    }

    if (uiState.isDeleted) {
        AccountClosedDialog(
            title = "Account deleted",
            body = "Your account has been deleted and you have been signed out. Our team removes " +
                "the last traces from our systems shortly.",
            onDismiss = onAccountClosed
        )
    }
}

/**
 * The per-category push switches.
 *
 * These are read by the alert watchers and by the notification Cloud Functions, and until this
 * card existed there was no way to write them: the preferences document was created with every
 * category on and could never be changed, so "mute likes" was a setting the app claimed to have
 * and did not.
 *
 * A missing document means everything is on, so a first visit here shows all four enabled without
 * having written anything — the first toggle is what creates the document.
 */
@Composable
private fun NotificationPrefsCard(
    prefs: NotificationPrefs,
    isLoading: Boolean,
    error: String,
    onToggle: (NotificationCategory, Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Notifications",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Choose what MindMingle may notify you about on this account.",
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            NotificationCategory.entries.forEachIndexed { index, category ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = category.label,
                            style = typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            text = category.description,
                            style = typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = prefs.isEnabled(category),
                        // Disabled only while the stored values are still loading; leaving it live
                        // would let a tap be overwritten by the response landing a moment later.
                        enabled = !isLoading,
                        onCheckedChange = { checked -> onToggle(category, checked) }
                    )
                }
            }

            if (error.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, style = typography.bodySmall, color = colors.error)
            }
        }
    }
}

@Composable
private fun DeactivateCard(
    dayOptions: List<Int>,
    selectedDays: Int,
    returnDateLabel: String,
    isBusy: Boolean,
    isDeactivating: Boolean,
    error: String,
    onSelectDays: (Int) -> Unit,
    onDeactivateClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "Take a break",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Deactivating hides your profile from Discover and stops all messages. " +
                    "You cannot sign in or undo it while the break runs — your account comes back " +
                    "on its own when the days are up.",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dayOptions.forEach { days ->
                    val isSelected = days == selectedDays
                    Surface(
                        onClick = { onSelectDays(days) },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) colors.primary else colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.height(42.dp).weight(1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$days days",
                                style = typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) colors.onPrimary else colors.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Comes back on $returnDateLabel",
                style = typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onDeactivateClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(18.dp),
                color = colors.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, colors.onSurfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDeactivating) {
                        CircularProgressIndicator(
                            color = colors.onSurface,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Deactivate my account",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                    }
                }
            }

            if (error.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, style = typography.bodySmall, color = colors.error)
            }
        }
    }
}

@Composable
private fun DeleteCard(
    isBusy: Boolean,
    isDeleting: Boolean,
    error: String,
    onDeleteClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = colors.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                text = "Delete my account",
                style = typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Permanently removes your profile, photos, matches, messages, likes and " +
                    "order history. This cannot be undone and you will not be able to sign in again.",
                style = typography.bodyMedium,
                color = colors.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                onClick = onDeleteClick,
                enabled = !isBusy,
                shape = RoundedCornerShape(18.dp),
                color = colors.error,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = colors.onError,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Delete my account",
                            style = typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onError
                        )
                    }
                }
            }

            if (error.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, style = typography.bodySmall, color = colors.error)
            }
        }
    }
}

@Composable
private fun AccountActionDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 8.dp) {
            Column(modifier = Modifier.widthIn(max = 420.dp).padding(24.dp)) {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = body,
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
                                text = confirmLabel,
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

/** Terminal confirmation — the only way out is [onDismiss], which returns to Onboarding. */
@Composable
private fun AccountClosedDialog(title: String, body: String, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 8.dp) {
            Column(modifier = Modifier.widthIn(max = 420.dp).padding(24.dp)) {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = body,
                    style = typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = colors.primary,
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Done",
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
