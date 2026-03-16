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
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.cafesito.app.data.Coffee
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
import com.cafesito.app.ui.diary.DiaryBaristaStats
import com.cafesito.app.ui.diary.DiaryConsumptionStats
import com.cafesito.app.ui.diary.DiaryHabitStats
import com.cafesito.app.ui.diary.DiaryPeriod
import com.cafesito.app.ui.diary.TriedCoffeeItem
import com.cafesito.shared.domain.diary.DiaryAnalyticsTargets
import com.cafesito.shared.domain.diary.DiaryPeriod as SharedDiaryPeriod
import com.cafesito.app.ui.profile.ProfileUiState
import com.cafesito.app.ui.profile.ProfileViewModel
import com.cafesito.app.ui.theme.*
import androidx.compose.ui.unit.IntOffset
import kotlin.math.ceil
import kotlin.math.roundToInt
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
        modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space2)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth().padding(Spacing.space6)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CAFEÍNA ESTIMADA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        IconButton(onClick = { showInfo = true }, modifier = Modifier.size(Spacing.space6)) {
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

            Spacer(Modifier.height(6.dp))
            ChartPremiumSection(analytics)
            Spacer(Modifier.height(Spacing.space4))
        }
    }
}

@Composable
fun ComparisonPill(percentage: Int, baseColor: Color? = null) {
    val isPositive = percentage > 0
    val color = baseColor ?: LocalCaramelAccent.current
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = Shapes.cardSmall
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.space2, vertical = 2.dp)
        ) {
            Icon(
                if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(Spacing.space1))
            Text("${if (isPositive) "+" else ""}$percentage%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MetricBoxPremium(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Shapes.shapeCardMedium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Spacing.space3),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(Spacing.space1))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
    }
}

@Composable
fun DiaryHabitCard(stats: DiaryHabitStats) {
    PremiumCard(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space1)) {
        Column(Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space4)) {
            DiaryStatsRow("Tazas", stats.avgCups, showTopDivider = false)
            DiaryStatsRow("Tamaño tazas", stats.mostSize)
            DiaryStatsRow("Método", stats.mostMethod)
            DiaryStatsRow("Día cafetero", stats.busiestDay)
        }
    }
}

@Composable
fun DiaryConsumptionCard(stats: DiaryConsumptionStats) {
    PremiumCard(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space1)) {
        Column(Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space4)) {
            DiaryStatsRow("Momento", "Mañana ${stats.momentPctMorning}% · Tarde ${stats.momentPctAfternoon}% · Noche ${stats.momentPctEvening}%", showTopDivider = false, valueBelowTitle = true)
            DiaryStatsRow("Cafeína", "${stats.avgCaffeine} mg")
            DiaryStatsRow("Dosis por café", "${stats.avgDose} g")
            DiaryStatsRow("Formato", stats.mostFormat)
            DiaryStatsRow("Previsión despensa", stats.pantryDaysLeft?.let { "~$it días" } ?: "—")
        }
    }
}

@Composable
private fun DiaryStatsRow(label: String, value: String, showTopDivider: Boolean = true, valueBelowTitle: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (showTopDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.background,
                thickness = 1.dp,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (valueBelowTitle) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.space3)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.space1))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.space3),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/** Mapa nombre de país (normalizado) a código ISO 3166-1 alpha-2 para bandera emoji. */
private val countryNameToIso = mapOf(
    "colombia" to "CO", "brasil" to "BR", "brazil" to "BR", "etiopía" to "ET", "ethiopia" to "ET",
    "guatemala" to "GT", "honduras" to "HN", "costa rica" to "CR", "perú" to "PE", "peru" to "PE",
    "kenia" to "KE", "kenya" to "KE", "indonesia" to "ID", "méxico" to "MX", "mexico" to "MX",
    "nicaragua" to "NI", "el salvador" to "SV", "india" to "IN", "vietnam" to "VN",
    "papúa nueva guinea" to "PG", "papua nueva guinea" to "PG", "uganda" to "UG",
    "tanzania" to "TZ", "ruanda" to "RW", "rwanda" to "RW", "ecuador" to "EC",
    "bolivia" to "BO", "venezuela" to "VE", "jamaica" to "JM", "república dominicana" to "DO",
    "republica dominicana" to "DO", "haití" to "HT", "haiti" to "HT", "yemen" to "YE",
    "china" to "CN", "panamá" to "PA", "panama" to "PA", "cuba" to "CU", "filipinas" to "PH",
    "tailandia" to "TH", "timor oriental" to "TL", "laos" to "LA", "myanmar" to "MM",
    "burundi" to "BI", "camerún" to "CM", "camerun" to "CM", "madagascar" to "MG",
    "españa" to "ES", "spain" to "ES", "italia" to "IT", "italy" to "IT", "francia" to "FR",
    "alemania" to "DE", "germany" to "DE", "estados unidos" to "US", "usa" to "US"
)

