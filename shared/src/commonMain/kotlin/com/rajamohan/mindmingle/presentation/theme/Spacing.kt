package com.rajamohan.mindmingle.presentation.theme

import androidx.compose.ui.unit.dp

/** App-wide screen-edge padding — every top-level screen's root container should use these instead of ad-hoc dp literals. */
object Spacing {
    val screenHorizontal = 20.dp
    val screenVertical = 16.dp

    /** Desktop screens sit in more open space than mobile, so their edge margin is wider. */
    val desktopScreenPadding = 32.dp
}
