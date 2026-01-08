package com.example.cafesito

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CafesitoApp : Application() {
    
    @Inject
    lateinit var seeder: com.example.cafesito.data.DataSeeder

    override fun onCreate() {
        super.onCreate()
        seeder.seedIfEmpty()
    }
}
