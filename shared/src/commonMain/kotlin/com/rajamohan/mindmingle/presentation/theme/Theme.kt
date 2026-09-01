package com.rajamohan.mindmingle.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)


@Immutable
data class AnonymousColors(
    val accents: List<List<Color>>,
    val backdrop: List<Color>,
    val whisper: Color,
    val onWhisper: Color,
    val onAccent: Color
)

val anonymousColorsLight = AnonymousColors(
    accents = listOf(
        listOf(anonymousAccentOneLight, anonymousAccentOneVariantLight),
        listOf(anonymousAccentTwoLight, anonymousAccentTwoVariantLight),
        listOf(anonymousAccentThreeLight, anonymousAccentThreeVariantLight),
        listOf(anonymousAccentFourLight, anonymousAccentFourVariantLight),
        listOf(anonymousAccentFiveLight, anonymousAccentFiveVariantLight)
    ),
    backdrop = listOf(anonymousBackdropTopLight, anonymousBackdropBottomLight),
    whisper = anonymousWhisperLight,
    onWhisper = onAnonymousWhisperLight,
    onAccent = onAnonymousAccentLight
)

val anonymousColorsDark = AnonymousColors(
    accents = listOf(
        listOf(anonymousAccentOneDark, anonymousAccentOneVariantDark),
        listOf(anonymousAccentTwoDark, anonymousAccentTwoVariantDark),
        listOf(anonymousAccentThreeDark, anonymousAccentThreeVariantDark),
        listOf(anonymousAccentFourDark, anonymousAccentFourVariantDark),
        listOf(anonymousAccentFiveDark, anonymousAccentFiveVariantDark)
    ),
    backdrop = listOf(anonymousBackdropTopDark, anonymousBackdropBottomDark),
    whisper = anonymousWhisperDark,
    onWhisper = onAnonymousWhisperDark,
    onAccent = onAnonymousAccentDark
)

val LocalAnonymousColors = staticCompositionLocalOf { anonymousColorsDark }

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
  val colorScheme = if (darkTheme) darkScheme else lightScheme
  val anonymousColors = if (darkTheme) anonymousColorsDark else anonymousColorsLight

  CompositionLocalProvider(LocalAnonymousColors provides anonymousColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = appTypography(),
      content = content
    )
  }
}


