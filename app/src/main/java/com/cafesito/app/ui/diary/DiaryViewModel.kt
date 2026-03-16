package com.cafesito.app.ui.diary

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafesito.app.data.*
import com.cafesito.shared.domain.diary.DiaryAnalyticsTargets
import com.cafesito.shared.domain.diary.DiaryPeriod as SharedDiaryPeriod
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

/** Convierte imagen desde Uri a WebP (calidad 85) para subir a Supabase. */
private fun Context.uriToWebPBytes(uri: Uri): ByteArray? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            val bitmap = BitmapFactory.decodeStream(input) ?: return@runCatching null
            val out = ByteArrayOutputStream()
            if (bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)) out.toByteArray() else null
        }
    }.getOrNull()
}

/** Devuelve el lunes 00:00 de la semana que contiene la fecha dada (en ms). */
fun getMondayOfWeek(dateMs: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = dateMs }
    val day = c.get(Calendar.DAY_OF_WEEK)
    val diff = if (day == Calendar.SUNDAY) -6 else Calendar.MONDAY - day
    c.add(Calendar.DAY_OF_MONTH, diff)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    return c.timeInMillis
}

/** Formato "d/M - d/M" para el rango de la semana (lunes a domingo). */
fun formatWeekRange(mondayMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = mondayMs }
    val d1 = cal.get(Calendar.DAY_OF_MONTH)
    val m1 = cal.get(Calendar.MONTH) + 1
    cal.add(Calendar.DAY_OF_MONTH, 6)
    val d2 = cal.get(Calendar.DAY_OF_MONTH)
    val m2 = cal.get(Calendar.MONTH) + 1
    return "$d1/$m1 - $d2/$m2"
}

/** Formato "marzo 2025" para el mes (primer día del mes en ms). */
fun formatMonthYear(monthStartMs: Long): String {
    return java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.forLanguageTag("es-ES")).format(java.util.Date(monthStartMs))
}

/** Solo nombre del mes (para selector en topbar cuando periodo es MES). */
fun formatMonthOnly(monthStartMs: Long): String {
    return java.text.SimpleDateFormat("MMMM", java.util.Locale.forLanguageTag("es-ES")).format(java.util.Date(monthStartMs))
}

enum class DiaryPeriod { HOY, SEMANA, MES }

data class ChartEntry(
    val label: String,
    val caffeine: Int,
    val water: Int
)

data class DiaryAnalytics(
    val chartData: List<ChartEntry>,
    val waterCount: Int,
    val totalWaterMl: Int,
    val cupsCount: Int,
    val totalCaffeine: Int,
    val averageCaffeine: Int,
    /** Media de un día en los últimos 30 días (común Android/iOS/webapp). */
    val avgCaffeineLast30: Int,
    val avgCupsLast30: Float,
    val avgHydrationPctLast30: Int,
    /** Porcentaje de tendencia cafeína vs objetivo (lógica compartida con webApp). */
    val caffeineTrendPct: Int,
    /** Porcentaje de tendencia hidratación vs objetivo (lógica compartida con webApp). */
    val hydrationTrendPct: Int,
    /** Progreso hidratación 0..100 (lógica compartida con webApp). */
    val hydrationProgressPct: Int,
    val period: DiaryPeriod,
    /** Solo en semana: true si la semana seleccionada es la actual (para mostrar "· Hoy" en el gráfico). */
    val isCurrentWeek: Boolean = false
)

data class DiaryHabitStats(
    val avgCups: String,
    val mostSize: String,
    val mostMethod: String,
    val busiestDay: String
)

data class DiaryConsumptionStats(
    val momentPctMorning: Int,
    val momentPctAfternoon: Int,
    val momentPctEvening: Int,
    val avgCaffeine: Int,
    val avgDose: Int,
    val mostFormat: String,
    val pantryDaysLeft: Int?
)

data class TriedCoffeeItem(
    val coffee: Coffee,
    val firstTriedMs: Long
)

