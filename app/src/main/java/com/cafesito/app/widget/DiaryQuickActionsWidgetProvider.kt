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
import kotlin.math.max

class DiaryQuickActionsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (intent.action) {
            ACTION_FILTER_DAY -> {
                WidgetDataStore.savePeriod(context, widgetId, WidgetPeriod.DAY)
                WidgetDataStore.savePage(context, widgetId, 0)
            }
            ACTION_FILTER_WEEK -> {
                WidgetDataStore.savePeriod(context, widgetId, WidgetPeriod.WEEK)
                WidgetDataStore.savePage(context, widgetId, 0)
            }
            ACTION_FILTER_MONTH -> {
                WidgetDataStore.savePeriod(context, widgetId, WidgetPeriod.MONTH)
                WidgetDataStore.savePage(context, widgetId, 0)
            }
            ACTION_PREV_PAGE -> {
                val page = WidgetDataStore.readPage(context, widgetId)
                WidgetDataStore.savePage(context, widgetId, max(0, page - 1))
            }
            ACTION_NEXT_PAGE -> {
                val page = WidgetDataStore.readPage(context, widgetId)
                WidgetDataStore.savePage(context, widgetId, page + 1)
            }
            else -> return
        }

        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, buildViews(context, widgetId))
    }

    companion object {
        private const val PAGE_SIZE = 6

        private const val ACTION_FILTER_DAY = "com.cafesito.app.widget.FILTER_DAY"
        private const val ACTION_FILTER_WEEK = "com.cafesito.app.widget.FILTER_WEEK"
        private const val ACTION_FILTER_MONTH = "com.cafesito.app.widget.FILTER_MONTH"
        private const val ACTION_PREV_PAGE = "com.cafesito.app.widget.PREV_PAGE"
        private const val ACTION_NEXT_PAGE = "com.cafesito.app.widget.NEXT_PAGE"

        private val VALUE_IDS = intArrayOf(
            R.id.widget_bar_value_1,
            R.id.widget_bar_value_2,
            R.id.widget_bar_value_3,
            R.id.widget_bar_value_4,
            R.id.widget_bar_value_5,
            R.id.widget_bar_value_6
        )

        private val LABEL_IDS = intArrayOf(
            R.id.widget_bar_label_1,
            R.id.widget_bar_label_2,
            R.id.widget_bar_label_3,
            R.id.widget_bar_label_4,
            R.id.widget_bar_label_5,
            R.id.widget_bar_label_6
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
            val allPoints = runBlocking { WidgetDataStore.loadDiaryChart(context, period) }

            val maxPage = if (allPoints.isEmpty()) 0 else (allPoints.size - 1) / PAGE_SIZE
            val safePage = WidgetDataStore.readPage(context, widgetId).coerceIn(0, maxPage)
            if (safePage != WidgetDataStore.readPage(context, widgetId)) {
                WidgetDataStore.savePage(context, widgetId, safePage)
            }

            val start = safePage * PAGE_SIZE
            val pagePoints = allPoints.drop(start).take(PAGE_SIZE)
            val maxChartValue = pagePoints.maxOfOrNull { max(it.caffeineMg, it.waterMl / 10) } ?: 1

            val totalCaffeine = allPoints.sumOf { it.caffeineMg }
            val totalWater = allPoints.sumOf { it.waterMl }

            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                setOnClickPendingIntent(R.id.widget_diary_container, navIntent(context, "OPEN_DIARY"))
                setOnClickPendingIntent(R.id.widget_add_coffee_btn, navIntent(context, "ADD_COFFEE_ENTRY"))
                setOnClickPendingIntent(R.id.widget_add_water_btn, navIntent(context, "ADD_WATER_ENTRY"))

                setOnClickPendingIntent(R.id.widget_filter_day, actionIntent(context, widgetId, ACTION_FILTER_DAY))
                setOnClickPendingIntent(R.id.widget_filter_week, actionIntent(context, widgetId, ACTION_FILTER_WEEK))
                setOnClickPendingIntent(R.id.widget_filter_month, actionIntent(context, widgetId, ACTION_FILTER_MONTH))
                setOnClickPendingIntent(R.id.widget_prev_page, actionIntent(context, widgetId, ACTION_PREV_PAGE))
                setOnClickPendingIntent(R.id.widget_next_page, actionIntent(context, widgetId, ACTION_NEXT_PAGE))

                setTextViewText(R.id.widget_diary_summary, "${totalCaffeine} mg cafeína · ${totalWater} ml agua")
                setTextViewText(R.id.widget_page_indicator, "${safePage + 1}/${maxPage + 1}")

                setTextViewText(R.id.widget_filter_day, if (period == WidgetPeriod.DAY) "• DÍA" else "DÍA")
                setTextViewText(R.id.widget_filter_week, if (period == WidgetPeriod.WEEK) "• SEM" else "SEM")
                setTextViewText(R.id.widget_filter_month, if (period == WidgetPeriod.MONTH) "• MES" else "MES")

                VALUE_IDS.forEachIndexed { index, valueId ->
                    val point = pagePoints.getOrNull(index)
                    if (point == null) {
                        setTextViewText(valueId, "")
                        setTextViewText(LABEL_IDS[index], "")
                    } else {
                        val bar = WidgetDataStore.barGlyph(max(point.caffeineMg, point.waterMl / 10), maxChartValue)
                        setTextViewText(valueId, bar)
                        setTextViewText(LABEL_IDS[index], point.label)
                    }
                }
            }
        }

        private fun actionIntent(context: Context, widgetId: Int, action: String): PendingIntent {
            val intent = Intent(context, DiaryQuickActionsWidgetProvider::class.java).apply {
                this.action = action
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 31 + action.hashCode(),
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
