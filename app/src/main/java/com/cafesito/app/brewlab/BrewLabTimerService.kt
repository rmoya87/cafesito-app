package com.cafesito.app.brewlab

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cafesito.app.MainActivity
import com.cafesito.app.R
import com.cafesito.app.notifications.NotificationActionReceiver
import com.cafesito.app.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground Service para el timer de Brew Lab. Muestra una notificación ongoing
 * con tiempo restante total de la elaboración (suma de todas las fases) y acciones
 * Pausar / Reanudar / Cancelar. Al llegar a 00:00 hace stopForeground, muestra la
 * notificación "¿Registrar elaboración?" ("Abre Cafesito y guarda en tu diario. Continuar.")
 * y se detiene; al tocarla la app abre directamente la pantalla Consumo.
 */
class BrewLabTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var elapsedSeconds = 0
    private var totalSeconds = 0
    private var methodName: String = ""
    private var isPaused = false
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                readStateFromPrefs()
                isPaused = true
                writeStateToPrefs()
                updateNotification()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                readStateFromPrefs()
                isPaused = false
                writeStateToPrefs()
                startTickLoop()
                updateNotification()
                return START_NOT_STICKY
            }
            ACTION_CANCEL -> {
                stopTimerAndService()
                return START_NOT_STICKY
            }
        }

        val total = intent?.getIntExtra(EXTRA_TOTAL_SECONDS, 0) ?: 0
        val method = intent?.getStringExtra(EXTRA_METHOD_NAME) ?: ""
        val current = intent?.getIntExtra(EXTRA_CURRENT_SECONDS, 0) ?: 0

        if (total <= 0 || method.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }

        totalSeconds = total
        methodName = method
        elapsedSeconds = current.coerceIn(0, total)
        isPaused = false

        writeStateToPrefs()
        setRunningInPrefs(true)

        startForegroundWithType()
        startTickLoop()
        updateNotification()

        return START_STICKY
    }

    private fun startForegroundWithType() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (elapsedSeconds < totalSeconds && !isPaused) {
                delay(1000)
                elapsedSeconds++
                writeStateToPrefs()
                withContext(Dispatchers.Main) { updateNotification() }
            }
            if (elapsedSeconds >= totalSeconds) {
                withContext(Dispatchers.Main) { onTimerCompleted() }
            }
        }
    }

    private fun onTimerCompleted() {
        tickJob?.cancel()
        setRunningInPrefs(false)
        setJustEndedInPrefs(true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        showRegisterNotification()
        stopSelf()
    }

    private fun stopTimerAndService() {
        tickJob?.cancel()
        setRunningInPrefs(false)
        clearPrefs()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification() {
        val notification = buildOngoingNotification()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
    }

    private fun buildOngoingNotification(): Notification {
        // Tiempo total restante de la elaboración (todas las fases); en pantalla se muestra además el paso actual.
        val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(0)
        val mm = remaining / 60
        val ss = remaining % 60
        val timeText = String.format("%02d:%02d", mm, ss)
        val title = getString(R.string.brew_timer_notification_title, methodName)
        val content = if (isPaused) {
            getString(R.string.brew_timer_notification_paused, timeText)
        } else {
            getString(R.string.brew_timer_notification_remaining, timeText)
        }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_OPEN_BREWLAB, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (isPaused) {
            PendingIntent.getService(
                this, 0,
                Intent(this, BrewLabTimerService::class.java).setAction(ACTION_RESUME),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                this, 0,
                Intent(this, BrewLabTimerService::class.java).setAction(ACTION_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val cancelAction = PendingIntent.getService(
            this, 0,
            Intent(this, BrewLabTimerService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannels.CHANNEL_BREW_TIMER)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_cafesito_tile)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_media_pause, if (isPaused) getString(R.string.brew_timer_resume) else getString(R.string.brew_timer_pause), pauseResumeAction)
            .addAction(android.R.drawable.ic_delete, getString(R.string.brew_timer_cancel), cancelAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
    }

    private fun showRegisterNotification() {
        val openConsumo = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(EXTRA_OPEN_BREWLAB_CONSUMO, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissLater = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS_BREW_REGISTER
                putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, REGISTER_NOTIFICATION_ID)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, NotificationChannels.CHANNEL_GENERAL)
            .setContentTitle(getString(R.string.brew_timer_register_title))
            .setContentText(getString(R.string.brew_timer_register_text))
            .setSmallIcon(R.drawable.ic_cafesito_tile)
            .setContentIntent(openConsumo)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_edit, getString(R.string.brew_timer_register_yes), openConsumo)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.brew_timer_register_later), dismissLater)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(REGISTER_NOTIFICATION_ID, notification)
    }

    private fun readStateFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (totalSeconds == 0) {
            totalSeconds = prefs.getInt(KEY_TOTAL, 0)
            methodName = prefs.getString(KEY_METHOD, "") ?: ""
        }
        elapsedSeconds = prefs.getInt(KEY_ELAPSED, elapsedSeconds).coerceIn(0, totalSeconds.coerceAtLeast(1))
    }

    private fun writeStateToPrefs() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_ELAPSED, elapsedSeconds)
            .putInt(KEY_TOTAL, totalSeconds)
            .putString(KEY_METHOD, methodName)
            .putBoolean(KEY_PAUSED, isPaused)
            .apply()
    }

    private fun setRunningInPrefs(running: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_RUNNING, running)
            .apply()
    }

    private fun setJustEndedInPrefs(ended: Boolean) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_JUST_ENDED, ended)
            .apply()
    }

    private fun clearPrefs() {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(KEY_ELAPSED)
            .remove(KEY_TOTAL)
            .remove(KEY_METHOD)
            .remove(KEY_PAUSED)
            .remove(KEY_RUNNING)
            .remove(KEY_JUST_ENDED)
            .apply()
    }

    override fun onDestroy() {
        tickJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.cafesito.app.brewlab.BrewLabTimerService.PAUSE"
        const val ACTION_RESUME = "com.cafesito.app.brewlab.BrewLabTimerService.RESUME"
        const val ACTION_CANCEL = "com.cafesito.app.brewlab.BrewLabTimerService.CANCEL"
        const val EXTRA_TOTAL_SECONDS = "total_seconds"
        const val EXTRA_METHOD_NAME = "method_name"
        const val EXTRA_CURRENT_SECONDS = "current_seconds"
        const val EXTRA_OPEN_BREWLAB = "open_brewlab"
        const val EXTRA_OPEN_DIARY_FROM_BREW = "open_diary_from_brew"
        /** Al tocar la notificación «¿Registrar elaboración?» abrir Brew Lab en pantalla Consumo (resultado). */
        const val EXTRA_OPEN_BREWLAB_CONSUMO = "open_brewlab_consumo"

        private const val NOTIFICATION_ID = 9001
        /** ID de la notificación «¿Registrar elaboración?»; usado para añadir acción «Más tarde» que la cierra. */
        const val REGISTER_NOTIFICATION_ID = 9002
        const val PREFS_NAME = "brew_timer_service"
        const val KEY_ELAPSED = "elapsed_seconds"
        const val KEY_TOTAL = "total_seconds"
        const val KEY_METHOD = "method_name"
        const val KEY_PAUSED = "paused"
        const val KEY_RUNNING = "running"
        const val KEY_JUST_ENDED = "just_ended"

        fun isRunning(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_RUNNING, false)
        }

        fun start(context: Context, methodName: String, totalSeconds: Int, currentSeconds: Int = 0) {
            val intent = Intent(context, BrewLabTimerService::class.java).apply {
                putExtra(EXTRA_METHOD_NAME, methodName)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_CURRENT_SECONDS, currentSeconds)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun sendPause(context: Context) {
            context.startService(Intent(context, BrewLabTimerService::class.java).setAction(ACTION_PAUSE))
        }

        fun sendResume(context: Context) {
            context.startService(Intent(context, BrewLabTimerService::class.java).setAction(ACTION_RESUME))
        }

        fun sendCancel(context: Context) {
            context.startService(Intent(context, BrewLabTimerService::class.java).setAction(ACTION_CANCEL))
        }
    }
}
