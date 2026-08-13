package com.rajamohan.mindmingle.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import mindmingle.shared.generated.resources.Res
import mindmingle.shared.generated.resources.poppins_bold
import mindmingle.shared.generated.resources.poppins_medium
import mindmingle.shared.generated.resources.poppins_regular
import mindmingle.shared.generated.resources.poppins_semibold

@Composable
fun getPoppinsFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
    )
}

@Composable
fun appTypography(): Typography {
    val poppins = getPoppinsFontFamily()
    val defaultTypography = Typography()

    return Typography(
        displayLarge = defaultTypography.displayLarge.copy(fontFamily = poppins),
        displayMedium = defaultTypography.displayMedium.copy(fontFamily = poppins),
        displaySmall = defaultTypography.displaySmall.copy(fontFamily = poppins),
        headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = poppins),
        headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = poppins),
        headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = poppins),
        titleLarge = defaultTypography.titleLarge.copy(fontFamily = poppins),
        titleMedium = defaultTypography.titleMedium.copy(fontFamily = poppins),
        titleSmall = defaultTypography.titleSmall.copy(fontFamily = poppins),
        bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = poppins),
        bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = poppins),
        bodySmall = defaultTypography.bodySmall.copy(fontFamily = poppins),
        labelLarge = defaultTypography.labelLarge.copy(fontFamily = poppins),
        labelMedium = defaultTypography.labelMedium.copy(fontFamily = poppins),
        labelSmall = defaultTypography.labelSmall.copy(fontFamily = poppins),
    )
}
