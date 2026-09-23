package com.rajamohan.mindmingle.presentation.likes.component.desktop

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import com.rajamohan.mindmingle.presentation.common.component.RemoteProfileImage
import com.rajamohan.mindmingle.presentation.home.component.mobile.PremiumBadge
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikesEvent
import com.rajamohan.mindmingle.presentation.common.icon.EnvelopeIcon
import com.rajamohan.mindmingle.presentation.likes.viewmodel.LikesViewModel
import com.rajamohan.mindmingle.presentation.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

private val likeCardPalettes = listOf(
    listOf(Color(0xFFFF4081), Color(0xFFFF80AB)),
    listOf(Color(0xFF00C853), Color(0xFFB9F6CA)),
    listOf(Color(0xFF651FFF), Color(0xFFB388FF)),
    listOf(Color(0xFFFF6D00), Color(0xFFFFD180))
)

private fun paletteFor(uid: String): List<Color> =
    likeCardPalettes[(uid.hashCode().let { if (it < 0) -it else it }) % likeCardPalettes.size]

/** Wide adaptive grid — the mobile 2-column grid stretched to use the extra desktop width properly. */
@Composable
fun DesktopLikesScreen(uid: String, onOpenChat: (otherUid: String) -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    val viewModel: LikesViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uid) {
        viewModel.loadLikes(uid)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column(modifier = Modifier.fillMaxSize().safeContentPadding().padding(Spacing.desktopScreenPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Likes", style = typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(text = "People who liked your profile", style = typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = colors.primaryContainer.copy(alpha = 0.6f)) {
                    Text(
                        text = "${uiState.totalCount} New",
                        style = typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                }
                uiState.entries.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            EnvelopeIcon(color = colors.onSurfaceVariant, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "No likes yet", style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                            Text(text = "Keep browsing Discover to get noticed", style = typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.entries, key = { it.user.uid }) { entry ->
                            val item = entry.user
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = colors.surface,
                                shadowElevation = 3.dp,
                                modifier = Modifier.height(292.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // A face, like every other surface in the app. This wall drew
                                    // a gradient and a glyph, so a page of people who liked you
                                    // was a page of identical tiles.
                                    RemoteProfileImage(
                                        url = item.displayPhotoUrls.firstOrNull().orEmpty(),
                                        uid = item.uid,
                                        contentDescription = item.name.ifBlank { "Profile photo" },
                                        placeholderIconSize = 44.dp,
                                        modifier = Modifier.fillMaxWidth().height(150.dp)
                                    )

                                    Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.name.ifBlank { "Unknown user" },
                                                style = typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (item.isPremium) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                PremiumBadge(size = 15.dp)
                                            }
                                        }

                                        Text(
                                            text = listOf(item.occupation, item.experienceLevel)
                                                .filter { it.isNotBlank() }
                                                .joinToString(" · "),
                                            style = typography.bodySmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(modifier = Modifier.weight(1f))

                                        // The actual point of the screen, and what it was missing:
                                        // desktop could see who liked you and do nothing about it.
                                        if (entry.isConnected) {
                                            DesktopLikeAction(
                                                label = "Message",
                                                isPrimary = true,
                                                onClick = { onOpenChat(item.uid) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                DesktopLikeAction(
                                                    label = "Ignore",
                                                    isPrimary = false,
                                                    onClick = { viewModel.onEvent(LikesEvent.Ignore(item.uid)) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                DesktopLikeAction(
                                                    label = "Like back",
                                                    isPrimary = true,
                                                    onClick = { viewModel.onEvent(LikesEvent.LikeBack(item.uid)) },
                                                    modifier = Modifier.weight(1f)
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
}

/** One action on a likes card. Same two weights the mobile list uses. */
@Composable
private fun DesktopLikeAction(
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isPrimary) colors.primary else colors.surfaceVariant.copy(alpha = 0.6f),
        modifier = modifier.height(38.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) colors.onPrimary else colors.onSurface
            )
        }
    }
}
