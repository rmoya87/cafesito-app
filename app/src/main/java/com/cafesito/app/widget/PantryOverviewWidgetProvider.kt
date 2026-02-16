package com.cafesito.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import kotlinx.coroutines.runBlocking

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
            val pantryItems = runBlocking { WidgetDataStore.loadPantryItems(context, maxItems = 4) }
            return RemoteViews(context.packageName, R.layout.widget_pantry_overview).apply {
                setOnClickPendingIntent(R.id.widget_pantry_container, navIntent(context))

                val ids = intArrayOf(
                    R.id.widget_pantry_item_1,
                    R.id.widget_pantry_item_2,
                    R.id.widget_pantry_item_3,
                    R.id.widget_pantry_item_4
                )

                if (pantryItems.isEmpty()) {
                    setTextViewText(R.id.widget_pantry_empty, "Tu despensa está vacía. Toca para añadir cafés.")
                    setViewVisibility(R.id.widget_pantry_empty, android.view.View.VISIBLE)
                    ids.forEach { setTextViewText(it, "") }
                } else {
                    setViewVisibility(R.id.widget_pantry_empty, android.view.View.GONE)
                    ids.forEachIndexed { index, id ->
                        val item = pantryItems.getOrNull(index)
                        setTextViewText(
                            id,
                            item?.let { "• ${it.coffeeName}: ${it.gramsRemaining}g/${it.totalGrams}g" } ?: ""
                        )
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
