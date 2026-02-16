package com.cafesito.app.startup

import android.content.Intent

object ShortcutActionResolver {
    fun resolve(intent: Intent?): String? {
        val directAction = intent?.getStringExtra("shortcut_action")
        if (!directAction.isNullOrBlank()) return directAction

        return when (intent?.data?.toString()) {
            "cafesito://shortcut/search" -> "SEARCH"
            "cafesito://shortcut/new_post" -> "NEW_POST"
            "cafesito://shortcut/new_review" -> "NEW_REVIEW"
            else -> null
        }
    }
}
