package com.cafesito.app.startup

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache

object AppUiInitializer {
    fun configure(activity: Activity) {
        setupCoil(activity)
        setupEdgeToEdge(activity)
    }

    private fun setupCoil(activity: Activity) {
        val imageLoader = ImageLoader.Builder(activity)
            .memoryCache { MemoryCache.Builder(activity).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(activity.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)
    }

    private fun setupEdgeToEdge(activity: Activity) {
        if (activity is ComponentActivity) {
            activity.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            )
        }
    }
}
