package com.rajamohan.mindmingle

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.rajamohan.mindmingle.core.JvmFirebaseInitializer

fun main() = application {
    JvmFirebaseInitializer.ensureInitialized()

    Window(
        onCloseRequest = ::exitApplication,
        title = "MindMingle",
    ) {
        App()
    }
}
