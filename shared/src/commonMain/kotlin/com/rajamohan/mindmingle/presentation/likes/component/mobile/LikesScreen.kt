package com.rajamohan.mindmingle.presentation.likes.component.mobile

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
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.shadow
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

@Composable
fun LikesScreen(uid: String) {
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
                .safeContentPadding()
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
                    text = "Likes & Matches",
                    style = typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "${uiState.likedByUsers.size} New",
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
                uiState.likedByUsers.isEmpty() -> {
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.likedByUsers) { item ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = colors.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                            .background(Brush.linearGradient(paletteFor(item.uid))),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DeveloperAvatarIcon(color = Color.White, modifier = Modifier.size(42.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = item.experienceLevel.ifBlank { "Dev" },
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            style = typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onSurface
                                        )
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
