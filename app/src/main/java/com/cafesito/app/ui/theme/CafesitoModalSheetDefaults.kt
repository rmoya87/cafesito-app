package com.cafesito.app.ui.theme

import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Valores comunes para [androidx.compose.material3.ModalBottomSheet] (paridad entre buscador, perfil, diario, etc.).
 */
object CafesitoModalSheetDefaults {
    val scrimColor: Color get() = ScrimDefault

    val shape: Shape get() = Shapes.sheetLarge

    @Composable
    fun containerColor(): Color = MaterialTheme.colorScheme.surfaceContainer

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun dragHandle() = BottomSheetDefaults.DragHandle()
}
