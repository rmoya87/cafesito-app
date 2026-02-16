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
    background = ScreenLightBackground,
    surface = PureWhite,
    surfaceVariant = PureWhite, // Forzado a blanco puro para evitar tonos grisáceos en cards
    surfaceContainer = ScreenLightBackground,
    surfaceContainerLow = ScreenLightBackground,
    surfaceContainerLowest = ScreenLightBackground,
    surfaceContainerHigh = ScreenLightBackground,
    surfaceContainerHighest = ScreenLightBackground,
    onSurface = EspressoDeep,
    onSurfaceVariant = EspressoMedium,
    outline = BorderLight,
    surfaceTint = Color.Transparent // Desactiva la elevación tonal que añade gris/tinte
)

private val DarkColorScheme = darkColorScheme(
    primary = CaramelSoft,
    onPrimary = PureBlack,
    secondary = CaramelSoft,
    onSecondary = PureBlack,
    background = ScreenDarkBackground,
    surface = PureBlack,
    surfaceVariant = PureBlack, // Negro puro para modo noche
    surfaceContainer = ScreenDarkBackground,
    surfaceContainerLow = ScreenDarkBackground,
    surfaceContainerLowest = ScreenDarkBackground,
    surfaceContainerHigh = ScreenDarkBackground,
    surfaceContainerHighest = ScreenDarkBackground,
    onSurface = Color.White,
    onSurfaceVariant = MutedCream,
    outline = PureBlack, // Cambiado de DarkOutline (gris muy oscuro) a negro puro
    surfaceTint = Color.Transparent // Desactiva la elevación tonal para mantener negro puro
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
