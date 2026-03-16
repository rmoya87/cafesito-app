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
/** Gris oscuro para líneas de separación y divisores en modo noche. */
val SeparatorDark = Color(0xFF212121)
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
/** Verde eléctrico para estado activo de listas (favoritos/listas custom). */
val ElectricGreen = Color(0xFF00E676)
val ElectricRed = Color(0xFFFF3B30)
val ErrorRed = ElectricRed
val OrangeYellow = Color(0xFFFFB300)

// --- Overlays y superficies secundarias (estandarizados) ---
/** Scrim para modales, bottom sheets y overlays. */
val ScrimDefault = PureBlack.copy(alpha = 0.5f)

/** Fondo del botón de cámara (modo noche). */
val CameraBgDark = Color(0xFF2D2D2D)
/** Fondo del botón de cámara (modo día). */
val CameraBgLight = Color(0xFFE0E0E0)

/** Botón primario inactivo (ej. Publicar deshabilitado) en modo noche. */
val ButtonInactiveDark = Color(0xFF3D3D3D)
/** Botón primario inactivo en modo día. */
val ButtonInactiveLight = Color(0xFFE8E8E8)

/** Fondo de tarjeta de consejo/advice (modo noche). */
val AdviceCardBgDark = Color(0xFF303030)
/** Fondo de tarjeta de consejo (modo día). */
val AdviceCardBgLight = Color(0xFFEFEFEF)
/** Texto en tarjeta de consejo (modo noche). */
val AdviceCardTextDark = Color(0xFFF0F0F0)
/** Texto en tarjeta de consejo (modo día). */
val AdviceCardTextLight = Color(0xFF2F2F2F)

/** Track inactivo de slider (modo noche). */
val SliderTrackInactiveDark = Color(0xFF404040)
/** Track inactivo de slider (modo día). */
val SliderTrackInactiveLight = Color(0xFFE0E0E0)

/** Grid/outline en gráfico radar (modo noche). */
val RadarGridDark = Color(0xFF757575)

/** Eje/etiquetas en gráficos (modo noche). */
val DateMetaAxisDark = Color(0xFF6F6760)
/** Eje/etiquetas en gráficos (modo día). */
val DateMetaAxisLight = Color(0xFFB0A8A0)

/** Switch track sin marcar (modo noche). */
val SwitchTrackOffDark = Color(0xFF424242)
/** Switch thumb sin marcar (modo noche). */
val SwitchThumbOffDark = Color(0xFFBDBDBD)
