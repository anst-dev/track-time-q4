package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = OnPrimaryBlue,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = OnSurfaceNeutral,
    onSurface = OnSurfaceNeutral,
    surfaceVariant = LightSurface,
    onSurfaceVariant = OnSurfaceNeutral
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark mode for premium screen feel
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
