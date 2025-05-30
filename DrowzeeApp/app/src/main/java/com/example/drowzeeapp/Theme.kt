package com.example.drowzeeapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B57D0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E3FD),
    onPrimaryContainer = Color(0xFF041E49),
    secondary = Color(0xFF175CDC),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2FF),
    onSecondaryContainer = Color(0xFF001A41),
    tertiary = Color(0xFF006C51),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8FF8D0),
    onTertiaryContainer = Color(0xFF002117),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFD),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFFAFDFD),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA6C8FF),
    onPrimary = Color(0xFF002E6A),
    primaryContainer = Color(0xFF0B57D0),
    onPrimaryContainer = Color(0xFFD3E3FD),
    secondary = Color(0xFFB2C5FF),
    onSecondary = Color(0xFF002E6A),
    secondaryContainer = Color(0xFF1546AE),
    onSecondaryContainer = Color(0xFFDAE2FF),
    tertiary = Color(0xFF71DBB4),
    onTertiary = Color(0xFF003828),
    tertiaryContainer = Color(0xFF00513C),
    onTertiaryContainer = Color(0xFF8FF8D0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1D),
    onBackground = Color(0xFFE1E3E3),
    surface = Color(0xFF191C1D),
    onSurface = Color(0xFFE1E3E3),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C7CF),
    outline = Color(0xFF8D9199)
)

@Composable
fun DrowsinessDetectionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Typography definitions
val Typography = androidx.compose.material3.Typography(
    // Typography definitions here
)
