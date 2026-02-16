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
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PantryOverviewWidgetProvider::class.java)
            manager.updateAppWidget(component, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            val items = runBlocking { WidgetDataStore.loadPantryItems(context, maxItems = 3) }
            return RemoteViews(context.packageName, R.layout.widget_pantry_overview).apply {
                setOnClickPendingIntent(R.id.widget_pantry_container, navIntent(context))

                val titleIds = intArrayOf(R.id.widget_pantry_item_title_1, R.id.widget_pantry_item_title_2, R.id.widget_pantry_item_title_3)
                val gramsIds = intArrayOf(R.id.widget_pantry_item_grams_1, R.id.widget_pantry_item_grams_2, R.id.widget_pantry_item_grams_3)
                val progressIds = intArrayOf(R.id.widget_pantry_item_progress_1, R.id.widget_pantry_item_progress_2, R.id.widget_pantry_item_progress_3)
                val rowIds = intArrayOf(R.id.widget_pantry_row_1, R.id.widget_pantry_row_2, R.id.widget_pantry_row_3)

                if (items.isEmpty()) {
                    setViewVisibility(R.id.widget_pantry_empty, View.VISIBLE)
                    rowIds.forEach { setViewVisibility(it, View.GONE) }
                } else {
                    setViewVisibility(R.id.widget_pantry_empty, View.GONE)
                    rowIds.forEachIndexed { i, rowId ->
                        val item = items.getOrNull(i)
                        if (item == null) {
                            setViewVisibility(rowId, View.GONE)
                        } else {
                            val pct = if (item.totalGrams <= 0) 0 else ((item.gramsRemaining.toFloat() / item.totalGrams) * 100).roundToInt().coerceIn(0, 100)
                            setViewVisibility(rowId, View.VISIBLE)
                            setTextViewText(titleIds[i], item.coffeeName)
                            setTextViewText(gramsIds[i], "${item.gramsRemaining}g · ${pct}%")
                            setProgressBar(progressIds[i], 100, pct, false)
                        }
                    }
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
