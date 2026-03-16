package com.cafesito.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Dimensiones fijas para listas, iconos y layout (alturas de fila, avatares, etc.).
 * Alineado con DESIGN_TOKENS.md y GUIA_UNIFICACION_COMPONENTES_UI.md B.5.2.
 */
object Dimens {
    /** Icono vacío / ilustración (ej. sin resultados de búsqueda). */
    val iconSizeEmpty: Dp = 64.dp
    /** Altura de fila de café en listas (SearchScreen, etc.). */
    val cardImageHeight: Dp = 86.dp
    /** Padding inferior de listas con contenido scroll (evitar corte por barra nav). */
    val contentPaddingBottom: Dp = 100.dp
    /** Altura barra de navegación inferior. */
    val navBarHeight: Dp = 64.dp
}
