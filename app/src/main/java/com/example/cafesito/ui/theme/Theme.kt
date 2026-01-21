package com.example.cafesito.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EspressoDeep,
    onPrimary = Color.White,
    secondary = CaramelAccent,
    onSecondary = Color.White,
    background = SoftOffWhite,
    surface = CreamLight,
    onSurface = EspressoDeep,
    outline = BorderLight
)

@Composable
fun CafesitoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = CafesitoTypography,
        content = content
    )
}
