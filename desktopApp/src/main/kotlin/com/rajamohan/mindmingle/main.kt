package com.rajamohan.mindmingle

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.rajamohan.mindmingle.core.JvmFirebaseInitializer

fun main() = application {
    JvmFirebaseInitializer.ensureInitialized()

    val windowState = rememberWindowState(
        width = 1100.dp,
        height = 760.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "MindMingle",
    ) {
        App()
    }
}
