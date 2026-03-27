package com.cafesito.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

/** En modo noche es marrón claro (CaramelSoft), en modo claro es marrón café (CaramelAccent). */
val LocalCaramelAccent = staticCompositionLocalOf { CaramelAccent }

/** Color para fechas/timestamps: gris oscuro en día, gris claro en noche. */
val LocalDateMetaColor = staticCompositionLocalOf { DateMetaLight }

/** Preferencia de tema: automático (sistema), claro u oscuro. */
object ThemeMode {
    const val KEY = "theme_mode"
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val DARK = "dark"
}

/** Preferencia para activar/desactivar Dynamic Color en Android 12+ (Material You). */
object DynamicColorMode {
    const val KEY = "dynamic_color_enabled"
    const val DEFAULT = true
}

/** Resuelve si debe usarse tema oscuro según preferencia guardada y sistema. */
fun resolveDarkTheme(themeMode: String, isSystemInDarkTheme: Boolean): Boolean = when (themeMode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    else -> isSystemInDarkTheme
}

private val LightColorScheme = lightColorScheme(
    primary = CaramelAccent,
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
    outlineVariant = BorderLight,
    surfaceTint = Color.Transparent // Desactiva la elevación tonal que añade gris/tinte
)

private val DarkColorScheme = darkColorScheme(
    primary = CaramelLight,
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
    outline = SeparatorDark, // Gris oscuro para líneas de separación y bordes en modo noche
    outlineVariant = SeparatorDark,
    surfaceTint = Color.Transparent // Desactiva la elevación tonal para mantener negro puro
)

@Composable
fun CafesitoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColorEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val baseColorScheme = if (dynamicColorEnabled && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }
    // Cards sólidas por tema: blanco en día y negro en noche.
    val surfaceForCards = if (darkTheme) PureBlack else PureWhite
    val separatorColor = if (darkTheme) SeparatorDark else BorderLight
    val colorScheme = baseColorScheme.copy(
        surface = surfaceForCards,
        surfaceVariant = surfaceForCards,
        outline = separatorColor,
        outlineVariant = separatorColor
    )

    val caramelAccent = if (darkTheme) CaramelSoft else CaramelAccent
    val dateMetaColor = if (darkTheme) DateMetaDark else DateMetaLight
    CompositionLocalProvider(
        LocalCaramelAccent provides caramelAccent,
        LocalDateMetaColor provides dateMetaColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CafesitoTypography,
            content = content
        )
    }
}
