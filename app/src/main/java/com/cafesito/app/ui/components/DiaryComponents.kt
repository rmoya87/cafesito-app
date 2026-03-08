package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import com.cafesito.app.R
import java.util.Calendar
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.CommentWithAuthor
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.PostWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.UserReviewInfo
import com.cafesito.app.ui.brewlab.BrewLabViewModel
import com.cafesito.app.ui.brewlab.BrewMethod
import com.cafesito.app.ui.brewlab.BrewPhaseInfo
import com.cafesito.app.ui.diary.DiaryAnalytics
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.shared.domain.diary.DiaryAnalyticsTargets
import com.cafesito.shared.domain.diary.DiaryPeriod as SharedDiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.theme.*
import com.cafesito.app.ui.timeline.CommentsViewModel
import com.cafesito.app.ui.timeline.TimelineNotification
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

// --- DIARY COMPONENTS ---

@Composable
fun CaffeinePremiumCard(analytics: DiaryAnalytics) {
    var showInfo by remember { mutableStateOf(false) }
    if (showInfo) InfoBottomSheet { showInfo = false }

    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CAFEÍNA ESTIMADA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Info, contentDescription = "Información", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "${analytics.totalCaffeine} mg",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(analytics.caffeineTrendPct)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("HIDRATACIÓN", style = MaterialTheme.typography.labelLarge, color = WaterBlue, fontSize = 10.sp)
                    Text(
                        text = "${analytics.totalWaterMl} ml",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(analytics.hydrationTrendPct, WaterBlue)
                }
            }

            Spacer(Modifier.height(24.dp))
            ChartPremiumSection(analytics)
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBoxPremium("Media", "${analytics.avgCaffeineLast30} mg", Icons.Default.Insights, Modifier.weight(1f))
                MetricBoxPremium("Tazas", "${analytics.cupsCount}", Icons.Filled.Coffee, Modifier.weight(1f))
                MetricBoxPremium("Progreso", "${analytics.hydrationProgressPct}%", Icons.Default.WaterDrop, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ComparisonPill(percentage: Int, baseColor: Color? = null) {
    val isPositive = percentage > 0
    val color = baseColor ?: LocalCaramelAccent.current
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(
                if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("${if (isPositive) "+" else ""}$percentage%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricBoxPremium(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
    }
}

@Composable
fun PantryPremiumCard(
    item: PantryItemWithDetails,
    onClick: (String) -> Unit,
    onOptionsClick: ((String) -> Unit)? = null
) {
    val totalGrams = item.pantryItem.totalGrams.coerceAtLeast(0)
    val remainingGrams = item.pantryItem.gramsRemaining.coerceIn(0, totalGrams)
    val progress = if (totalGrams > 0) remainingGrams.toFloat() / totalGrams.toFloat() else 0f

    PremiumCard(
        modifier = Modifier.clickable { onClick(item.coffee.id) },
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            Box {
                AsyncImage(
                    model = item.coffee.imageUrl,
                    contentDescription = item.coffee.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
                if (onOptionsClick != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.82f), CircleShape)
                            .clickable { onOptionsClick(item.coffee.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(Modifier.padding(12.dp)) {
                Text(
                    text = item.coffee.nombre.toCoffeeNameFormat(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                @Suppress("DEPRECATION")
                Text(
                    text = "${remainingGrams}/${totalGrams}g",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = LocalCaramelAccent.current,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(Modifier
        .fillMaxWidth()
        .padding(48.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Estado de error de red/carga: mensaje + botón Reintentar. Patrón unificado (ver docs/UX_EMPTY_AND_ERROR_STATES.md). */
@Composable
fun ErrorStateMessage(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        OutlinedButton(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

@Composable
fun PeriodSelectorPremium(period: DiaryPeriod, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                period.name,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
            Icon(
                Icons.Default.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryBottomSheet(
    onDismiss: () -> Unit,
    onAddWater: () -> Unit,
    onAddCoffee: () -> Unit,
    onAddPantry: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text(
                text = "NUEVO REGISTRO", 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary, 
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            ModalMenuOption(title = "Agua", icon = Icons.Default.WaterDrop, color = WaterBlue, onClick = onAddWater)
            ModalMenuOption(title = "Café", icon = Icons.Default.Coffee, color = MaterialTheme.colorScheme.onSurface, onClick = onAddCoffee)
            ModalMenuOption(
                title = "Añadir a Despensa",
                iconPainter = painterResource(id = R.drawable.shelves_24),
                color = MaterialTheme.colorScheme.onSurface,
                onClick = onAddPantry
            )
        }
    }
}

private val DIARY_CALENDAR_MONTH_NAMES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryDatePickerSheet(
    onDismiss: () -> Unit,
    onGoToToday: () -> Unit,
    onPickDate: (year: Int, month: Int, dayOfMonth: Int) -> Unit,
    selectedDateMs: Long,
    entries: List<DiaryEntryEntity>
) {
    val todayStartMs = remember {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 0)
        c.set(java.util.Calendar.MINUTE, 0)
        c.set(java.util.Calendar.SECOND, 0)
        c.set(java.util.Calendar.MILLISECOND, 0)
        c.timeInMillis
    }

    val (datesWithCoffee, datesWithWater) = remember(entries) {
        val coffee = mutableSetOf<String>()
        val water = mutableSetOf<String>()
        val cal = java.util.Calendar.getInstance()
        entries.forEach { entry ->
            cal.timeInMillis = entry.timestamp
            val dateStr = "%04d-%02d-%02d".format(
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            )
            if (entry.type.equals("WATER", ignoreCase = true)) water.add(dateStr)
            else coffee.add(dateStr)
        }
        Pair(coffee, water)
    }

    val monthsToShow = remember {
        val now = java.util.Calendar.getInstance()
        List(24) { i ->
            val c = java.util.Calendar.getInstance()
            c.set(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), 1)
            c.add(java.util.Calendar.MONTH, -i)
            Pair(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.MONTH))
        }
    }

    val coffeeDotColor = LocalCaramelAccent.current
    val waterDotColor = WaterBlue
    val dotBorderColor = MaterialTheme.colorScheme.surfaceContainer

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Selecciona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Button(
                        onClick = onGoToToday,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Ir a hoy")
                    }
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(monthsToShow.size) { idx ->
                    val (year, month) = monthsToShow[idx]
                    val firstDay = java.util.Calendar.getInstance().apply {
                        set(year, month, 1)
                    }
                    val lastDay = java.util.Calendar.getInstance().apply {
                        set(year, month, firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
                    }
                    val daysInMonth = firstDay.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                    val startWeekday = firstDay.get(java.util.Calendar.DAY_OF_WEEK)
                    val startOffset = (startWeekday - 2 + 7) % 7

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${DIARY_CALENDAR_MONTH_NAMES[month]} $year",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("L", "M", "X", "J", "V", "S", "D").forEach { label ->
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val cells = remember(startOffset, daysInMonth) {
                            List(startOffset) { null } + (1..daysInMonth).map { it }
                        }
                        val rows = remember(cells) { cells.chunked(7) }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rows.forEach { rowCells ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    rowCells.forEach { dayOrNull ->
                                        if (dayOrNull == null) {
                                            Spacer(Modifier.size(36.dp))
                                        } else {
                                            val day = dayOrNull
                                            val dateStr = "%04d-%02d-%02d".format(year, month + 1, day)
                                            val hasCoffee = datesWithCoffee.contains(dateStr)
                                            val hasWater = datesWithWater.contains(dateStr)
                                            val dayStartMs = remember(year, month, day) {
                                                val c = java.util.Calendar.getInstance()
                                                c.set(year, month, day)
                                                c.set(java.util.Calendar.HOUR_OF_DAY, 0)
                                                c.set(java.util.Calendar.MINUTE, 0)
                                                c.set(java.util.Calendar.SECOND, 0)
                                                c.set(java.util.Calendar.MILLISECOND, 0)
                                                c.timeInMillis
                                            }
                                            val isSelected = dayStartMs == selectedDateMs
                                            val isToday = dayStartMs == todayStartMs
                                            Surface(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .then(
                                                        if (isToday && !isSelected)
                                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                        else Modifier
                                                    )
                                                    .clip(RoundedCornerShape(12.dp)),
                                                shape = RoundedCornerShape(12.dp),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    else -> Color.Transparent
                                                },
                                                onClick = {
                                                    onPickDate(year, month, day)
                                                    onDismiss()
                                                }
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = day.toString(),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = when {
                                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                                            isToday -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                    if (hasCoffee || hasWater) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.Center,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(top = 2.dp)
                                                        ) {
                                                            if (hasCoffee) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(8.dp)
                                                                        .clip(CircleShape)
                                                                        .border(1.dp, dotBorderColor, CircleShape)
                                                                        .background(coffeeDotColor, CircleShape)
                                                                )
                                                                if (hasWater) Spacer(Modifier.width(4.dp))
                                                            }
                                                            if (hasWater) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(8.dp)
                                                                        .clip(CircleShape)
                                                                        .border(1.dp, dotBorderColor, CircleShape)
                                                                        .background(waterDotColor, CircleShape)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun EntryOption(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChartPremiumSection(analytics: DiaryAnalytics) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val sharedPeriod = remember(analytics.period) {
        when (analytics.period) {
            DiaryPeriod.HOY -> SharedDiaryPeriod.HOY
            DiaryPeriod.SEMANA -> SharedDiaryPeriod.SEMANA
            DiaryPeriod.MES -> SharedDiaryPeriod.MES
        }
    }
    val caffeineTarget = remember(sharedPeriod) { DiaryAnalyticsTargets.caffeineTargetMg(sharedPeriod) }
    val caffeineTargetPerSlot = remember(caffeineTarget, analytics.period, analytics.chartData.size) {
        when (analytics.period) {
            DiaryPeriod.HOY -> caffeineTarget.toFloat()
            DiaryPeriod.SEMANA -> caffeineTarget / 7f
            DiaryPeriod.MES -> caffeineTarget / analytics.chartData.size.coerceAtLeast(1).toFloat()
        }
    }
    val chartMaxCaffeine = remember(analytics.chartData) {
        (analytics.chartData.maxOfOrNull { entry -> entry.caffeine } ?: 0).coerceAtLeast(1)
    }
    val chartMaxCaffeineRelative = remember(chartMaxCaffeine, caffeineTargetPerSlot) {
        val adjustedTarget = (caffeineTargetPerSlot * 0.6f).toInt().coerceAtLeast(1)
        maxOf(chartMaxCaffeine, adjustedTarget)
    }
    val chartMaxWater = remember(analytics.chartData) {
        (analytics.chartData.maxOfOrNull { entry -> entry.water } ?: 0).coerceAtLeast(1)
    }
    val caffeineBrown = LocalCaramelAccent.current
    val waterElectricBlue = WaterBlue

    val currentSlotIndex = remember(analytics.period) {
        val cal = Calendar.getInstance()
        when (analytics.period) {
            DiaryPeriod.HOY -> cal.get(Calendar.HOUR_OF_DAY)
            DiaryPeriod.SEMANA -> if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else cal.get(Calendar.DAY_OF_WEEK) - 2
            DiaryPeriod.MES -> (cal.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, 30)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportPx = with(density) { maxWidth.roundToPx() }
        val columnPx = with(density) { (56.dp + 12.dp).roundToPx() }
        LaunchedEffect(analytics.chartData, viewportPx, scrollState.maxValue) {
            if (scrollState.maxValue <= 0 || viewportPx <= 0 || analytics.chartData.isEmpty()) return@LaunchedEffect
            val idx = currentSlotIndex.coerceIn(0, analytics.chartData.size - 1)
            val targetScroll = (idx * columnPx) - (viewportPx / 2) + (columnPx / 2)
            scrollState.animateScrollTo(targetScroll.coerceIn(0, scrollState.maxValue))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            analytics.chartData.forEachIndexed { index, entry ->
            val isCurrent = index == currentSlotIndex
            val caffeineHeight = (entry.caffeine.toFloat() / chartMaxCaffeineRelative.toFloat()).coerceIn(0.05f, 1f)
            val waterHeight = (entry.water.toFloat() / chartMaxWater.toFloat()).coerceIn(0.05f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(56.dp)
                    .then(if (isCurrent) Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)) else Modifier)
            ) {
                Row(
                    modifier = Modifier.height(100.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(26.dp), verticalArrangement = Arrangement.Bottom) {
                        if (entry.caffeine > 0) {
                            Text(
                                text = "${entry.caffeine}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = caffeineBrown,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(caffeineHeight)
                            .clip(CircleShape)
                            .background(caffeineBrown.copy(alpha = if (entry.caffeine > 0) 1f else 0.35f)))
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(26.dp), verticalArrangement = Arrangement.Bottom) {
                        if (entry.water > 0) {
                            Text(
                                text = "${entry.water}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = waterElectricBlue,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth(1f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Box(Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(waterHeight)
                            .clip(CircleShape)
                            .background(waterElectricBlue.copy(alpha = if (entry.water > 0) 1f else 0.35f)))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (isCurrent) "${entry.label} · Hoy" else entry.label,
                    fontSize = 9.sp,
                    color = if (isCurrent) caffeineBrown else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBottomSheet(selectedPeriod: DiaryPeriod, onDismiss: () -> Unit, onPeriodSelected: (DiaryPeriod) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text(
                text = "SELECCIONAR PERIODO", 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            DiaryPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                Surface(
                    onClick = { onPeriodSelected(period); onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        period.name,
                        modifier = Modifier.padding(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
