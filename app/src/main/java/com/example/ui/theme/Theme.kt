package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ScanovaDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF0B1020),
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = EmeraldSuccess,
    onSecondary = Color(0xFF052E16),
    secondaryContainer = EmeraldSuccessContainerDark,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = AmberWarning,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = AmberWarningContainerDark,
    onTertiaryContainer = Color(0xFFFEF3C7),
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseErrorContainerDark,
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val ScanovaLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = EmeraldSuccess,
    onSecondary = Color.White,
    secondaryContainer = EmeraldSuccessContainerLight,
    onSecondaryContainer = Color(0xFF064E3B),
    tertiary = AmberWarning,
    onTertiary = Color.White,
    tertiaryContainer = AmberWarningContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
    error = RoseError,
    onError = Color.White,
    errorContainer = RoseErrorContainerLight,
    onErrorContainer = Color(0xFF7F1D1D),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun ScanovaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> ScanovaDarkColorScheme
        else -> ScanovaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    ScanovaTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        content = content
    )
}
