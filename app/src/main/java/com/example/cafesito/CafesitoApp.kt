package com.example.cafesito

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class CafesitoApp : Application() {
    
    @Inject
    lateinit var seeder: com.example.cafesito.data.DataSeeder

    override fun onCreate() {
        super.onCreate()
        
        // Ejecutamos la siembra de datos en un scope de corrutina
        // para no bloquear el hilo principal y soportar la función suspend
        MainScope().launch {
            seeder.seedIfNeeded()
        }
    }
}