/** Devuelve código ISO del país para mostrar bandera como imagen, o null si no hay mapa. */
private fun favoriteOriginIso(countryName: String): String? {
    if (countryName == "—" || countryName.isBlank()) return null
    return countryNameToIso[countryName.trim().lowercase()]
}

private val FLAG_CDN_BASE = "https://flagcdn.com/w40/"

@Composable
private fun originFavoriteRow(countryName: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.background,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.space3),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Origen favorito",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val iso = favoriteOriginIso(countryName)
            if (iso != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    AsyncImage(
                        model = FLAG_CDN_BASE + iso.lowercase() + ".png",
                        contentDescription = null,
                        modifier = Modifier.size(24.dp, 18.dp),
                        contentScale = ContentScale.FillBounds
                    )
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = countryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun DiaryBaristaCard(
    stats: DiaryBaristaStats,
    onCafesProbadosClick: () -> Unit
) {
    PremiumCard(modifier = Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space1)) {
        Column(Modifier.padding(horizontal = Spacing.space5, vertical = Spacing.space4)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCafesProbadosClick)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cafés probados",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stats.distinctCoffees}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(Spacing.space1))
                    Icon(Icons.Default.ChevronRight, contentDescription = "Ver más", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
            DiaryStatsRow("Tostadores probados", "${stats.distinctRoasters}")
            originFavoriteRow(stats.favoriteOrigin)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaristaCoffeesListSheet(
    coffees: List<TriedCoffeeItem>,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES")) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = Shapes.sheetLarge
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = 40.dp)) {
            Text(
                text = "Cafés probados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.space4)
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                items(coffees) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Shapes.cardSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(Spacing.space3),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.coffee.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = item.coffee.imageUrl,
                                contentDescription = item.coffee.nombre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(48.dp).clip(Shapes.cardSmall)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(Shapes.cardSmall)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Coffee, contentDescription = "Café", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.width(Spacing.space3))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.coffee.nombre.toCoffeeNameFormat(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Primera vez: ${dateFormat.format(Date(item.firstTriedMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
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
        shape = Shapes.pill
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
                    val isDark = isSystemInDarkTheme()
                    val optionsIconTint = if (isDark) PureWhite else MaterialTheme.colorScheme.onSurface
                    val optionsBgColor = if (isDark) PureBlack else PureWhite.copy(alpha = 0.82f)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(optionsBgColor, CircleShape)
                            .clickable { onOptionsClick(item.pantryItem.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Opciones", tint = optionsIconTint, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Column(Modifier.padding(Spacing.space3)) {
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
                Spacer(Modifier.height(Spacing.space2))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.space1)
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
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
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
        shape = Shapes.cardSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space3),
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
                modifier = Modifier.size(Spacing.space4)
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
    val quickActionIconColor = if (isSystemInDarkTheme()) PureWhite else PureBlack
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = Shapes.sheetLarge
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = 40.dp)) {
            Text(
text = "NUEVO REGISTRO",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.space4),
                textAlign = TextAlign.Center
            )
            ModalMenuOption(title = "Agua", iconPainter = painterResource(id = R.drawable.agua), color = quickActionIconColor, onClick = onAddWater)
            ModalMenuOption(title = "Café", icon = Icons.Default.Coffee, color = quickActionIconColor, onClick = onAddCoffee)
            ModalMenuOption(
                title = "Añadir a Despensa",
                iconPainter = painterResource(id = R.drawable.shelves_24),
                color = quickActionIconColor,
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
        shape = Shapes.sheetLarge
    ) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp).navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space6, vertical = Spacing.space4),
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
                    .padding(horizontal = Spacing.space6),
                verticalArrangement = Arrangement.spacedBy(Spacing.space5)
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
                        Spacer(Modifier.height(Spacing.space2))
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
                        Spacer(Modifier.height(Spacing.space1))
                        val cells = remember(startOffset, daysInMonth) {
                            List(startOffset) { null } + (1..daysInMonth).map { it }
                        }
                        val rows = remember(cells) { cells.chunked(7) }
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Spacing.space1)
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
                                                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, Shapes.cardSmall)
                                                        else Modifier
                                                    )
                                                    .clip(Shapes.cardSmall),
                                                shape = Shapes.cardSmall,
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
                                                                        .size(Spacing.space2)
                                                                        .clip(CircleShape)
                                                                        .border(1.dp, dotBorderColor, CircleShape)
                                                                        .background(coffeeDotColor, CircleShape)
                                                                )
                                                                if (hasWater) Spacer(Modifier.width(Spacing.space1))
                                                            }
                                                            if (hasWater) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(Spacing.space2)
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
            Spacer(Modifier.height(Spacing.space6))
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
        shape = Shapes.shapeCardMedium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(Spacing.space4), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(Spacing.space6))
            Spacer(Modifier.width(Spacing.space4))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = "Abrir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// Gráfico: ancho completo, curvas suaves (Catmull-Rom); alto ampliado y margen interno para no cortar líneas ni etiquetas
