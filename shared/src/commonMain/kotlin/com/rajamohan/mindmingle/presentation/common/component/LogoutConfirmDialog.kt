package com.rajamohan.mindmingle.presentation.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Shown by every Log Out control in the app — mobile Profile, desktop Profile and the desktop
 * side rail — so a mis-tap never ends the session on its own.
 */
@Composable
fun LogoutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = colors.surface, shadowElevation = 8.dp) {
            Column(modifier = Modifier.widthIn(max = 420.dp).padding(24.dp)) {
                Text(
                    text = "Log out?",
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "You'll be signed out of MindMingle on this device. Your profile, " +
                        "matches and messages stay exactly as they are.",
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
                                text = "Stay signed in",
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
                                text = "Log Out",
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
