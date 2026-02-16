package com.cafesito.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import kotlinx.coroutines.runBlocking
import java.net.URL
import kotlin.math.roundToInt

class PantryOverviewWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, safeBuildViews(context))
        }
    }

    companion object {
        private const val TAG = "PantryWidget"
        private const val CARD_COUNT = 4

        private val CARD_IDS = intArrayOf(
            R.id.widget_pantry_card_1,
            R.id.widget_pantry_card_2,
            R.id.widget_pantry_card_3,
            R.id.widget_pantry_card_4
        )
        private val IMAGE_IDS = intArrayOf(
            R.id.widget_pantry_item_image_1,
            R.id.widget_pantry_item_image_2,
            R.id.widget_pantry_item_image_3,
            R.id.widget_pantry_item_image_4
        )
        private val NAME_IDS = intArrayOf(
            R.id.widget_pantry_item_name_1,
            R.id.widget_pantry_item_name_2,
            R.id.widget_pantry_item_name_3,
            R.id.widget_pantry_item_name_4
        )
        private val BRAND_IDS = intArrayOf(
            R.id.widget_pantry_item_brand_1,
            R.id.widget_pantry_item_brand_2,
            R.id.widget_pantry_item_brand_3,
            R.id.widget_pantry_item_brand_4
        )
        private val STOCK_IDS = intArrayOf(
            R.id.widget_pantry_item_stock_1,
            R.id.widget_pantry_item_stock_2,
            R.id.widget_pantry_item_stock_3,
            R.id.widget_pantry_item_stock_4
        )
        private val PROGRESS_IDS = intArrayOf(
            R.id.widget_pantry_item_progress_1,
            R.id.widget_pantry_item_progress_2,
            R.id.widget_pantry_item_progress_3,
            R.id.widget_pantry_item_progress_4
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PantryOverviewWidgetProvider::class.java)
            manager.updateAppWidget(component, safeBuildViews(context))
        }

        private fun safeBuildViews(context: Context): RemoteViews {
            return runCatching { buildViews(context) }
                .onFailure { Log.e(TAG, "Error al construir widget despensa", it) }
                .getOrElse { fallbackViews(context) }
        }

        private fun fallbackViews(context: Context): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_pantry_overview).apply {
                setOnClickPendingIntent(R.id.widget_pantry_container, navIntent(context))
                setViewVisibility(R.id.widget_pantry_empty, View.VISIBLE)
                setTextViewText(R.id.widget_pantry_empty, "Abre la app para sincronizar despensa")
                setViewVisibility(R.id.widget_pantry_more, View.GONE)
                CARD_IDS.forEach { setViewVisibility(it, View.GONE) }
            }
        }

        private fun buildViews(context: Context): RemoteViews {
            val allItems = runBlocking { WidgetDataStore.loadPantryItems(context, maxItems = 20) }
            val items = allItems.take(CARD_COUNT)
            return RemoteViews(context.packageName, R.layout.widget_pantry_overview).apply {
                setOnClickPendingIntent(R.id.widget_pantry_container, navIntent(context))

                if (items.isEmpty()) {
                    setViewVisibility(R.id.widget_pantry_empty, View.VISIBLE)
                    setViewVisibility(R.id.widget_pantry_more, View.GONE)
                    CARD_IDS.forEach { setViewVisibility(it, View.GONE) }
                    return@apply
                }

                setViewVisibility(R.id.widget_pantry_empty, View.GONE)

                repeat(CARD_COUNT) { index ->
                    val item = items.getOrNull(index)
                    if (item == null) {
                        setViewVisibility(CARD_IDS[index], View.GONE)
                    } else {
                        val safeTotal = item.totalGrams.coerceAtLeast(1)
                        val safeRemaining = item.gramsRemaining.coerceAtLeast(0)
                        val pct = ((safeRemaining.toFloat() / safeTotal) * 100f)
                            .takeIf { it.isFinite() }
                            ?.roundToInt()
                            ?.coerceIn(0, 100)
                            ?: 0

                        setViewVisibility(CARD_IDS[index], View.VISIBLE)
                        setTextViewText(NAME_IDS[index], item.coffeeName)
                        setTextViewText(BRAND_IDS[index], item.coffeeBrand)
                        setTextViewText(STOCK_IDS[index], "${safeRemaining}g de ${safeTotal}g")
                        setProgressBar(PROGRESS_IDS[index], 100, pct, false)

                        val bitmap = loadCoffeeImage(context, item.imageUrl)
                        if (bitmap != null) {
                            setImageViewBitmap(IMAGE_IDS[index], bitmap)
                        } else {
                            setImageViewResource(IMAGE_IDS[index], R.drawable.ic_launcher_foreground)
                        }
                    }
                }

                val remaining = allItems.size - CARD_COUNT
                if (remaining > 0) {
                    setViewVisibility(R.id.widget_pantry_more, View.VISIBLE)
                    setTextViewText(R.id.widget_pantry_more, "+${remaining} cafés más en tu despensa")
                } else {
                    setViewVisibility(R.id.widget_pantry_more, View.GONE)
                }
            }
        }

        private fun loadCoffeeImage(context: Context, rawUrl: String): Bitmap? {
            if (rawUrl.isBlank()) return null
            return runCatching {
                when {
                    rawUrl.startsWith("content://") || rawUrl.startsWith("file://") || rawUrl.startsWith("android.resource://") -> {
                        val uri = Uri.parse(rawUrl)
                        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                    }
                    rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> {
                        URL(rawUrl).openStream().use { BitmapFactory.decodeStream(it) }
                    }
                    else -> BitmapFactory.decodeFile(rawUrl)
                }
            }.getOrNull()
        }

        private fun navIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", "OPEN_PANTRY")
            }
            return PendingIntent.getActivity(
                context,
                9911,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
