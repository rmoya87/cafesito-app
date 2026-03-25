package com.cafesito.app.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DirectShareShortcutPublisher @Inject constructor() {

    companion object {
        const val EXTRA_DIRECT_SHARE_TARGET_TYPE = "direct_share_target_type"
        const val EXTRA_DIRECT_SHARE_TARGET_ID = "direct_share_target_id"
    }

    suspend fun publishSuggestedTargets(context: Context, targets: List<DirectShareTarget>) {
        if (targets.isEmpty()) return

        // Evita caché de iconos en launchers OEM (MIUI/HyperOS) al reciclar IDs.
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)

        val shortcuts = targets.map { target ->
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(target.deepLink),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_DIRECT_SHARE_TARGET_TYPE, target.type.name.lowercase())
                putExtra(EXTRA_DIRECT_SHARE_TARGET_ID, target.id)
            }
            val iconRes = when (target.type) {
                DirectShareTargetType.LIST -> R.drawable.shortcut_list_transparent_24
                DirectShareTargetType.CONTACT -> android.R.drawable.ic_menu_myplaces
            }
            val contactAvatarIcon = if (target.type == DirectShareTargetType.CONTACT) {
                target.avatarUrl?.let { avatarUrl ->
                    loadBitmapFromUrl(avatarUrl)?.let { IconCompat.createWithAdaptiveBitmap(it) }
                }
            } else {
                null
            }
            val fallbackIcon = drawableIcon(context, iconRes)?.let { bitmap ->
                if (target.type == DirectShareTargetType.LIST) {
                    // Lista sin fondo para que el launcher no pinte "placa" gris adicional.
                    IconCompat.createWithBitmap(bitmap)
                } else {
                    IconCompat.createWithAdaptiveBitmap(bitmap)
                }
            } ?: IconCompat.createWithResource(context, iconRes)
            val builder = ShortcutInfoCompat.Builder(
                context,
                "direct_share_v2_${target.type.name.lowercase()}_${target.id}"
            )

            builder
                .setShortLabel(target.label.take(24))
                .setLongLabel("Compartir con ${target.label}")
                .setIcon(contactAvatarIcon ?: fallbackIcon)
                .setIntent(intent)
                .setLongLived(true)
                .also {
                    if (target.type == DirectShareTargetType.CONTACT) {
                        it.setPerson(
                            Person.Builder()
                                .setName(target.label)
                                .setIcon(contactAvatarIcon ?: fallbackIcon)
                                .build()
                        )
                    }
                }
                .build()
        }

        ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
    }

    private fun loadBitmapFromUrl(url: String): Bitmap? = runCatching {
        java.net.URL(url).openStream().use { input ->
            BitmapFactory.decodeStream(input)
        }
    }.getOrNull()

    private fun drawableIcon(context: Context, resId: Int): Bitmap? = runCatching {
        val drawable: Drawable = androidx.core.content.ContextCompat.getDrawable(context, resId) ?: return null
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }.getOrNull()
}
