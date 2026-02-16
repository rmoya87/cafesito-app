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

        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, buildViews(context, widgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        handleFilterAction(context, intent)
    }



    private fun handleFilterAction(context: Context, intent: Intent) {
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val period = when (intent.action) {
            ACTION_FILTER_DAY -> WidgetPeriod.DAY
            ACTION_FILTER_WEEK -> WidgetPeriod.WEEK
            ACTION_FILTER_MONTH -> WidgetPeriod.MONTH
            else -> return
        }

        WidgetDataStore.savePeriod(context, widgetId, period)
        AppWidgetManager.getInstance(context).updateAppWidget(widgetId, buildViews(context, widgetId))
    }
    companion object {
        private const val SLOT_COUNT = 31

        private const val ACTION_FILTER_DAY = "com.cafesito.app.widget.FILTER_DAY"
        private const val ACTION_FILTER_WEEK = "com.cafesito.app.widget.FILTER_WEEK"
        private const val ACTION_FILTER_MONTH = "com.cafesito.app.widget.FILTER_MONTH"

        private val COFFEE_IDS = intArrayOf(
            R.id.widget_bar_coffee_1, R.id.widget_bar_coffee_2, R.id.widget_bar_coffee_3, R.id.widget_bar_coffee_4, R.id.widget_bar_coffee_5,
            R.id.widget_bar_coffee_6, R.id.widget_bar_coffee_7, R.id.widget_bar_coffee_8, R.id.widget_bar_coffee_9, R.id.widget_bar_coffee_10,
            R.id.widget_bar_coffee_11, R.id.widget_bar_coffee_12, R.id.widget_bar_coffee_13, R.id.widget_bar_coffee_14, R.id.widget_bar_coffee_15,
            R.id.widget_bar_coffee_16, R.id.widget_bar_coffee_17, R.id.widget_bar_coffee_18, R.id.widget_bar_coffee_19, R.id.widget_bar_coffee_20,
            R.id.widget_bar_coffee_21, R.id.widget_bar_coffee_22, R.id.widget_bar_coffee_23, R.id.widget_bar_coffee_24, R.id.widget_bar_coffee_25,
            R.id.widget_bar_coffee_26, R.id.widget_bar_coffee_27, R.id.widget_bar_coffee_28, R.id.widget_bar_coffee_29, R.id.widget_bar_coffee_30,
            R.id.widget_bar_coffee_31
        )

        private val WATER_IDS = intArrayOf(
            R.id.widget_bar_water_1, R.id.widget_bar_water_2, R.id.widget_bar_water_3, R.id.widget_bar_water_4, R.id.widget_bar_water_5,
            R.id.widget_bar_water_6, R.id.widget_bar_water_7, R.id.widget_bar_water_8, R.id.widget_bar_water_9, R.id.widget_bar_water_10,
            R.id.widget_bar_water_11, R.id.widget_bar_water_12, R.id.widget_bar_water_13, R.id.widget_bar_water_14, R.id.widget_bar_water_15,
            R.id.widget_bar_water_16, R.id.widget_bar_water_17, R.id.widget_bar_water_18, R.id.widget_bar_water_19, R.id.widget_bar_water_20,
            R.id.widget_bar_water_21, R.id.widget_bar_water_22, R.id.widget_bar_water_23, R.id.widget_bar_water_24, R.id.widget_bar_water_25,
            R.id.widget_bar_water_26, R.id.widget_bar_water_27, R.id.widget_bar_water_28, R.id.widget_bar_water_29, R.id.widget_bar_water_30,
            R.id.widget_bar_water_31
        )

        private val LABEL_IDS = intArrayOf(
            R.id.widget_bar_label_1, R.id.widget_bar_label_2, R.id.widget_bar_label_3, R.id.widget_bar_label_4, R.id.widget_bar_label_5,
            R.id.widget_bar_label_6, R.id.widget_bar_label_7, R.id.widget_bar_label_8, R.id.widget_bar_label_9, R.id.widget_bar_label_10,
            R.id.widget_bar_label_11, R.id.widget_bar_label_12, R.id.widget_bar_label_13, R.id.widget_bar_label_14, R.id.widget_bar_label_15,
            R.id.widget_bar_label_16, R.id.widget_bar_label_17, R.id.widget_bar_label_18, R.id.widget_bar_label_19, R.id.widget_bar_label_20,
            R.id.widget_bar_label_21, R.id.widget_bar_label_22, R.id.widget_bar_label_23, R.id.widget_bar_label_24, R.id.widget_bar_label_25,
            R.id.widget_bar_label_26, R.id.widget_bar_label_27, R.id.widget_bar_label_28, R.id.widget_bar_label_29, R.id.widget_bar_label_30,
            R.id.widget_bar_label_31
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
            val points = runBlocking { WidgetDataStore.loadDiaryChart(context, period) }
            val maxCoffee = points.maxOfOrNull { it.caffeineMg } ?: 1
            val maxWater = points.maxOfOrNull { it.waterMl } ?: 1

            val totalCaffeine = points.sumOf { it.caffeineMg }
            val totalWater = points.sumOf { it.waterMl }

            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                setOnClickPendingIntent(R.id.widget_diary_container, navIntent(context, "OPEN_DIARY"))

                setOnClickPendingIntent(R.id.widget_filter_day, actionIntent(context, widgetId, ACTION_FILTER_DAY))
                setOnClickPendingIntent(R.id.widget_filter_week, actionIntent(context, widgetId, ACTION_FILTER_WEEK))
                setOnClickPendingIntent(R.id.widget_filter_month, actionIntent(context, widgetId, ACTION_FILTER_MONTH))

                setTextViewText(R.id.widget_diary_summary, "${totalCaffeine} mg cafeína · ${totalWater} ml agua")
                setTextViewText(R.id.widget_filter_day, if (period == WidgetPeriod.DAY) "• DÍA" else "DÍA")
                setTextViewText(R.id.widget_filter_week, if (period == WidgetPeriod.WEEK) "• SEM" else "SEM")
                setTextViewText(R.id.widget_filter_month, if (period == WidgetPeriod.MONTH) "• MES" else "MES")

                repeat(SLOT_COUNT) { index ->
                    val point = points.getOrNull(index)
                    if (point == null) {
                        setTextViewText(COFFEE_IDS[index], "")
                        setTextViewText(WATER_IDS[index], "")
                        setTextViewText(LABEL_IDS[index], "")
                    } else {
                        setTextViewText(COFFEE_IDS[index], WidgetDataStore.barGlyph(point.caffeineMg, max(1, maxCoffee)))
                        setTextViewText(WATER_IDS[index], WidgetDataStore.barGlyph(point.waterMl, max(1, maxWater)))
                        setTextViewText(LABEL_IDS[index], point.label)
                    }
                }
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 31 + action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
