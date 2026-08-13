package com.rajamohan.mindmingle.core.settings

import platform.Foundation.NSUserDefaults

actual object AppearanceStorage {
    private const val KEY_MODE = "appearance_mode"
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun load(): AppearanceMode {
        val stored = defaults.stringForKey(KEY_MODE) ?: return AppearanceMode.SYSTEM
        return runCatching { AppearanceMode.valueOf(stored) }.getOrNull() ?: AppearanceMode.SYSTEM
    }

    actual fun save(mode: AppearanceMode) {
        defaults.setObject(mode.name, KEY_MODE)
    }
}
