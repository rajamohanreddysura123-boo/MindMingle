package com.rajamohan.mindmingle

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.rajamohan.mindmingle.core.settings.AppearanceMode
import com.rajamohan.mindmingle.core.settings.AppearanceSettings
import com.rajamohan.mindmingle.di.KoinInitializer
import com.rajamohan.mindmingle.presentation.navigation.MindMingleEntryPoint
import com.rajamohan.mindmingle.presentation.theme.AppTheme

@Composable
fun App() {
    remember {
        KoinInitializer.initKoin()
    }

    val appearanceMode by AppearanceSettings.mode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val darkTheme = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    AppTheme(darkTheme = darkTheme) {
        MindMingleEntryPoint()
    }
}
