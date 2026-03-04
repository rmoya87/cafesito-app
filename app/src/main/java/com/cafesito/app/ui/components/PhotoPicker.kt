package com.cafesito.app.ui.components

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Flujo unificado de selección de foto compatible con la política de Google Play:
 * - Cámara: solo permiso CAMERA.
 * - Galería: Photo Picker (PickVisualMedia), sin permisos READ_MEDIA_IMAGES/READ_MEDIA_VIDEO.
 * No se declaran ni usan permisos de acceso persistente a fotos/vídeos.
 */

/**
 * Crea un Uri temporal para guardar la foto de cámara (FileProvider).
 */
fun createTempImageUriForPicker(context: Context, prefix: String = "photo"): Uri {
    val file = File(context.cacheDir, "${prefix}_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * Contenido del bottom sheet "Añadir foto": Hacer foto (cámara) y Elegir de galería (Photo Picker).
 * El llamador debe pasar callbacks que:
 * - onTakePhoto: pidan permiso CAMERA si hace falta y lancen el cameraLauncher con un Uri temporal.
 * - onPickFromGallery: lancen el galleryLauncher (PickVisualMedia).
 */
@Composable
fun AddPhotoBottomSheetContent(
    onDismissRequest: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
    ) {
        Text(
            text = "AÑADIR FOTO",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp),
            fontWeight = FontWeight.Bold
        )
        ModalMenuOption(
            title = "Hacer foto",
            icon = Icons.Default.PhotoCamera,
            color = MaterialTheme.colorScheme.primary,
            onClick = {
                onTakePhoto()
                onDismissRequest()
            }
        )
        ModalMenuOption(
            title = "Elegir de galería",
            icon = Icons.Default.Collections,
            color = MaterialTheme.colorScheme.primary,
            onClick = {
                onPickFromGallery()
                onDismissRequest()
            }
        )
    }
}
