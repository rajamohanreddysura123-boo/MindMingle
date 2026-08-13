package com.rajamohan.mindmingle.core.settings

import java.util.prefs.Preferences

actual object AppearanceStorage {
    private const val KEY_MODE = "appearance_mode"
    private val prefs = Preferences.userRoot().node("com/rajamohan/mindmingle/settings")

    actual fun load(): AppearanceMode {
        val stored = prefs.get(KEY_MODE, null) ?: return AppearanceMode.SYSTEM
        return runCatching { AppearanceMode.valueOf(stored) }.getOrNull() ?: AppearanceMode.SYSTEM
    }

    actual fun save(mode: AppearanceMode) {
        prefs.put(KEY_MODE, mode.name)
    }
}
