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

class DiaryQuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val period = when (intent.action) {
            ACTION_FILTER_DAY -> WidgetPeriod.DAY
            ACTION_FILTER_WEEK -> WidgetPeriod.WEEK
            ACTION_FILTER_MONTH -> WidgetPeriod.MONTH
            else -> null
        }

        if (period != null && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetDataStore.savePeriod(context, widgetId, period)
            val manager = AppWidgetManager.getInstance(context)
            manager.updateAppWidget(widgetId, buildViews(context, widgetId))
        }
    }

    companion object {
        private const val ACTION_FILTER_DAY = "com.cafesito.app.widget.FILTER_DAY"
        private const val ACTION_FILTER_WEEK = "com.cafesito.app.widget.FILTER_WEEK"
        private const val ACTION_FILTER_MONTH = "com.cafesito.app.widget.FILTER_MONTH"
        private val VALUE_IDS = intArrayOf(
            R.id.widget_bar_value_1, R.id.widget_bar_value_2, R.id.widget_bar_value_3, R.id.widget_bar_value_4,
            R.id.widget_bar_value_5, R.id.widget_bar_value_6, R.id.widget_bar_value_7, R.id.widget_bar_value_8,
            R.id.widget_bar_value_9, R.id.widget_bar_value_10, R.id.widget_bar_value_11, R.id.widget_bar_value_12,
            R.id.widget_bar_value_13, R.id.widget_bar_value_14
        )
        private val LABEL_IDS = intArrayOf(
            R.id.widget_bar_label_1, R.id.widget_bar_label_2, R.id.widget_bar_label_3, R.id.widget_bar_label_4,
            R.id.widget_bar_label_5, R.id.widget_bar_label_6, R.id.widget_bar_label_7, R.id.widget_bar_label_8,
            R.id.widget_bar_label_9, R.id.widget_bar_label_10, R.id.widget_bar_label_11, R.id.widget_bar_label_12,
            R.id.widget_bar_label_13, R.id.widget_bar_label_14
        )

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DiaryQuickActionsWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, buildViews(context, id))
            }
        }

        private fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val period = WidgetDataStore.readPeriod(context, widgetId)
            val chart = runBlocking { WidgetDataStore.loadDiaryChart(context, period) }
            val maxChartValue = chart.maxOfOrNull { maxOf(it.caffeineMg, it.waterMl / 10) } ?: 1

            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                setOnClickPendingIntent(R.id.widget_diary_container, navIntent(context, "OPEN_DIARY"))
                setOnClickPendingIntent(R.id.widget_add_coffee_btn, navIntent(context, "ADD_COFFEE_ENTRY"))
                setOnClickPendingIntent(R.id.widget_add_water_btn, navIntent(context, "ADD_WATER_ENTRY"))

                setOnClickPendingIntent(R.id.widget_filter_day, periodIntent(context, widgetId, ACTION_FILTER_DAY))
                setOnClickPendingIntent(R.id.widget_filter_week, periodIntent(context, widgetId, ACTION_FILTER_WEEK))
                setOnClickPendingIntent(R.id.widget_filter_month, periodIntent(context, widgetId, ACTION_FILTER_MONTH))

                val summary = chart.sumOf { it.caffeineMg }
                setTextViewText(R.id.widget_diary_summary, "☕ ${summary}mg · 💧 ${chart.sumOf { it.waterMl }}ml")

                setTextViewText(R.id.widget_filter_day, if (period == WidgetPeriod.DAY) "● Día" else "Día")
                setTextViewText(R.id.widget_filter_week, if (period == WidgetPeriod.WEEK) "● Semana" else "Semana")
                setTextViewText(R.id.widget_filter_month, if (period == WidgetPeriod.MONTH) "● Mes" else "Mes")

                VALUE_IDS.forEachIndexed { index, viewId ->
                    val point = chart.getOrNull(index)
                    if (point == null) {
                        setTextViewText(viewId, "")
                        setTextViewText(LABEL_IDS[index], "")
                    } else {
                        val bars = WidgetDataStore.barGlyph(maxOf(point.caffeineMg, point.waterMl / 10), maxChartValue)
                        setTextViewText(viewId, bars)
                        setTextViewText(LABEL_IDS[index], point.label)
                    }
                }
            }
        }

        private fun periodIntent(context: Context, widgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, DiaryQuickActionsWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId + action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
