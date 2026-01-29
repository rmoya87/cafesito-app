package com.cafesito.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
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
    onSurfaceVariant = EspressoMedium,
    outline = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CaramelSoft,
    onPrimary = NightEspresso,
    secondary = CaramelSoft,
    onSecondary = NightEspresso,
    background = NightEspresso,
    surface = DarkCoffeeBean,
    onSurface = SoftOffWhite,
    onSurfaceVariant = MutedCream,
    outline = DarkBorder
)

@Composable
fun CafesitoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CafesitoTypography,
        content = content
    )
}
