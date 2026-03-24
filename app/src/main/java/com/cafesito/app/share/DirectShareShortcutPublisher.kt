package com.cafesito.app.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectShareShortcutPublisher @Inject constructor() {

    fun publishSuggestedTargets(context: Context, targets: List<DirectShareTarget>) {
        if (targets.isEmpty()) return

        val shortcuts = targets.map { target ->
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(target.deepLink),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            ShortcutInfoCompat.Builder(context, "direct_share_${target.type.name.lowercase()}_${target.id}")
                .setShortLabel(target.label.take(24))
                .setLongLabel("Compartir con ${target.label}")
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(intent)
                .setLongLived(true)
                .build()
        }

        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
