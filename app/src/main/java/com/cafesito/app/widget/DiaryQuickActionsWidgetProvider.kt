package com.cafesito.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

class DiaryQuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, safeBuildViews(context, id, appWidgetManager.getAppWidgetOptions(id)))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, safeBuildViews(context, appWidgetId, newOptions))
    }

    companion object {
        private const val TAG = "DiaryWidget"
        private const val SLOT_COUNT = 7
        private const val EXPANDED_MIN_HEIGHT_DP = 220

        private val COFFEE_BAR_IDS = intArrayOf(
            R.id.widget_bar_coffee_1,
            R.id.widget_bar_coffee_2,
            R.id.widget_bar_coffee_3,
            R.id.widget_bar_coffee_4,
            R.id.widget_bar_coffee_5,
            R.id.widget_bar_coffee_6,
            R.id.widget_bar_coffee_7
        )

        private val WATER_BAR_IDS = intArrayOf(
            R.id.widget_bar_water_1,
            R.id.widget_bar_water_2,
            R.id.widget_bar_water_3,
            R.id.widget_bar_water_4,
            R.id.widget_bar_water_5,
            R.id.widget_bar_water_6,
            R.id.widget_bar_water_7
        )

        private val VALUE_IDS = intArrayOf(
            R.id.widget_bar_value_1,
            R.id.widget_bar_value_2,
            R.id.widget_bar_value_3,
            R.id.widget_bar_value_4,
            R.id.widget_bar_value_5,
            R.id.widget_bar_value_6,
            R.id.widget_bar_value_7
        )

        private val LABEL_IDS = intArrayOf(
            R.id.widget_bar_label_1,
            R.id.widget_bar_label_2,
            R.id.widget_bar_label_3,
            R.id.widget_bar_label_4,
            R.id.widget_bar_label_5,
            R.id.widget_bar_label_6,
            R.id.widget_bar_label_7
        )

        private val COFFEE_LEVEL_DRAWABLES = intArrayOf(
            R.drawable.widget_bar_coffee_0,
            R.drawable.widget_bar_coffee_1,
            R.drawable.widget_bar_coffee_2,
            R.drawable.widget_bar_coffee_3,
            R.drawable.widget_bar_coffee_4,
            R.drawable.widget_bar_coffee_5
        )

        private val WATER_LEVEL_DRAWABLES = intArrayOf(
            R.drawable.widget_bar_water_0,
            R.drawable.widget_bar_water_1,
            R.drawable.widget_bar_water_2,
            R.drawable.widget_bar_water_3,
            R.drawable.widget_bar_water_4,
            R.drawable.widget_bar_water_5
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DiaryQuickActionsWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, safeBuildViews(context, id, manager.getAppWidgetOptions(id)))
            }
        }

        private fun safeBuildViews(context: Context, widgetId: Int, options: Bundle?): RemoteViews {
            return runCatching { buildViews(context, widgetId, options) }
                .onFailure { Log.e(TAG, "Error al construir widget diario", it) }
                .getOrElse { fallbackViews(context, widgetId, options) }
        }

        private fun fallbackViews(context: Context, widgetId: Int, options: Bundle?): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                setTextViewText(R.id.widget_diary_summary, "Semana sin datos")
                setImageViewResource(R.id.widget_diary_cover, R.drawable.ic_launcher_foreground)
                setViewVisibility(
                    R.id.widget_diary_actions,
                    if (isExpanded(options)) View.VISIBLE else View.GONE
                )
                setOnClickPendingIntent(R.id.widget_add_coffee, navIntent(context, "ADD_COFFEE_ENTRY"))
                setOnClickPendingIntent(R.id.widget_add_water, navIntent(context, "ADD_WATER_ENTRY"))
                repeat(SLOT_COUNT) { i ->
                    setTextViewText(VALUE_IDS[i], "")
                    setTextViewText(LABEL_IDS[i], "")
                    setImageViewResource(COFFEE_BAR_IDS[i], COFFEE_LEVEL_DRAWABLES.first())
                    setImageViewResource(WATER_BAR_IDS[i], WATER_LEVEL_DRAWABLES.first())
                }
            }
        }

        private fun buildViews(context: Context, widgetId: Int, options: Bundle?): RemoteViews {
            val period = WidgetDataStore.readPeriod(context, widgetId)
            val points = runBlocking { WidgetDataStore.loadDiaryChart(context, period) }.takeLast(SLOT_COUNT)
            val latestCoffeeImageUrl = runBlocking { WidgetDataStore.loadLatestCoffeeImageUrl(context) }
            val maxCoffee = points.maxOfOrNull { it.caffeineMg } ?: 1
            val maxWater = points.maxOfOrNull { it.waterMl } ?: 1

            val totalCaffeine = points.sumOf { it.caffeineMg }
            val totalWater = points.sumOf { it.waterMl }

            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                val summaryPrefix = when(period) {
                    WidgetPeriod.DAY -> "Hoy:"
                    WidgetPeriod.WEEK -> "Semana:"
                    WidgetPeriod.MONTH -> "Mes:"
                }
                setTextViewText(R.id.widget_diary_summary, "$summaryPrefix ${totalCaffeine} mg · ${totalWater} ml")
                setViewVisibility(
                    R.id.widget_diary_actions,
                    if (isExpanded(options)) View.VISIBLE else View.GONE
                )
                setImageViewResource(R.id.widget_diary_cover, R.drawable.ic_launcher_foreground)
                loadBitmapFromUrl(latestCoffeeImageUrl)?.let { bitmap ->
                    setImageViewBitmap(R.id.widget_diary_cover, bitmap)
                }

                setOnClickPendingIntent(R.id.widget_add_coffee, navIntent(context, "ADD_COFFEE_ENTRY"))
                setOnClickPendingIntent(R.id.widget_add_water, navIntent(context, "ADD_WATER_ENTRY"))

                repeat(SLOT_COUNT) { index ->
                    val point = points.getOrNull(index)
                    if (point == null) {
                        setTextViewText(VALUE_IDS[index], "")
                        setTextViewText(LABEL_IDS[index], "")
                        setImageViewResource(COFFEE_BAR_IDS[index], COFFEE_LEVEL_DRAWABLES.first())
                        setImageViewResource(WATER_BAR_IDS[index], WATER_LEVEL_DRAWABLES.first())
                    } else {
                        setTextViewText(VALUE_IDS[index], "${point.caffeineMg}/${point.waterMl}")
                        setTextViewText(LABEL_IDS[index], point.label)
                        setImageViewResource(
                            COFFEE_BAR_IDS[index],
                            COFFEE_LEVEL_DRAWABLES[barLevel(point.caffeineMg, max(1, maxCoffee))]
                        )
                        setImageViewResource(
                            WATER_BAR_IDS[index],
                            WATER_LEVEL_DRAWABLES[barLevel(point.waterMl, max(1, maxWater))]
                        )
                    }
                }
            }
        }

        private fun loadBitmapFromUrl(url: String?): Bitmap? {
            if (url.isNullOrBlank()) return null
            return runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                    doInput = true
                }
                connection.inputStream.use { input ->
                    BitmapFactory.decodeStream(input)
                }?.let { original ->
                    Bitmap.createScaledBitmap(original, 96, 96, true)
                }
            }.getOrNull()
        }

        private fun barLevel(value: Int, maxValue: Int): Int {
            if (value <= 0 || maxValue <= 0) return 0
            val ratio = value.toFloat() / maxValue
            return when {
                ratio >= 0.85f -> 5
                ratio >= 0.65f -> 4
                ratio >= 0.45f -> 3
                ratio >= 0.25f -> 2
                else -> 1
            }
        }

        private fun isExpanded(options: Bundle?): Boolean {
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val maxHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0) ?: 0
            return maxOf(minHeight, maxHeight) >= EXPANDED_MIN_HEIGHT_DP
        }

        private fun navIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("shortcut_action", action)
            }
            return PendingIntent.getActivity(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
