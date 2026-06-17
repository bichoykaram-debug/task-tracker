package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccent,
    secondary = DarkAccent2,
    background = DarkBg1,
    surface = DarkSurface,
    onPrimary = DarkBg0,
    onSecondary = DarkBg0,
    onBackground = DarkText,
    onSurface = DarkText,
    error = DarkDanger
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccent,
    secondary = LightAccent2,
    background = LightBg1,
    surface = LightSurface,
    onPrimary = LightSurface2,
    onSecondary = LightSurface2,
    onBackground = LightText,
    onSurface = LightText,
    error = LightDanger
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
