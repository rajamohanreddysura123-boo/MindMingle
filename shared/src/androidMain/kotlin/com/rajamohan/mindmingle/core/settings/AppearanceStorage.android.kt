package com.rajamohan.mindmingle.core.settings

import android.content.Context
import com.rajamohan.mindmingle.core.AppContext

actual object AppearanceStorage {
    private const val PREFS_NAME = "oo_settings"
    private const val KEY_MODE = "appearance_mode"

    actual fun load(): AppearanceMode {
        val context = AppContext.get() as? Context ?: return AppearanceMode.SYSTEM
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MODE, null)
        return stored?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM
    }

    actual fun save(mode: AppearanceMode) {
        val context = AppContext.get() as? Context ?: return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode.name).apply()
    }
}
