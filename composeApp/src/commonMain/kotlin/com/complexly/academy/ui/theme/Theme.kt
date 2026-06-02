package com.complexly.academy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CrashCourseTeal,
    onPrimary = Color.White,
    background = CreamBackground,
    onBackground = CharcoalText,
    surface = CardBackground,
    onSurface = CharcoalText,
    surfaceVariant = CreamBackground,
    onSurfaceVariant = MediumGrayText,
    outline = CardBorder
)

// Define dark mode scheme for completeness (MaterialTheme standard fallback)
private val DarkColorScheme = darkColorScheme(
    primary = CrashCourseTeal,
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF37474F)
)

@Composable
fun ScienceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
