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

internal data class PantryWidgetItem(
    val coffeeName: String,
    val gramsRemaining: Int,
    val totalGrams: Int
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

    suspend fun resolveUserId(context: Context): Int? = withContext(Dispatchers.IO) {
        db(context).sessionDao().getActiveSession().first()?.userId
    }

    fun savePeriod(context: Context, widgetId: Int, period: WidgetPeriod) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
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
        val entries = db(context).diaryDao().getDiaryEntries(userId).first()
        when (period) {
            WidgetPeriod.DAY -> buildDayChart(entries)
            WidgetPeriod.WEEK -> buildWeekChart(entries)
            WidgetPeriod.MONTH -> buildMonthChart(entries)
        }
    }

    suspend fun loadPantryItems(context: Context, maxItems: Int = 4): List<PantryWidgetItem> = withContext(Dispatchers.IO) {
        val userId = resolveUserId(context) ?: return@withContext emptyList()
        val diaryDao = db(context).diaryDao()
        val coffeeDao = db(context).coffeeDao()
        val pantry = diaryDao.getPantryItems(userId).first()
            .sortedByDescending { it.gramsRemaining }
            .take(maxItems)

        pantry.map { item ->
            val coffee = coffeeDao.getCoffeeById(item.coffeeId)
            PantryWidgetItem(
                coffeeName = coffee?.nombre?.ifBlank { coffee?.marca ?: "Café" } ?: "Café",
                gramsRemaining = item.gramsRemaining,
                totalGrams = item.totalGrams
            )
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
        val grouped = today.groupBy { Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY) }

        val hours = listOf(6, 8, 10, 12, 14, 16, 18, 20, 22)
        return hours.map { hour ->
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
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val weekEntries = entries.filter { it.timestamp >= start }
        val labels = listOf("L", "M", "X", "J", "V", "S", "D")
        val grouped = weekEntries.groupBy {
            val day = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_WEEK)
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
        val start = cal.timeInMillis
        val monthEntries = entries.filter { it.timestamp >= start }
        val grouped = monthEntries.groupBy {
            Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
        }

        return (1..14).map { index ->
            val values = grouped[index].orEmpty()
            DiaryWidgetChartPoint(
                label = index.toString(),
                caffeineMg = values.filter { it.type == "CUP" }.sumOf { it.caffeineAmount },
                waterMl = values.filter { it.type == "WATER" }.sumOf { it.amountMl }
            )
        }
    }

    fun barGlyph(value: Int, maxValue: Int): String {
        if (maxValue <= 0 || value <= 0) return "▁"
        val ratio = value.toFloat() / maxValue.toFloat()
        return when {
            ratio >= 0.9f -> "█"
            ratio >= 0.75f -> "▇"
            ratio >= 0.60f -> "▆"
            ratio >= 0.45f -> "▅"
            ratio >= 0.30f -> "▄"
            ratio >= 0.15f -> "▃"
            else -> "▂"
        }
    }
}
