package com.rajamohan.mindmingle.presentation.common.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.rajamohan.mindmingle.presentation.common.icon.DeveloperAvatarIcon
import com.rajamohan.mindmingle.presentation.home.component.mobile.avatarGradientFor

/**
 * A profile photo from Firebase Storage, drawn over the app's per-uid gradient.
 *
 * Every failure mode lands on that gradient — the look the deck had before photos existed.
 * Profiles legitimately have no photos, and a Storage URL can 404 after an account purge, so an
 * empty result is a normal state here and not something to surface as an error.
 *
 * [placeholderIconSize] is null on full-bleed surfaces (deck hero, detail screen), where a glyph
 * floating in the middle of a tall gradient reads as a broken load rather than as a stand-in.
 */
@Composable
internal fun RemoteProfileImage(
    url: String,
    uid: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderIconSize: Dp? = null
) {
    Box(modifier = modifier.background(Brush.linearGradient(avatarGradientFor(uid)))) {
        if (url.isBlank()) {
            PlaceholderGlyph(placeholderIconSize)
            return@Box
        }

        val painter = rememberAsyncImagePainter(model = url, contentScale = contentScale)
        val state by painter.state.collectAsState()

        when (state) {
            is AsyncImagePainter.State.Success -> Image(
                painter = painter,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )

            is AsyncImagePainter.State.Loading -> CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.8f),
                strokeWidth = 2.dp,
                modifier = Modifier.align(Alignment.Center).size(22.dp)
            )

            else -> PlaceholderGlyph(placeholderIconSize)
        }
    }
}

@Composable
private fun BoxScope.PlaceholderGlyph(size: Dp?) {
    if (size == null) return
    DeveloperAvatarIcon(
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.align(Alignment.Center).size(size)
    )
}
