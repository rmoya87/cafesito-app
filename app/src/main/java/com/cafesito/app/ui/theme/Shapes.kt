package com.cafesito.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Formas estándar alineadas con DESIGN_TOKENS.md.
 * Usar en Surface, Card, Button, chips para consistencia.
 */
object Shapes {
    /** 10dp — botones pequeños, inputs. */
    val small = RoundedCornerShape(10.dp)
    /** 12dp — cards pequeñas, chips (--radius-md). */
    val cardSmall = RoundedCornerShape(12.dp)
    /** 16dp — cards, modales (--radius-lg). */
    val card = RoundedCornerShape(16.dp)
    /** 18dp — cards principales (--radius-card). */
    val cardLarge = RoundedCornerShape(18.dp)
    /** 20dp — bottom sheet superior, botón pill. */
    val sheetTop = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    /** 24dp — chips pill, avatar. */
    val pill = RoundedCornerShape(24.dp)
    /** 32dp — bottom sheet grande, premium card. */
    val sheetLarge = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
}