data class DiaryBaristaStats(
    val distinctCoffees: Int,
    val distinctRoasters: Int,
    val favoriteOrigin: String,
    val coffeesWithFirstTried: List<TriedCoffeeItem>
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val coffeeRepository: CoffeeRepository,
    private val syncManager: SyncManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(DiaryPeriod.SEMANA)
    val selectedPeriod: StateFlow<DiaryPeriod> = _selectedPeriod.asStateFlow()

    private val calendarTodayStart: Long
        get() {
            val c = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }
    private val _selectedDiaryDateMs = MutableStateFlow(0L)
    val selectedDiaryDateMs: StateFlow<Long> = _selectedDiaryDateMs.asStateFlow()

    init {
        if (_selectedDiaryDateMs.value == 0L) _selectedDiaryDateMs.value = getMondayOfWeek(System.currentTimeMillis())
    }

    fun setSelectedDiaryDateMs(dayStartMs: Long) { _selectedDiaryDateMs.value = dayStartMs }
    fun prevWeek() {
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.DAY_OF_MONTH, -7)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }
    fun nextWeek() {
        val nextMonday = _selectedDiaryDateMs.value + 604800000L
        val currentWeekMonday = getMondayOfWeek(System.currentTimeMillis())
        if (nextMonday > currentWeekMonday) return
        _selectedDiaryDateMs.value = nextMonday
    }
    fun prevDay() {
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.DAY_OF_MONTH, -1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }
    fun nextDay() {
        if (_selectedDiaryDateMs.value >= calendarTodayStart) return
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }

    fun prevMonth() {
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        c.add(Calendar.MONTH, -1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }

    fun nextMonth() {
        val c = Calendar.getInstance().apply { timeInMillis = _selectedDiaryDateMs.value }
        val now = Calendar.getInstance()
        if (c.get(Calendar.YEAR) >= now.get(Calendar.YEAR) && c.get(Calendar.MONTH) >= now.get(Calendar.MONTH)) return
        c.add(Calendar.MONTH, 1)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        _selectedDiaryDateMs.value = c.timeInMillis
    }

    private val allEntriesFlow = diaryRepository.getDiaryEntries()

    /** Considera una entrada como taza de café para estadísticas (incl. método Rápido y resto de BrewLab). */
    private fun isCupEntry(e: DiaryEntryEntity): Boolean =
        e.type.equals(DiaryRepository.TYPE_CUP, ignoreCase = true) ||
            (e.type != DiaryRepository.TYPE_WATER && e.preparationType.startsWith("Lab:"))

    /** Considera una entrada como agua para estadísticas. */
    private fun isWaterEntry(e: DiaryEntryEntity): Boolean =
        e.type.equals(DiaryRepository.TYPE_WATER, ignoreCase = true)

    /** Todas las entradas del diario (sin filtrar por periodo), para el calendario y marcar días con café/agua. */
    val allDiaryEntries: StateFlow<List<DiaryEntryEntity>> = allEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = combine(
        allEntriesFlow,
        _selectedPeriod,
        _selectedDiaryDateMs
    ) { entries, period, selectedDateMs ->
        val calendar = Calendar.getInstance()
        val startTime = when (period) {
            DiaryPeriod.HOY -> selectedDateMs
            DiaryPeriod.SEMANA -> getMondayOfWeek(selectedDateMs)
            DiaryPeriod.MES -> {
                calendar.timeInMillis = selectedDateMs
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                calendar.timeInMillis
            }
        }
        val endTime = when (period) {
            DiaryPeriod.HOY -> startTime + 86400000L
            DiaryPeriod.SEMANA -> {
                val weekEnd = startTime + 604800000L
                val currentWeekMonday = getMondayOfWeek(System.currentTimeMillis())
                val selectedWeekMonday = getMondayOfWeek(selectedDateMs)
                if (selectedWeekMonday == currentWeekMonday) {
                    minOf(weekEnd, System.currentTimeMillis() + 1)
                } else {
                    weekEnd
                }
            }
            DiaryPeriod.MES -> {
                calendar.timeInMillis = startTime
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
        }
        entries
            .filter { it.timestamp >= startTime && it.timestamp < endTime }
            .sortedByDescending { it.timestamp }
    }
    .onEach { _isLoading.value = false } 
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pantryItems: StateFlow<List<PantryItemWithDetails>> = diaryRepository.getPantryItems()
        .onEach { _isLoading.value = false } 
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val finishedCoffees: StateFlow<List<FinishedCoffeeWithDetails>> = diaryRepository.getFinishedCoffees()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableCoffees: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Catálogo + custom para número y listado de cafés probados (mismo criterio que CafesProbadosViewModel). */
    private val coffeesForBarista: StateFlow<List<CoffeeWithDetails>> = coffeeRepository.allCoffeesIncludingCustom
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val analytics: StateFlow<DiaryAnalytics> = combine(allEntriesFlow, _selectedPeriod, _selectedDiaryDateMs) { entries, period, selectedDateMs ->
        try {
        val calendar = Calendar.getInstance()
        val now = System.currentTimeMillis()
        calendar.timeInMillis = now
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        val (currentEntries, previousEntries, averageValue) = when (period) {
            DiaryPeriod.HOY -> {
                val dayStart = selectedDateMs
                val dayEnd = dayStart + 86400000L
                val dailySums = entries.groupBy {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
                }.values.map { it.filter { e -> isCupEntry(e) }.sumOf { e -> e.caffeineAmount } }
                Triple(
                    entries.filter { it.timestamp in dayStart until dayEnd },
                    entries.filter { it.timestamp in (dayStart - 86400000) until dayStart },
                    if (dailySums.isEmpty()) 0 else dailySums.average().toInt()
                )
            }
            DiaryPeriod.SEMANA -> {
                val startOfWeek = getMondayOfWeek(selectedDateMs)
                val weekEnd = startOfWeek + 604800000L
                val currentWeekMonday = getMondayOfWeek(now)
                val effectiveEndOfWeek = if (startOfWeek == currentWeekMonday) minOf(weekEnd, now + 1) else weekEnd
                val weeklySums = entries.groupBy { val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }; "${c.get(Calendar.YEAR)}-${c.get(Calendar.WEEK_OF_YEAR)}" }.values.map { it.filter { e -> isCupEntry(e) }.sumOf { e -> e.caffeineAmount } }
                Triple(
                    entries.filter { it.timestamp >= startOfWeek && it.timestamp < effectiveEndOfWeek },
                    entries.filter { it.timestamp in (startOfWeek - 604800000) until startOfWeek },
                    if (weeklySums.isEmpty()) 0 else weeklySums.average().toInt()
                )
            }
            DiaryPeriod.MES -> {
                calendar.timeInMillis = selectedDateMs
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val startOfSelectedMonth = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                val endOfSelectedMonth = calendar.timeInMillis
                val monthlySums = entries.groupBy { val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }; "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH)}" }.values.map { it.filter { e -> isCupEntry(e) }.sumOf { e -> e.caffeineAmount } }
                Triple(entries.filter { it.timestamp >= startOfSelectedMonth && it.timestamp < endOfSelectedMonth }, emptyList<DiaryEntryEntity>(), if(monthlySums.isEmpty()) 0 else monthlySums.average().toInt())
            }
        }

        val totalCaffeine = currentEntries.filter { isCupEntry(it) }.sumOf { it.caffeineAmount }
        val totalWaterMl = currentEntries.filter { isWaterEntry(it) }.sumOf { it.amountMl }

        val sharedPeriod = when (period) {
            DiaryPeriod.HOY -> SharedDiaryPeriod.HOY
            DiaryPeriod.SEMANA -> SharedDiaryPeriod.SEMANA
            DiaryPeriod.MES -> SharedDiaryPeriod.MES
        }
        val caffeineTarget = DiaryAnalyticsTargets.caffeineTargetMg(sharedPeriod)
        val hydrationTarget = DiaryAnalyticsTargets.hydrationTargetMl(sharedPeriod)
        val caffeineTrendPct = DiaryAnalyticsTargets.trendPercent(totalCaffeine, caffeineTarget)
        val hydrationTrendPct = DiaryAnalyticsTargets.trendPercent(totalWaterMl, hydrationTarget)
        val hydrationProgressPct = DiaryAnalyticsTargets.hydrationProgressPercent(totalWaterMl, sharedPeriod)

        val dayMs = 86400000L
        val hydrationTargetDay = 2000
        val last30Avg = run {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis
            var sumCaffeine = 0; var sumCups = 0; var sumHydrationPct = 0
            for (i in 0 until 30) {
                val dayStart = todayStart - (29 - i) * dayMs
                val dayEnd = dayStart + dayMs
                val dayEntries = entries.filter { it.timestamp in dayStart until dayEnd }
                sumCaffeine += dayEntries.filter { isCupEntry(it) }.sumOf { it.caffeineAmount }
                sumCups += dayEntries.count { isCupEntry(it) }
                val waterMl = dayEntries.filter { isWaterEntry(it) }.sumOf { it.amountMl }
                sumHydrationPct += (waterMl.toDouble() / hydrationTargetDay * 100).toInt().coerceIn(0, 100)
            }
            Triple(sumCaffeine / 30, sumCups / 30.0, sumHydrationPct / 30)
        }

        val chartData = when (period) {
            DiaryPeriod.HOY -> {
                val hourlyData = currentEntries.groupBy { 
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY)
                }
                (0..23).map { hour -> 
                    val caffeineVal = hourlyData[hour]?.filter { isCupEntry(it) }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = hourlyData[hour]?.filter { isWaterEntry(it) }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(String.format("%02d:00", hour), caffeineVal, waterVal)
                }
            }
            DiaryPeriod.SEMANA -> {
                val weeklyData = currentEntries.groupBy { 
                    val day = Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_WEEK)
                    if (day == Calendar.SUNDAY) 6 else day - 2
                }
                listOf("L", "M", "X", "J", "V", "S", "D").mapIndexed { i, d -> 
                    val caffeineVal = weeklyData[i]?.filter { isCupEntry(it) }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = weeklyData[i]?.filter { isWaterEntry(it) }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(d, caffeineVal, waterVal)
                }
            }
            DiaryPeriod.MES -> {
                calendar.timeInMillis = selectedDateMs
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthlyData = currentEntries.groupBy {
                    Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.DAY_OF_MONTH)
                }
                (1..maxDays).map { day ->
                    val caffeineVal = monthlyData[day]?.filter { isCupEntry(it) }?.sumOf { it.caffeineAmount } ?: 0
                    val waterVal = monthlyData[day]?.filter { isWaterEntry(it) }?.sumOf { it.amountMl } ?: 0
                    ChartEntry(day.toString(), caffeineVal, waterVal)
                }
            }
        }

        val isCurrentWeek = period == DiaryPeriod.SEMANA && getMondayOfWeek(System.currentTimeMillis()) == selectedDateMs
        DiaryAnalytics(
            chartData = chartData,
            waterCount = currentEntries.count { isWaterEntry(it) },
            totalWaterMl = totalWaterMl,
            cupsCount = currentEntries.count { isCupEntry(it) },
            totalCaffeine = totalCaffeine,
            averageCaffeine = averageValue,
            avgCaffeineLast30 = last30Avg.first,
            avgCupsLast30 = last30Avg.second.toFloat(),
            avgHydrationPctLast30 = last30Avg.third,
            caffeineTrendPct = caffeineTrendPct,
            hydrationTrendPct = hydrationTrendPct,
            hydrationProgressPct = hydrationProgressPct,
            period = period,
            isCurrentWeek = isCurrentWeek
        )
        } catch (e: Exception) {
            defaultDiaryAnalytics()
        }
    }
    .onEach { _isLoading.value = false }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultDiaryAnalytics())

    private fun defaultDiaryAnalytics(): DiaryAnalytics {
        val chartData = when (_selectedPeriod.value) {
            DiaryPeriod.HOY -> (0..23).map { ChartEntry(String.format("%02d:00", it), 0, 0) }
            DiaryPeriod.SEMANA -> listOf("L", "M", "X", "J", "V", "S", "D").map { ChartEntry(it, 0, 0) }
            DiaryPeriod.MES -> (1..31).map { ChartEntry(it.toString(), 0, 0) }
        }
        return DiaryAnalytics(
            chartData = chartData,
            waterCount = 0,
            totalWaterMl = 0,
            cupsCount = 0,
            totalCaffeine = 0,
            averageCaffeine = 0,
            avgCaffeineLast30 = 0,
            avgCupsLast30 = 0f,
            avgHydrationPctLast30 = 0,
            caffeineTrendPct = 0,
            hydrationTrendPct = 0,
            hydrationProgressPct = 0,
            period = _selectedPeriod.value,
            isCurrentWeek = false
        )
    }

    private val dayNames = listOf("Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado")
    private val sinMetodo = "—"

    private fun stripLabPrefix(prep: String): String {
        val raw = if (prep.contains("|")) prep.substringBefore("|").trim() else prep
        return raw.replace(Regex("^(?:lab:\\s*|elaboracion:\\s*)", RegexOption.IGNORE_CASE), "").trim().ifEmpty { sinMetodo }
    }

    val habitStats: StateFlow<DiaryHabitStats> = combine(diaryEntries, _selectedPeriod, _selectedDiaryDateMs) { entries, period, selectedMs ->
        val coffeeEntries = entries.filter { isCupEntry(it) }
        val now = System.currentTimeMillis()
        val periodDays = when (period) {
            DiaryPeriod.HOY -> 1
            DiaryPeriod.SEMANA -> {
                val currentWeekMonday = getMondayOfWeek(now)
                val selectedWeekMonday = getMondayOfWeek(selectedMs)
                if (selectedWeekMonday == currentWeekMonday) {
                    val cal = Calendar.getInstance().apply { timeInMillis = now }
                    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                    val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                    (daysFromMonday + 1).coerceIn(1, 7)
                } else 7
            }
            DiaryPeriod.MES -> {
                val calSelected = Calendar.getInstance().apply { timeInMillis = selectedMs }
                val calNow = Calendar.getInstance()
                val isCurrentMonth = calSelected.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                    calSelected.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
                if (isCurrentMonth) calNow.get(Calendar.DAY_OF_MONTH)
                else calSelected.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
        val avgCups = if (periodDays > 0) "%.1f".format(Locale.ROOT, coffeeEntries.size.toDouble() / periodDays) else "0"
        val sizeCount = coffeeEntries.groupingBy { (it.sizeLabel ?: "").trim().ifEmpty { "—" } }.eachCount()
        val mostSize = sizeCount.maxByOrNull { it.value }?.key ?: "—"
        val methodCount = coffeeEntries.groupingBy { e -> stripLabPrefix(e.preparationType) }.eachCount()
        val mostMethod = methodCount.maxByOrNull { it.value }?.key ?: sinMetodo
        val dayCount = coffeeEntries.groupingBy { e ->
            Calendar.getInstance().apply { timeInMillis = e.timestamp }.get(Calendar.DAY_OF_WEEK) - 1
        }.eachCount()
        val busiestDay = dayCount.maxByOrNull { it.value }?.key?.let { dayNames.getOrNull(it) } ?: "—"
        DiaryHabitStats(avgCups = avgCups, mostSize = mostSize, mostMethod = mostMethod, busiestDay = busiestDay)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DiaryHabitStats("0", "—", sinMetodo, "—"))

    @Suppress("UNCHECKED_CAST")
    val consumptionStats: StateFlow<DiaryConsumptionStats> = combine(
        diaryEntries,
        allDiaryEntries,
        availableCoffees,
        pantryItems,
        _selectedPeriod,
        _selectedDiaryDateMs
    ) { arr ->
        val entries = arr[0] as List<DiaryEntryEntity>
        val allEntries = arr[1] as List<DiaryEntryEntity>
        val coffees = arr[2] as List<CoffeeWithDetails>
        val pantry = arr[3] as List<PantryItemWithDetails>
        val period = arr[4] as DiaryPeriod
        val selectedMs = arr[5] as Long
        val coffeeEntries = entries.filter { isCupEntry(it) }
        val coffeeById = coffees.associate { it.coffee.id to it.coffee }
        val periodDays = when (period) {
            DiaryPeriod.HOY -> 1
            DiaryPeriod.SEMANA -> 7
            DiaryPeriod.MES -> {
                val c = Calendar.getInstance().apply { timeInMillis = selectedMs }
                c.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
        }
        var morn = 0; var after = 0; var even = 0
        coffeeEntries.forEach { e ->
            val h = Calendar.getInstance().apply { timeInMillis = e.timestamp }.get(Calendar.HOUR_OF_DAY)
            when {
                h in 6 until 12 -> morn++
                h in 12 until 20 -> after++
                else -> even++
            }
        }
        val total = morn + after + even
        fun pct(n: Int) = if (total > 0) (n * 100 / total) else 0
        val avgCaffeine = if (coffeeEntries.isNotEmpty()) coffeeEntries.sumOf { it.caffeineAmount } / coffeeEntries.size else 0
        val withDose = coffeeEntries.filter { it.coffeeGrams > 0 }
        val avgDose = if (withDose.isNotEmpty()) withDose.sumOf { it.coffeeGrams } / withDose.size else 0
        val formatCount = coffeeEntries.mapNotNull { e -> e.coffeeId?.let { id -> coffeeById[id]?.formato?.trim()?.ifEmpty { null } ?: "—" } }
            .groupingBy { it }.eachCount()
        val mostFormat = formatCount.maxByOrNull { it.value }?.key ?: "—"
        val now = System.currentTimeMillis()
        val thirtyDaysMs = 30L * 86400000
        val from30 = now - thirtyDaysMs
        val coffeeEntriesLast30 = allEntries.filter { isCupEntry(it) && it.timestamp >= from30 }
        val totalGramsLast30 = coffeeEntriesLast30.sumOf { it.coffeeGrams }
        val avgGramsPerDayPantry = if (totalGramsLast30 > 0) totalGramsLast30.toDouble() / 30 else 0.0
        val totalPantryGrams = pantry.sumOf { it.pantryItem.gramsRemaining.coerceAtLeast(0) }
        val pantryDaysLeft = if (avgGramsPerDayPantry > 0 && totalPantryGrams > 0) (totalPantryGrams / avgGramsPerDayPantry).toInt() else null
        DiaryConsumptionStats(
            momentPctMorning = pct(morn),
            momentPctAfternoon = pct(after),
            momentPctEvening = pct(even),
            avgCaffeine = avgCaffeine,
            avgDose = avgDose,
            mostFormat = mostFormat,
            pantryDaysLeft = pantryDaysLeft
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DiaryConsumptionStats(0, 0, 0, 0, 0, "—", null))

    /** Datos barista (cafés probados, tostadores, origen favorito): totalidad de la cuenta, no del periodo. Misma lógica que CafesProbadosViewModel (catálogo+custom, número = tamaño del listado). */
    val baristaStats: StateFlow<DiaryBaristaStats> = combine(allDiaryEntries, coffeesForBarista) { entries, coffees ->
        val coffeeEntries = entries.filter { isCupEntry(it) }
        val coffeeById = coffees.associate { it.coffee.id to it.coffee }
        val byCoffeeId = mutableMapOf<String, Long>()
        val roasterSet = mutableSetOf<String>()
        val originCount = mutableMapOf<String, Int>()
        coffeeEntries.forEach { e ->
            e.coffeeId?.let { id ->
                val ts = e.timestamp
                if (ts < (byCoffeeId[id] ?: Long.MAX_VALUE)) byCoffeeId[id] = ts
                coffeeById[id]?.let { c ->
                    c.marca.trim().takeIf { it.isNotEmpty() }?.let { roasterSet.add(it) }
                    (c.paisOrigen?.trim()?.takeIf { it.isNotEmpty() } ?: "—").let { orig ->
                        if (orig != "—") originCount[orig] = (originCount[orig] ?: 0) + 1
                    }
                }
            }
        }
        val favoriteOrigin = originCount.maxByOrNull { it.value }?.key ?: "—"
        val coffeesWithFirstTried = byCoffeeId.mapNotNull { (id, firstMs) ->
            coffeeById[id]?.let { TriedCoffeeItem(it, firstMs) }
        }.sortedBy { it.firstTriedMs }
        DiaryBaristaStats(
            distinctCoffees = coffeesWithFirstTried.size,
            distinctRoasters = roasterSet.size,
            favoriteOrigin = favoriteOrigin,
            coffeesWithFirstTried = coffeesWithFirstTried
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
        DiaryBaristaStats(0, 0, "—", emptyList()))

    fun setPeriod(period: DiaryPeriod) {
        _selectedPeriod.value = period
        when (period) {
            DiaryPeriod.HOY -> _selectedDiaryDateMs.value = calendarTodayStart
            DiaryPeriod.SEMANA -> _selectedDiaryDateMs.value = getMondayOfWeek(System.currentTimeMillis())
            DiaryPeriod.MES -> {
                val c = Calendar.getInstance()
                c.set(Calendar.DAY_OF_MONTH, 1)
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                _selectedDiaryDateMs.value = c.timeInMillis
            }
        }
    }

    /** Refresca la lista de cafés (tabla coffees) desde Supabase para pantallas que la usan (p. ej. AddStockScreen). Centralizado en SyncManager con reintento. */
    fun refreshCoffeesForStock() {
        viewModelScope.launch {
            try {
                syncManager.syncCoffeesIfNeeded(force = true)
            } catch (e: Exception) { /* best-effort */ }
        }
    }

    /** Fuerza recarga de entradas del diario desde remoto y actualiza flujos. */
    fun refreshDiaryEntries() {
        viewModelScope.launch {
            try {
                diaryRepository.syncDiaryEntriesFromRemote()
                diaryRepository.triggerRefresh()
            } catch (e: Exception) { /* best-effort */ }
            finally { _isLoading.value = false }
        }
    }
    
    fun refreshData(showLoader: Boolean = false) {
        if (showLoader) _isLoading.value = true
        viewModelScope.launch {
            try {
                diaryRepository.syncDiaryEntriesFromRemote()
                diaryRepository.syncPantryItems()
                diaryRepository.syncPendingDiaryEntries()
                diaryRepository.triggerRefresh()
            } catch (e: Exception) { /* best-effort */ }
            finally { if (showLoader) _isLoading.value = false }
            refreshDiaryWidget()
        }
    }

    suspend fun addCoffeeConsumption(
        coffeeId: String?, 
        coffeeName: String, 
        coffeeBrand: String,
        caffeineAmount: Int, 
        amountMl: Int, 
        coffeeGrams: Int,
        preparationType: String,
        sizeLabel: String? = null,
        pantryItemId: String? = null
    ) {
        diaryRepository.addDiaryEntry(
            coffeeId, coffeeName, coffeeBrand, caffeineAmount, "CUP", amountMl, coffeeGrams,
            preparationType, sizeLabel,
            reduceFromPantry = true,
            reduceFromPantryItemId = pantryItemId
        )
        refreshDiaryWidget()
    }
    
    suspend fun addWaterConsumption(amountMl: Int) {
            diaryRepository.addDiaryEntry(null, "Agua", "", 0, "WATER", amountMl, 0, "None", null)

        refreshDiaryWidget()
    }
    
    fun updateEntry(entry: DiaryEntryEntity) {
        viewModelScope.launch {
            runCatching { diaryRepository.updateDiaryEntry(entry) }.onSuccess { refreshDiaryWidget() }
        }
    }

    fun deleteEntry(entryId: Long) {
        viewModelScope.launch {
            diaryRepository.deleteDiaryEntry(entryId)
            diaryRepository.triggerRefresh()
            refreshDiaryWidget()
        }
    }
    
    fun addToPantry(coffeeId: String, grams: Int, onSuccess: () -> Unit = {}, onFailure: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                diaryRepository.addToPantry(coffeeId, grams)
                diaryRepository.triggerRefresh()
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error adding to pantry", e)
                onFailure?.invoke()
            }
        }
    }
    
    fun updateStock(pantryItemId: String, total: Int, remaining: Int) {
        viewModelScope.launch {
            diaryRepository.updatePantryStockById(pantryItemId, total, remaining)
            diaryRepository.triggerRefresh()
            refreshDiaryWidget()
        }
    }

    fun removeFromPantry(pantryItemId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                diaryRepository.deletePantryItemById(pantryItemId)
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error removing from pantry", e)
            }
        }
    }

    fun markCoffeeAsFinished(pantryItemId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                diaryRepository.markCoffeeAsFinished(pantryItemId)
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error marking coffee as finished", e)
            }
        }
    }
    
    fun saveCustomCoffee(
        name: String, brand: String, specialty: String, roast: String?, variety: String?,
        country: String, hasCaffeine: Boolean, format: String, totalGrams: Int,
        imageUri: Uri?, onSuccess: () -> Unit,
        descripcion: String? = null,
        proceso: String? = null,
        codigoBarras: String? = null,
        moliendaRecomendada: String? = null,
        productUrl: String? = null,
        aroma: Float = 0f, sabor: Float = 0f, cuerpo: Float = 0f, acidez: Float = 0f, dulzura: Float = 0f
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) { context.uriToWebPBytes(uri) }
                }
                diaryRepository.createCustomCoffeeAndAddToPantry(
                    name, brand, specialty, roast, variety, country, hasCaffeine, format, imageBytes, totalGrams,
                    descripcion, proceso, codigoBarras, moliendaRecomendada, productUrl, aroma, sabor, cuerpo, acidez, dulzura
                )
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al guardar café personalizado", e)
            }
        }
    }

    fun saveCustomCoffeeForDiary(
        name: String, brand: String, specialty: String, roast: String?, variety: String?,
        country: String, hasCaffeine: Boolean, format: String,
        imageUri: Uri?, totalGrams: Int = 250, onSuccess: (String) -> Unit,
        descripcion: String? = null,
        proceso: String? = null,
        codigoBarras: String? = null,
        moliendaRecomendada: String? = null,
        productUrl: String? = null,
        aroma: Float = 0f, sabor: Float = 0f, cuerpo: Float = 0f, acidez: Float = 0f, dulzura: Float = 0f
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) { context.uriToWebPBytes(uri) }
                }
                val coffeeId = diaryRepository.createCustomCoffeeAndAddToPantry(
                    name = name,
                    brand = brand,
                    specialty = specialty,
                    roast = roast,
                    variety = variety,
                    country = country,
                    hasCaffeine = hasCaffeine,
                    format = format,
                    imageBytes = imageBytes,
                    totalGrams = totalGrams,
                    descripcion = descripcion,
                    proceso = proceso,
                    codigoBarras = codigoBarras,
                    moliendaRecomendada = moliendaRecomendada,
                    productUrl = productUrl,
                    aroma = aroma,
                    sabor = sabor,
                    cuerpo = cuerpo,
                    acidez = acidez,
                    dulzura = dulzura
                )
                coffeeId?.let(onSuccess)
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al guardar café", e)
            }
        }
    }

    fun updateCustomCoffee(
        id: String, name: String, brand: String, specialty: String, roast: String?,
        variety: String?, country: String, hasCaffeine: Boolean, format: String,
        imageUri: Uri?, totalGrams: Int, onSuccess: () -> Unit,
        descripcion: String? = null,
        proceso: String? = null,
        codigoBarras: String? = null,
        moliendaRecomendada: String? = null,
        productUrl: String? = null,
        aroma: Float = 0f, sabor: Float = 0f, cuerpo: Float = 0f, acidez: Float = 0f, dulzura: Float = 0f
    ) {
        viewModelScope.launch {
            try {
                val imageBytes = imageUri?.let { uri ->
                    withContext(Dispatchers.IO) { context.uriToWebPBytes(uri) }
                }
                diaryRepository.updateCustomCoffee(
                    id, name, brand, specialty, roast, variety, country, hasCaffeine, format, imageBytes, totalGrams,
                    descripcion, proceso, codigoBarras, moliendaRecomendada, productUrl, aroma, sabor, cuerpo, acidez, dulzura
                )
                refreshDiaryWidget()
                onSuccess()
            } catch (e: Exception) {
                Log.e("DIARY_VIEWMODEL", "Error al actualizar café personalizado", e)
            }
        }
    }

    private fun refreshDiaryWidget() {
        // Widgets desactivados en esta rama.
    }
}
