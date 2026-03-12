package com.cafesito.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest

private const val ASSET_ICONS = "icons/"

/**
 * Carga un SVG desde assets y lo devuelve como [Painter] para usar en Icon/Image.
 * Requiere que el [ImageLoader] global tenga registrado [coil.decode.SvgDecoder].
 */
@Composable
fun rememberSvgFromAssetPainter(assetPath: String): Painter {
    val context = LocalContext.current
    val fullPath = if (assetPath.startsWith(ASSET_ICONS)) assetPath else "$ASSET_ICONS$assetPath"
    val request = remember(fullPath) {
        ImageRequest.Builder(context)
            .data(context.assets.open(fullPath).readBytes())
            .build()
    }
    return rememberAsyncImagePainter(model = request, imageLoader = context.imageLoader)
}

@Composable
fun rememberListAltSvgPainter(): Painter = rememberSvgFromAssetPainter("list_alt.svg")

@Composable
fun rememberListAltAddSvgPainter(): Painter = rememberSvgFromAssetPainter("list_alt_add.svg")

@Composable
fun rememberListAltCheckSvgPainter(): Painter = rememberSvgFromAssetPainter("list_alt_check.svg")