private const val CHART_GRACE_PCT = 1.08f
private val CHART_MIN_HEIGHT_DP = 35.dp   // altura total de la sección (gráfica + ejes)
private val CHART_CANVAS_HEIGHT_DP = 280.dp // altura del área de dibujo (más alta, con márgenes arriba/abajo)
private val CHART_PADDING_TOP_DP = Spacing.space5   // margen exterior superior
private val CHART_PADDING_BOTTOM_DP = 18.dp // margen exterior inferior
private val CHART_INTERNAL_TOP_DP = 28.dp  // espacio interno arriba para etiquetas (evita que se corten)
private val CHART_INTERNAL_BOTTOM_DP = 28.dp // espacio interno abajo para que la curva Catmull-Rom no se corte en 0
private val LABEL_OFFSET_ABOVE_PX = 42
private val CAFFEINE_BROWN = Color(0xFF6F4E37)
private val WATER_BLUE = Color(0xFF2196F3)

@Composable
fun ChartPremiumSection(analytics: DiaryAnalytics) {
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
        ceil(maxOf(chartMaxCaffeine, adjustedTarget) * CHART_GRACE_PCT).toInt()
    }
    val chartMaxWater = remember(analytics.chartData) {
        val raw = (analytics.chartData.maxOfOrNull { entry -> entry.water } ?: 0).coerceAtLeast(1)
        ceil(raw * CHART_GRACE_PCT).toInt()
    }

    val currentSlotIndex = remember(analytics.period) {
        val cal = Calendar.getInstance()
        when (analytics.period) {
            DiaryPeriod.HOY -> cal.get(Calendar.HOUR_OF_DAY)
            DiaryPeriod.SEMANA -> if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 6 else cal.get(Calendar.DAY_OF_WEEK) - 2
            DiaryPeriod.MES -> (cal.get(Calendar.DAY_OF_MONTH) - 1).coerceIn(0, 30)
        }
    }

    val isMonthPeriod = analytics.period == DiaryPeriod.MES
    val monthChartWidthDp = (analytics.chartData.size.coerceAtLeast(1) * 40).dp
    val monthScrollState = rememberScrollState()

    val chartConstraintsModifier = if (isMonthPeriod) Modifier.width(monthChartWidthDp) else Modifier.fillMaxWidth()
    val chartWrapperModifier = if (isMonthPeriod) Modifier.fillMaxWidth().horizontalScroll(monthScrollState) else Modifier.fillMaxWidth()

    Box(modifier = chartWrapperModifier) {
        BoxWithConstraints(modifier = chartConstraintsModifier) {
        val viewportPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val chartColWidthPx = viewportPx / analytics.chartData.size.coerceAtLeast(1)
        val chartHeightPx = with(density) { (CHART_CANVAS_HEIGHT_DP - CHART_PADDING_TOP_DP - CHART_PADDING_BOTTOM_DP).roundToPx() }.coerceAtLeast(1)
        val chartTopPaddingPx = with(density) { CHART_INTERNAL_TOP_DP.roundToPx().toFloat() }
        val chartInternalBottomPx = with(density) { CHART_INTERNAL_BOTTOM_DP.roundToPx().toFloat() }
        val chartDrawHeight = (chartHeightPx - chartTopPaddingPx - chartInternalBottomPx).coerceAtLeast(1f)
        val yBottomPx = chartTopPaddingPx + chartDrawHeight

        fun smoothSeries(values: List<Int>): List<Int> {
            if (values.isEmpty()) return values
            if (values.size <= 2) return values
            val out = mutableListOf(values[0])
            val k = 3
            for (i in 1 until values.size - 1) {
                val start = (i - k).coerceAtLeast(0)
                val end = (i + k).coerceAtMost(values.size - 1)
                val sum = (start..end).sumOf { values[it] }
                val count = end - start + 1
                out.add(sum / count)
            }
            out.add(values[values.size - 1])
            return out
        }
        fun buildSmoothPathCatmullRom(values: List<Int>, maxVal: Int): Path {
            val path = Path()
            if (values.isEmpty() || maxVal <= 0) return path
            val points = values.mapIndexed { i, v ->
                val x = i * chartColWidthPx + chartColWidthPx / 2f
                val y = if (v == 0) yBottomPx else {
                    val norm = (v.toFloat() / maxVal).coerceIn(0f, 1f)
                    chartTopPaddingPx + chartDrawHeight * (1f - norm)
                }
                Pair(x, y)
            }
            if (points.size < 2) return path
            val tension = 0.25f
            path.moveTo(points[0].first, points[0].second)
            for (i in 0 until points.size - 1) {
                val p0 = points[(i - 1).coerceAtLeast(0)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
                val c1x = p1.first + (p2.first - p0.first) * tension
                val c1y = p1.second + (p2.second - p0.second) * tension
                val c2x = p2.first - (p3.first - p1.first) * tension
                val c2y = p2.second - (p3.second - p1.second) * tension
                path.cubicTo(c1x, c1y, c2x, c2y, p2.first, p2.second)
            }
            return path
        }
        fun labelPoints(values: List<Int>, maxVal: Int): List<Pair<Float, Float>> {
            if (values.isEmpty() || maxVal <= 0) return emptyList()
            return values.mapIndexed { i, v ->
                val x = i * chartColWidthPx + chartColWidthPx / 2f
                val y = if (v == 0) yBottomPx else {
                    val norm = (v.toFloat() / maxVal).coerceIn(0f, 1f)
                    chartTopPaddingPx + chartDrawHeight * (1f - norm)
                }
                Pair(x, y)
            }
        }

        val waterPath = remember(analytics.chartData, chartMaxWater, chartColWidthPx) {
            buildSmoothPathCatmullRom(analytics.chartData.map { it.water }, chartMaxWater)
        }
        val caffeinePath = remember(analytics.chartData, chartMaxCaffeineRelative, chartColWidthPx) {
            buildSmoothPathCatmullRom(analytics.chartData.map { it.caffeine }, chartMaxCaffeineRelative)
        }
        val waterLabelPoints = remember(analytics.chartData, chartMaxWater, chartColWidthPx) {
            labelPoints(analytics.chartData.map { it.water }, chartMaxWater)
        }
        val caffeineLabelPoints = remember(analytics.chartData, chartMaxCaffeineRelative, chartColWidthPx) {
            labelPoints(analytics.chartData.map { it.caffeine }, chartMaxCaffeineRelative)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = CHART_CANVAS_HEIGHT_DP + CHART_PADDING_TOP_DP + CHART_PADDING_BOTTOM_DP + 32.dp)
        ) {
            val chartVisible = remember(analytics.chartData) { mutableStateOf(false) }
            val isFirstAnimation = remember { mutableStateOf(true) }
            LaunchedEffect(analytics.chartData) {
                chartVisible.value = false
                kotlinx.coroutines.delay(50)
                chartVisible.value = true
            }
            val chartProgress by animateFloatAsState(
                targetValue = if (chartVisible.value) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (isFirstAnimation.value) 1500 else 600,
                    easing = EaseInOutCubic
                ),
                label = "chartAlpha"
            )
            LaunchedEffect(chartProgress) {
                if (chartProgress >= 0.99f) isFirstAnimation.value = false
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CHART_CANVAS_HEIGHT_DP)
                    .padding(top = CHART_PADDING_TOP_DP, bottom = CHART_PADDING_BOTTOM_DP)
                    .drawWithContent {
                        clipRect(left = 0f, top = 0f, right = size.width * chartProgress, bottom = size.height) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 2.5.dp.toPx()
                    drawPath(
                        path = waterPath,
                        color = WATER_BLUE,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = caffeinePath,
                        color = CAFFEINE_BROWN,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                Box(Modifier.fillMaxSize()) {
                    analytics.chartData.forEachIndexed { index, entry ->
                        if (entry.water > 0 && index < waterLabelPoints.size) {
                            val (xPx, yPx) = waterLabelPoints[index]
                            Text(
                                text = entry.water.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = WATER_BLUE,
                                modifier = Modifier
                                    .offset { IntOffset(xPx.roundToInt() - 6, (yPx - LABEL_OFFSET_ABOVE_PX - 22).roundToInt()) }
                                    .align(Alignment.TopStart)
                            )
                        }
                        if (entry.caffeine > 0 && index < caffeineLabelPoints.size) {
                            val (xPx, yPx) = caffeineLabelPoints[index]
                            Text(
                                text = entry.caffeine.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CAFFEINE_BROWN,
                                modifier = Modifier
                                    .offset { IntOffset(xPx.roundToInt() - 6, (yPx - LABEL_OFFSET_ABOVE_PX - 38).roundToInt()) }
                                    .align(Alignment.TopStart)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space1))
            val isDarkMode = isSystemInDarkTheme()
            val axisLabelGray = if (isDarkMode) DateMetaAxisDark else DateMetaAxisLight
            val currentDayColor = if (isDarkMode) PureWhite else PureBlack
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                analytics.chartData.forEachIndexed { index, entry ->
                    val isCurrent = index == currentSlotIndex
                    val showHoy = isCurrent && (analytics.period != DiaryPeriod.SEMANA || analytics.isCurrentWeek)
                    Text(
                        text = if (showHoy) "${entry.label} - Hoy" else entry.label,
                        fontSize = 10.sp,
                        color = if (isCurrent) currentDayColor else axisLabelGray,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBottomSheet(
    selectedPeriod: DiaryPeriod,
    selectedDateMs: Long,
    canGoNextMonth: Boolean,
    onDismiss: () -> Unit,
    onPeriodSelected: (DiaryPeriod) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = 40.dp)) {
            Text(
                text = "SELECCIONAR PERIODO",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.space4),
                textAlign = TextAlign.Center
            )
            DiaryPeriod.values().forEach { period ->
                val isSelected = period == selectedPeriod
                val label = when (period) {
                    DiaryPeriod.HOY -> "HOY"
                    DiaryPeriod.SEMANA -> "SEMANA"
                    DiaryPeriod.MES -> "MES"
                }
                Surface(
                    onClick = { onPeriodSelected(period); onDismiss() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.space1),
                    shape = Shapes.card,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        label,
                        modifier = Modifier.padding(Spacing.space4),
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            if (selectedPeriod == DiaryPeriod.MES && selectedDateMs != 0L) {
                val monthLabel = com.cafesito.app.ui.diary.formatMonthYear(selectedDateMs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.space4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = onPrevMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mes anterior")
                    }
                    Text(
                        text = monthLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = Spacing.space4)
                    )
                    if (canGoNextMonth) {
                        IconButton(onClick = onNextMonth) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Mes siguiente")
                        }
                    } else {
                        Spacer(Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}
