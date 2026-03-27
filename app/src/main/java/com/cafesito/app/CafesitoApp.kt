package com.cafesito.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cafesito.app.ui.timeline.TimelineNotificationWorker
import com.cafesito.app.ui.theme.AppLanguageManager
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CafesitoApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.applySavedLanguage(this)
        // GTM: si GTM_CONTAINER_ID está definido, AnalyticsHelper envía también al dataLayer.
        // Para que el contenedor procese los eventos, hay que cargarlo (p. ej. descargar desde GTM
        // y usar TagManager.getInstance(this).loadContainerPreferNonDefault(id, resId)); ver docs/ANALITICAS.md.
        scheduleTimelineNotificationWorker()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleTimelineNotificationWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<TimelineNotificationWorker>(
            15,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "timeline_notifications",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
