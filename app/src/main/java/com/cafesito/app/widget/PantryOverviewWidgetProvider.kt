package com.cafesito.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import kotlinx.coroutines.runBlocking
import kotlin.math.roundToInt

class PantryOverviewWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        private const val CARD_COUNT = 3

        private val CARD_IDS = intArrayOf(
            R.id.widget_pantry_card_1,
            R.id.widget_pantry_card_2,
            R.id.widget_pantry_card_3
        )
        private val IMAGE_IDS = intArrayOf(
            R.id.widget_pantry_item_image_1,
            R.id.widget_pantry_item_image_2,
            R.id.widget_pantry_item_image_3
        )
        private val NAME_IDS = intArrayOf(
            R.id.widget_pantry_item_name_1,
            R.id.widget_pantry_item_name_2,
            R.id.widget_pantry_item_name_3
        )
        private val BRAND_IDS = intArrayOf(
            R.id.widget_pantry_item_brand_1,
            R.id.widget_pantry_item_brand_2,
            R.id.widget_pantry_item_brand_3
        )
        private val STOCK_IDS = intArrayOf(
            R.id.widget_pantry_item_stock_1,
            R.id.widget_pantry_item_stock_2,
            R.id.widget_pantry_item_stock_3
        )
        private val PROGRESS_IDS = intArrayOf(
            R.id.widget_pantry_item_progress_1,
            R.id.widget_pantry_item_progress_2,
            R.id.widget_pantry_item_progress_3
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PantryOverviewWidgetProvider::class.java)
            manager.updateAppWidget(component, buildViews(context))
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
                        val pct = ((item.gramsRemaining.toFloat() / item.totalGrams) * 100f).roundToInt().coerceIn(0, 100)
                        setViewVisibility(CARD_IDS[index], View.VISIBLE)
                        setTextViewText(NAME_IDS[index], item.coffeeName)
                        setTextViewText(BRAND_IDS[index], item.coffeeBrand)
                        setTextViewText(STOCK_IDS[index], "${item.gramsRemaining}g de ${item.totalGrams}g")
                        setProgressBar(PROGRESS_IDS[index], 100, pct, false)
                        setImageViewResource(IMAGE_IDS[index], R.drawable.ic_launcher_foreground)
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
