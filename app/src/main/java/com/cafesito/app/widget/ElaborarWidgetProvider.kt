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
import com.cafesito.app.brewlab.BrewLabTimerService

class ElaborarWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }

    private fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, ElaborarWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) onUpdate(context, manager, ids)
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val openBrewLabIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BrewLabTimerService.EXTRA_OPEN_BREWLAB, true)
            putExtra(BrewLabTimerService.EXTRA_ENTRY_SOURCE, LockEntryFeatureFlags.widgetEntrySource(context))
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2011,
            openBrewLabIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(context.packageName, R.layout.widget_elaborar).apply {
            setOnClickPendingIntent(R.id.widget_elaborar_root, pendingIntent)
            setOnClickPendingIntent(R.id.widget_elaborar_button, pendingIntent)
        }
    }
}
