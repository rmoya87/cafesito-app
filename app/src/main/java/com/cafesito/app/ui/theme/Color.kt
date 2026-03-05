package com.cafesito.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light Palette: Espresso & Cream Premium
val EspressoDeep = Color(0xFF1A120B)
val CoffeeBrown = EspressoDeep // Alias para compatibilidad
val EspressoMedium = Color(0xFF3C2A21)
val CaramelAccent = Color(0xFF6F4E37) // Marrón Café Eléctrico
val PureWhite = Color(0xFFFFFFFF)
val ScreenLightBackground = Color(0xFFF7F7F7)
val CreamLight = Color(0xFFF9F5F2)
val SoftOffWhite = Color(0xFFFDFDFD)
val BorderLight = Color(0xFFE0E0E0).copy(alpha = 0.4f)

// Dark Palette: Night Roast (marrón claro como en web)
val PureBlack = Color(0xFF000000)
val ScreenDarkBackground = Color(0xFF212121)
val NightEspresso = Color(0xFF0F0905)
val DarkCoffeeBean = Color(0xFF1E1611)
/** Marrón claro para modo noche (menciones, me gusta, acentos). Mismo que web --caramel-accent en dark. */
val CaramelSoft = Color(0xFFD4A373)
/** Marrón muy claro para modo noche (primary, títulos). Mismo que web --espresso-deep en dark. */
val CaramelLight = Color(0xFFE8C9A8)
val DarkBorder = Color(0xFF3E322B)
val DarkOutline = Color(0xFF1A1A1A) // Negro algo más claro para bordes
val MutedCream = Color(0xFFBDB7B2)

/** Gris oscuro para fechas/timestamps en modo día. */
val DateMetaLight = Color(0xFF5C5C5C)
/** Gris claro para fechas/timestamps en modo noche. */
val DateMetaDark = Color(0xFFB8B8B8)

// Semánticos (igual en light y dark)
val WaterBlue = Color(0xFF2196F3)
val WaterBlueBackground = Color(0xFFE3F2FD) // fondo suave para entradas de agua
val DisabledGray = Color(0xFF9E9E9E)

val SuccessGreen = Color(0xFF81C784)
val ElectricRed = Color(0xFFFF3B30)
val ErrorRed = ElectricRed
val OrangeYellow = Color(0xFFFFB300)
