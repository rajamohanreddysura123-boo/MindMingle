package com.rajamohan.mindmingle.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A modal overlay, drawn as ordinary content in the same window rather than through
 * `androidx.compose.ui.window.Dialog`.
 *
 * This exists specifically because `Dialog` opens a genuinely separate OS-level window on
 * Compose Desktop, and content inside that window did not reliably receive mouse-wheel scroll —
 * every dialog with a scrollable body (the filter sheet, the profile detail popup, the desktop
 * payment sheet) was affected, while the exact same `Modifier.verticalScroll` works everywhere
 * else in the app, inside the main window. Rendering the scrim and content as regular composables
 * inside the caller's own window sidesteps that gap entirely: scrolling here is not a special
 * case, it is the same mechanism every other screen already uses correctly.
 *
 * ## Using this correctly
 * The caller must place this as the LAST child of a `Box` that already fills the screen — Compose
 * draws later `Box` children on top of earlier ones, which is what makes this read as an overlay
 * rather than as another block of inline content pushing the layout around. A real `Dialog` needed
 * no such care, since it was a separate window; this is the one thing callers have to get right in
 * exchange for reliable scrolling.
 */
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    dismissOnClickOutside: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .then(
                if (dismissOnClickOutside) {
                    Modifier.pointerInput(Unit) { detectTapGestures { onDismissRequest() } }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // A tap on the content itself must not fall through to the scrim behind it and dismiss
        // the dialog it landed on.
        Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures { } }) {
            content()
        }
    }
}
