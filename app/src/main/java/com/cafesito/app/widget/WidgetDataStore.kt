package com.cafesito.app.widget

import android.content.Context
import androidx.room.Room
import com.cafesito.app.data.AppDatabase
import com.cafesito.app.data.DiaryEntryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

internal enum class WidgetPeriod { DAY, WEEK, MONTH }

internal data class DiaryWidgetChartPoint(
    val label: String,
    val caffeineMg: Int,
    val waterMl: Int
)

internal object WidgetDataStore {

    private const val PREFS = "cafesito_widget_prefs"
    private const val KEY_PERIOD_PREFIX = "diary_period_"

    @Volatile
    private var database: AppDatabase? = null

    private fun db(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "cafesito_db"
            ).fallbackToDestructiveMigration().build().also { database = it }
        }
    }

    private suspend fun resolveUserId(context: Context): Int? = withContext(Dispatchers.IO) {
        val appDb = runCatching { db(context) }.getOrNull() ?: return@withContext null

        runCatching { appDb.sessionDao().getActiveSession().first()?.userId }.getOrNull()
            ?: runCatching {
                appDb.openHelper.readableDatabase
                    .query("SELECT userId FROM diary_entries ORDER BY timestamp DESC LIMIT 1")
                    .use { c -> if (c.moveToFirst()) c.getInt(0) else null }
            }.getOrNull()
            ?: runCatching {
                appDb.openHelper.readableDatabase
                    .query("SELECT userId FROM pantry_items ORDER BY lastUpdated DESC LIMIT 1")
                    .use { c -> if (c.moveToFirst()) c.getInt(0) else null }
            }.getOrNull()
    }

    fun savePeriod(context: Context, widgetId: Int, period: WidgetPeriod) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PERIOD_PREFIX + widgetId, period.name)
            .apply()
    }

    fun readPeriod(context: Context, widgetId: Int): WidgetPeriod {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PERIOD_PREFIX + widgetId, WidgetPeriod.DAY.name)
        return WidgetPeriod.entries.firstOrNull { it.name == value } ?: WidgetPeriod.DAY
    }

    suspend fun loadDiaryChart(context: Context, period: WidgetPeriod): List<DiaryWidgetChartPoint> = withContext(Dispatchers.IO) {
        val userId = resolveUserId(context) ?: return@withContext emptyList()
        val entries = runCatching { db(context).diaryDao().getDiaryEntries(userId).first() }.getOrElse { emptyList() }
        when (period) {
            WidgetPeriod.DAY -> buildDayChart(entries)
            WidgetPeriod.WEEK -> buildWeekChart(entries)
            WidgetPeriod.MONTH -> buildMonthChart(entries)
        }
    }

    private fun buildDayChart(entries: List<DiaryEntryEntity>): List<DiaryWidgetChartPoint> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val today = entries.filter { it.timestamp >= start }
        val grouped = today.groupBy { 
            val entryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            entryCal.get(Calendar.HOUR_OF_DAY)
        }

        return (0..23).map { hour ->
            val values = grouped[hour].orEmpty()
            DiaryWidgetChartPoint(
                label = String.format(Locale.getDefault(), "%02d", hour),
                caffeineMg = values.filter { it.type == "CUP" }.sumOf { it.caffeineAmount },
                waterMl = values.filter { it.type == "WATER" }.sumOf { it.amountMl }
            )
        }
    }

    private fun buildWeekChart(entries: List<DiaryEntryEntity>): List<DiaryWidgetChartPoint> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            if (timeInMillis > System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, -1)
            }
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val weekEntries = entries.filter { it.timestamp >= start }
        val labels = listOf("L", "M", "X", "J", "V", "S", "D")

        val grouped = weekEntries.groupBy {
            val entryCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val day = entryCal.get(Calendar.DAY_OF_WEEK)
            if (day == Calendar.SUNDAY) 6 else day - 2
        }

        return labels.mapIndexed { index, label ->
            val values = grouped[index].orEmpty()
            DiaryWidgetChartPoint(
                label = label,
                caffeineMg = values.filter { it.type == "CUP" }.sumOf { it.caffeineAmount },
                waterMl = values.filter { it.type == "WATER" }.sumOf { it.amountMl }
            )
        }
    }

    private fun buildMonthChart(entries: List<DiaryEntryEntity>): List<DiaryWidgetChartPoint> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val start = cal.timeInMillis
        val monthEntries = entries.filter { it.timestamp >= start }
        val grouped = monthEntries.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
        }

        return (1..maxDays).map { day ->
            val values = grouped[day].orEmpty()
            DiaryWidgetChartPoint(
                label = day.toString(),
                caffeineMg = values.filter { it.type == "CUP" }.sumOf { it.caffeineAmount },
                waterMl = values.filter { it.type == "WATER" }.sumOf { it.amountMl }
            )
        }
    }
}
