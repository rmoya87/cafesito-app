package com.cafesito.app.ui.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.cafesito.app.ui.components.CoffeeListItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.BuildConfig
import com.cafesito.app.ui.theme.Shapes
import com.cafesito.app.ui.theme.Spacing
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker as OsmMarker
import android.graphics.drawable.Drawable

/** Separa pais_origen en tokens (coma, barra). */
private fun splitOrigins(paisOrigen: String?): List<String> {
    if (paisOrigen.isNullOrBlank()) return emptyList()
    return paisOrigen.split(*charArrayOf(',', '/', '|')).map { it.trim() }.filter { it.isNotEmpty() }
}

/** Crea un Drawable con el emoji de bandera para usar como icono de marcador. Usa TextView para que el emoji se renderice correctamente. */
private fun createFlagIconDrawable(ctx: Context, flagEmoji: String, sizePx: Int): Drawable {
    val text = flagEmoji.ifBlank { "\uD83C\uDF0D" } // 🌍
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = sizePx / 2f
    val radius = (sizePx / 2f) - 2f
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    canvas.drawCircle(center, center, radius, bgPaint)
    canvas.drawCircle(center, center, radius, strokePaint)
    val textView = TextView(ctx).apply {
        setText(text)
        textSize = (sizePx * 0.5f) / ctx.resources.displayMetrics.density
        setTextColor(Color.BLACK)
        gravity = Gravity.CENTER
        setPadding(0, 0, 0, 0)
    }
    textView.measure(
        View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
    )
    textView.layout(0, 0, sizePx, sizePx)
    canvas.save()
    canvas.translate(0f, 0f)
    textView.draw(canvas)
    canvas.restore()
    return BitmapDrawable(ctx.resources, bitmap).apply {
        setBounds(0, 0, sizePx, sizePx)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafesProbadosScreen(
    onBack: () -> Unit,
    onCoffeeClick: (String) -> Unit,
    viewModel: CafesProbadosViewModel = hiltViewModel()
) {
    val coffeesWithFirstTried by viewModel.coffeesWithFirstTried.collectAsState()
    var selectedCountry by remember { mutableStateOf<String?>(null) }

    val countriesWithCoords = remember(coffeesWithFirstTried) {
        val seen = mutableSetOf<String>()
        coffeesWithFirstTried.flatMap { (coffee, _) ->
            splitOrigins(coffee.paisOrigen).mapNotNull { country ->
                val key = country.trim().lowercase()
                if (key in seen) return@mapNotNull null
                CountryCoords.getCoords(country)?.let { coords ->
                    seen.add(key)
                    Triple(country, coords.lat, coords.lng)
                }
            }
        }
    }

    val filteredCoffees = remember(coffeesWithFirstTried, selectedCountry) {
        if (selectedCountry == null) coffeesWithFirstTried
        else {
            val key = selectedCountry!!.trim().lowercase()
            coffeesWithFirstTried.filter { (coffee, _) ->
                splitOrigins(coffee.paisOrigen).any { it.trim().lowercase() == key }
            }
        }
    }

    /** Agrupa por país (subtítulo = país). Orden: por país, luego por firstTriedMs. */
    val coffeesByCountry = remember(filteredCoffees) {
        val noOrigin = "—"
        val map = mutableMapOf<String, MutableList<TriedCoffeeItem>>()
        filteredCoffees.forEach { item ->
            val origins = splitOrigins(item.coffee.paisOrigen)
            val country = origins.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() } ?: noOrigin
            map.getOrPut(country) { mutableListOf() }.add(item)
        }
        map.values.forEach { list -> list.sortBy { it.firstTriedMs } }
        map.entries.sortedWith(compareBy<Map.Entry<String, MutableList<TriedCoffeeItem>>>({ it.key == noOrigin }, { it.key })).map { (country, list) ->
            country to list
        }
    }

    val defaultCenter = remember { GeoPoint(20.0, 0.0) }
    val initialZoom = 2.0
    val config = LocalConfiguration.current
    val screenHeightDp = config.screenHeightDp
    val listState = rememberLazyListState()
    val mapHeightDp = (screenHeightDp / 3).dp

    /** Ajustar vista del mapa: una vez al cargar (todos los pins) y al quitar filtro (X) volver a mostrar todos. */
    val hasInitialFit = remember { mutableStateOf(false) }
    val prevSelectedCountry = remember { mutableStateOf<String?>(null) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "map") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(mapHeightDp)
                    .clip(RectangleShape)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
                        val mapTilerKey = BuildConfig.MAPTILER_API_KEY
                        val tileSource = if (mapTilerKey.isNotEmpty()) {
                            object : OnlineTileSourceBase("MapTiler OMT", 0, 18, 256, ".png", arrayOf("https://api.maptiler.com/maps/basic-v2/")) {
                                override fun getTileURLString(pMapTileIndex: Long): String {
                                    val z = MapTileIndex.getZoom(pMapTileIndex)
                                    val x = MapTileIndex.getX(pMapTileIndex)
                                    val y = MapTileIndex.getY(pMapTileIndex)
                                    return "${getBaseUrl()}$z/$x/$y.png?key=$mapTilerKey"
                                }
                            }
                        } else {
                            TileSourceFactory.MAPNIK
                        }
                        MapView(ctx).apply {
                            setTileSource(tileSource)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            setMinZoomLevel(3.0)
                            setMaxZoomLevel(18.0)
                            controller.setZoom(initialZoom)
                            controller.setCenter(if (countriesWithCoords.isNotEmpty()) {
                                val first = countriesWithCoords.first()
                                GeoPoint(first.second, first.third)
                            } else {
                                defaultCenter
                            })
                        }
                    },
                    update = { mapView ->
                        val ctx = mapView.context
                        val sizePx = (32 * ctx.resources.displayMetrics.density).toInt()
                        mapView.setMinZoomLevel(3.0)
                        mapView.setMaxZoomLevel(18.0)
                        val points = countriesWithCoords.map { (_, lat, lng) -> GeoPoint(lat, lng) }
                        val fitAllPins: () -> Unit = {
                            if (points.isNotEmpty()) {
                                if (points.size == 1) {
                                    mapView.controller.setCenter(points[0])
                                    mapView.controller.setZoom(4.0)
                                } else {
                                    val box = BoundingBox.fromGeoPoints(points)
                                    val borderPx = (48 * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(64)
                                    mapView.zoomToBoundingBox(box, false, borderPx)
                                }
                            }
                        }
                        if (countriesWithCoords.isNotEmpty() && !hasInitialFit.value) {
                            hasInitialFit.value = true
                            mapView.post(fitAllPins)
                        }
                        if (prevSelectedCountry.value != null && selectedCountry == null && countriesWithCoords.isNotEmpty()) {
                            mapView.post(fitAllPins)
                        }
                        prevSelectedCountry.value = selectedCountry
                        mapView.overlays.clear()
                        countriesWithCoords.forEach { (country, lat, lng) ->
                            val flagEmoji = CountryFlags.getFlagEmoji(country)
                            val marker = OsmMarker(mapView).apply {
                                position = GeoPoint(lat, lng)
                                title = country
                                icon = createFlagIconDrawable(ctx, flagEmoji, sizePx)
                                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_CENTER)
                                setOnMarkerClickListener { _, _ ->
                                    selectedCountry = country
                                    mapView.controller.setCenter(GeoPoint(lat, lng))
                                    mapView.controller.setZoom(6.0)
                                    true
                                }
                            }
                            mapView.overlays.add(marker)
                        }
                        mapView.invalidate()
                    }
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(top = Spacing.space4, start = Spacing.space4, end = Spacing.space4),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a Mi diario",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (selectedCountry != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(top = Spacing.space4, start = Spacing.space4, end = Spacing.space4),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        IconButton(
                            onClick = { selectedCountry = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpiar filtro de país",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
        coffeesByCountry.forEach { (country, items) ->
            item(key = "header_$country") {
                Text(
                    text = country,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.space3, bottom = Spacing.space1)
                )
            }
            items(items, key = { it.coffee.id }) { item ->
                val dateFormat = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.forLanguageTag("es-ES")) }
                CoffeeListItem(
                    coffee = item.coffee,
                    subtitle = item.coffee.marca + (item.coffee.paisOrigen?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    secondLine = "Primera vez: ${dateFormat.format(java.util.Date(item.firstTriedMs))}",
                    imageSize = 48.dp,
                    showChevron = true,
                    onClick = onCoffeeClick
                )
            }
        }
    }
}
