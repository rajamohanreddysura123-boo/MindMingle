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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
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
fun DesktopLikesScreen(uid: String) {
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
                    Text(text = "Likes & Matches", style = typography.headlineMedium, fontWeight = FontWeight.Bold, color = colors.onBackground)
                    Text(text = "People who liked your profile", style = typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(12.dp), color = colors.primaryContainer.copy(alpha = 0.6f)) {
                    Text(
                        text = "${uiState.likedByUsers.size} New",
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
                uiState.likedByUsers.isEmpty() -> {
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
                        items(uiState.likedByUsers, key = { it.uid }) { item ->
                            Surface(shape = RoundedCornerShape(22.dp), color = colors.surface, shadowElevation = 3.dp, modifier = Modifier.height(210.dp)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(120.dp).background(Brush.linearGradient(paletteFor(item.uid))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(44.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.5f),
                                            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                                        ) {
                                            Text(
                                                text = item.experienceLevel.ifBlank { "Dev" },
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Text(text = item.name, style = typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
                                        Text(
                                            text = item.occupation,
                                            style = typography.bodySmall,
                                            color = colors.onSurfaceVariant
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
