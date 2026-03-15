package com.cafesito.app.ui.timeline

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineNotificationStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getNotifiedIds(): Set<String> = prefs.getStringSet(KEY_NOTIFIED, emptySet()) ?: emptySet()

    fun addNotifiedIds(ids: Set<String>) {
        if (ids.isEmpty()) return
        val updated = getNotifiedIds() + ids
        prefs.edit().putStringSet(KEY_NOTIFIED, updated).apply()
    }

    private companion object {
        const val PREFS_NAME = "timeline_notifications"
        const val KEY_NOTIFIED = "notified_ids"
    }
}
