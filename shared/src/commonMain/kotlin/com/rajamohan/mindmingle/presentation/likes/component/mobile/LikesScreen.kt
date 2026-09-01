package com.rajamohan.mindmingle.presentation.likes.component.mobile

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.home.component.mobile.PremiumBadge
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikeEntry
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikesEvent
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikesViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * Who liked this user, split into the two states a like can be in.
 *
 * A like arrives as a request and stays one until it is answered. Returning it is what opens the
 * conversation, so returned likes carry a "Chat" action and the rest carry "Like back" / "Ignore".
 * Nothing here ever tells the other person they were turned down.
 *
 * [onOpenChat] hands the other user's uid to the host so it can switch to the Chat tab.
 */
@Composable
fun LikesScreen(
    uid: String,
    onOpenChat: (otherUid: String) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: LikesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.loadLikes(uid)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = colors.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.screenVertical)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Likes",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${uiState.totalCount}",
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }

                uiState.entries.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EnvelopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No likes yet",
                                style = typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Keep browsing Discover to get noticed",
                                style = typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (uiState.pending.isNotEmpty()) {
                            item(key = "header_pending") {
                                SectionHeader(text = "Waiting for your answer")
                            }
                            items(uiState.pending, key = { "pending_${it.user.uid}" }) { entry ->
                                LikeRow(
                                    entry = entry,
                                    onLikeBack = { viewModel.onEvent(LikesEvent.LikeBack(entry.user.uid)) },
                                    onIgnore = { viewModel.onEvent(LikesEvent.Ignore(entry.user.uid)) },
                                    onOpenChat = { onOpenChat(entry.user.uid) }
                                )
                            }
                        }

                        if (uiState.connected.isNotEmpty()) {
                            item(key = "header_connected") {
                                SectionHeader(text = "You liked back")
                            }
                            items(uiState.connected, key = { "connected_${it.user.uid}" }) { entry ->
                                LikeRow(
                                    entry = entry,
                                    onLikeBack = {},
                                    onIgnore = {},
                                    onOpenChat = { onOpenChat(entry.user.uid) }
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
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun LikeRow(
    entry: LikeEntry,
    onLikeBack: () -> Unit,
    onIgnore: () -> Unit,
    onOpenChat: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val user = entry.user

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RemoteProfileImage(
                url = user.displayPhotoUrls.firstOrNull().orEmpty(),
                uid = user.uid,
                contentDescription = user.name.ifBlank { "Profile photo" },
                placeholderIconSize = 26.dp,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name.ifBlank { "Unknown user" },
                        style = typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (user.isPremium) {
                        Spacer(modifier = Modifier.width(4.dp))
                        PremiumBadge(size = 15.dp)
                    }
                }
                Text(
                    text = listOf(user.occupation, user.experienceLevel)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    style = typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            if (entry.isConnected) {
                ActionPill(label = "Chat", isPrimary = true, onClick = onOpenChat)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionPill(label = "Ignore", isPrimary = false, onClick = onIgnore)
                    ActionPill(label = "Like back", isPrimary = true, onClick = onLikeBack)
                }
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, isPrimary: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = if (isPrimary) colors.primary else colors.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPrimary) colors.onPrimary else colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
