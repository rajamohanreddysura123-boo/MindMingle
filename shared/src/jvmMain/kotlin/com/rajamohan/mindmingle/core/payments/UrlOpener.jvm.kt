package com.rajamohan.mindmingle.core.payments

import java.awt.Desktop
import java.net.URI

actual object UrlOpener {
    actual fun open(url: String): Boolean {
        return try {
            // Desktop.isDesktopSupported is false on headless JVMs and on some Linux setups with
            // no xdg-open; the QR is the fallback in exactly those cases.
            if (!Desktop.isDesktopSupported()) return false
            val desktop = Desktop.getDesktop()
            if (!desktop.isSupported(Desktop.Action.BROWSE)) return false
            desktop.browse(URI(url))
            true
        } catch (e: Exception) {
            false
        }
    }
}
