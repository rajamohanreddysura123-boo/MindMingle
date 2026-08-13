package com.rajamohan.mindmingle.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

/** User's theme preference (Profile > Appearance) — persisted per platform, reactive so the root AppTheme recomposes on change. */
object AppearanceSettings {
    private val _mode = MutableStateFlow(AppearanceStorage.load())
    val mode: StateFlow<AppearanceMode> = _mode.asStateFlow()

    fun set(mode: AppearanceMode) {
        _mode.value = mode
        AppearanceStorage.save(mode)
    }
}

/** Platform persistence only — reactivity lives in [AppearanceSettings]. */
expect object AppearanceStorage {
    fun load(): AppearanceMode
    fun save(mode: AppearanceMode)
}
