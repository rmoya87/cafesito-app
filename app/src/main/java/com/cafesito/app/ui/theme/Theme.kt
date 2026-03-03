package com.cafesito.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** En modo noche es marrón claro (CaramelSoft), en modo claro es marrón café (CaramelAccent). */
val LocalCaramelAccent = staticCompositionLocalOf { CaramelAccent }

private val LightColorScheme = lightColorScheme(
    primary = EspressoDeep,
    onPrimary = Color.White,
    secondary = CaramelAccent,
    onSecondary = Color.White,
    tertiary = BorderLight,
    onTertiary = EspressoDeep,
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
    tertiary = MutedCream,
    onTertiary = PureBlack,
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

    val caramelAccent = if (darkTheme) CaramelSoft else CaramelAccent
    CompositionLocalProvider(LocalCaramelAccent provides caramelAccent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CafesitoTypography,
            content = content
        )
    }
}
