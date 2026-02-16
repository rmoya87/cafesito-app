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

class DiaryQuickActionsWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context))
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DiaryQuickActionsWidgetProvider::class.java)
            manager.updateAppWidget(component, buildViews(context))
        }

        private fun buildViews(context: Context): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_diary_quick_actions).apply {
                setOnClickPendingIntent(R.id.widget_diary_container, navIntent(context, "OPEN_DIARY"))
                setOnClickPendingIntent(R.id.widget_add_coffee_btn, navIntent(context, "ADD_COFFEE_ENTRY"))
                setOnClickPendingIntent(R.id.widget_add_water_btn, navIntent(context, "ADD_WATER_ENTRY"))
            }
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
