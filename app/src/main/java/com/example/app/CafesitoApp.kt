package com.cafesito.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CafesitoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Eliminada la lógica de seeding local. 
        // La sincronización de datos ahora depende exclusivamente de Supabase mediante SyncManager.
    }
}
