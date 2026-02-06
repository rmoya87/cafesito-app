package com.cafesito.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
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
import coil.compose.AsyncImage
import com.cafesito.app.R
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

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp),
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(content = content)
    }
}

@Composable
fun Modifier.premiumBorder(shape: RoundedCornerShape = RoundedCornerShape(32.dp)) = 
    this.border(1.dp, MaterialTheme.colorScheme.outline, shape)

@Composable
fun borderLight() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

@Composable
fun ModernAvatar(
    imageUrl: String?,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(size - 8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun AnimatedTabIndicator(
    selectedTabIndex: Int,
    tabsCount: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val tabWidth = if (tabsCount > 0) maxWidth / tabsCount else maxWidth
        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTabIndex,
            animationSpec = spring(stiffness = 800f),
            label = "tabOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassyTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBackIos, 
                        contentDescription = "Back", 
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
            scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        )
    )
}

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    )
}

@Composable
fun ProfileHeaderShimmer() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerItem(modifier = Modifier
            .size(110.dp)
            .clip(CircleShape))
        Spacer(Modifier.height(16.dp))
        ShimmerItem(modifier = Modifier
            .height(30.dp)
            .width(200.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerItem(modifier = Modifier
            .height(20.dp)
            .width(120.dp))
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ShimmerItem(modifier = Modifier
                .height(50.dp)
                .width(80.dp))
            ShimmerItem(modifier = Modifier
                .height(50.dp)
                .width(80.dp))
            ShimmerItem(modifier = Modifier
                .height(50.dp)
                .width(80.dp))
        }
    }
}

@Composable
fun PostCardShimmer() {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(horizontal = 16.dp)) {
                ShimmerItem(modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.7f))
                Spacer(Modifier.height(8.dp))
                ShimmerItem(modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.9f))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun CaffeinePremiumCardShimmer() {
    PremiumCard(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerItem(modifier = Modifier
                        .height(10.dp)
                        .width(120.dp))
                    ShimmerItem(modifier = Modifier
                        .height(30.dp)
                        .width(100.dp))
                    ShimmerItem(modifier = Modifier
                        .height(20.dp)
                        .width(50.dp))
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                     ShimmerItem(modifier = Modifier
                         .height(10.dp)
                         .width(80.dp))
                     ShimmerItem(modifier = Modifier
                         .height(24.dp)
                         .width(90.dp))
                     ShimmerItem(modifier = Modifier
                         .height(20.dp)
                         .width(50.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
            ShimmerItem(modifier = Modifier
                .fillMaxWidth()
                .height(120.dp))
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerItem(modifier = Modifier
                    .weight(1f)
                    .height(60.dp))
                ShimmerItem(modifier = Modifier
                    .weight(1f)
                    .height(60.dp))
                ShimmerItem(modifier = Modifier
                    .weight(1f)
                    .height(60.dp))
            }
        }
    }
}

@Composable
fun DiaryItemShimmer() {
    Surface(
        modifier = Modifier.fillMaxWidth(), 
        color = MaterialTheme.colorScheme.surface, 
        shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerItem(modifier = Modifier
                .size(40.dp)
                .clip(CircleShape))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                ShimmerItem(modifier = Modifier
                    .height(20.dp)
                    .fillMaxWidth(0.7f))
                Spacer(Modifier.height(4.dp))
                ShimmerItem(modifier = Modifier
                    .height(16.dp)
                    .fillMaxWidth(0.9f))
            }
        }
    }
}

@Composable
fun PantryItemShimmer() {
    PremiumCard(shape = RoundedCornerShape(28.dp)) {
        Column {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
            Column(Modifier.padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ShimmerItem(modifier = Modifier
                        .height(16.dp)
                        .width(50.dp))
                    ShimmerItem(modifier = Modifier
                        .height(16.dp)
                        .width(40.dp))
                }
                Spacer(Modifier.height(6.dp))
                ShimmerItem(modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape))
            }
        }
    }
}

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
                            Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "${analytics.totalCaffeine} mg",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(analytics.comparisonPercentage)
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("HIDRATACIÓN", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2196F3), fontSize = 10.sp)
                    Text(
                        text = "${analytics.totalWaterMl} ml",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    ComparisonPill(-2, Color(0xFF2196F3))
                }
            }

            Spacer(Modifier.height(24.dp))
            ChartPremiumSection(analytics)
            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBoxPremium("Media", "${analytics.averageCaffeine} mg", Icons.Default.AutoGraph, Modifier.weight(1f))
                MetricBoxPremium("Tazas", "${analytics.cupsCount}", Icons.Default.Coffee, Modifier.weight(1f))
                MetricBoxPremium("Progreso", "85%", Icons.Default.WaterDrop, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ComparisonPill(percentage: Int, baseColor: Color? = null) {
    val isPositive = percentage > 0
    val color = baseColor ?: if (isPositive) ErrorRed else SuccessGreen
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
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
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
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f

    PremiumCard(
        modifier = Modifier.clickable { onClick(item.coffee.id) },
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            if (onOptionsClick != null) {
                IconButton(
                    onClick = { onOptionsClick(item.coffee.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }

            Column(Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)) {
                Text(item.coffee.marca.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(item.coffee.nombre, color = Color.White, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${item.pantryItem.gramsRemaining}g", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("${(progress * 100).roundToInt()}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline
            )
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
fun AddEntryBottomSheet(onDismiss: () -> Unit, onAddWater: () -> Unit, onAddCoffee: () -> Unit, onAddPantry: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("NUEVO REGISTRO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            ModalMenuOption(title = "Registro de Agua", icon = Icons.Default.WaterDrop, color = Color(0xFF2196F3), onClick = onAddWater)
            ModalMenuOption(title = "Registro de Café", icon = Icons.Default.Coffee, color = MaterialTheme.colorScheme.onSurface, onClick = onAddCoffee)
            ModalMenuOption(title = "Añadir a Despensa", icon = Icons.Default.Inventory, color = MaterialTheme.colorScheme.primary, onClick = onAddPantry)
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
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ChartPremiumSection(analytics: DiaryAnalytics) {
    val scrollState = rememberScrollState()

    LaunchedEffect(analytics.chartData) { scrollState.animateScrollTo(scrollState.maxValue) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        analytics.chartData.forEach { entry ->
            val caffeineHeight = (entry.caffeine.toFloat() / 400f).coerceIn(0.05f, 1f)
            val waterHeight = (entry.water.toFloat() / 2000f).coerceIn(0.05f, 1f)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
                Row(modifier = Modifier.height(80.dp), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier
                        .width(10.dp)
                        .fillMaxHeight(caffeineHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary))
                    Spacer(Modifier.width(4.dp))
                    Box(Modifier
                        .width(10.dp)
                        .fillMaxHeight(waterHeight)
                        .clip(CircleShape)
                        .background(Color(0xFF2196F3).copy(alpha = 0.4f)))
                }
                Spacer(Modifier.height(8.dp))
                Text(entry.label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodBottomSheet(selectedPeriod: DiaryPeriod, onDismiss: () -> Unit, onPeriodSelected: (DiaryPeriod) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text("SELECCIONAR PERIODO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
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

// --- PROFILE COMPONENTS ---

@Composable
fun ProfilePosts(
    posts: List<PostWithDetails>,
    isCurrentUser: Boolean,
    activeUser: UserEntity?,
    viewModel: ProfileViewModel,
    onUserClick: (Int) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onCommentClick: (String) -> Unit,
    onEditClick: (PostWithDetails) -> Unit,
    onDeleteClick: (PostWithDetails) -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(posts) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                PostCard(
                    details = item,
                    onUserClick = { onUserClick(item.author.id) },
                    onCommentClick = { onCommentClick(item.post.id) },
                    onLikeClick = { viewModel.onToggleLike(item.post.id) },
                    isLiked = activeUser?.let { me -> item.likes.any { it.userId == me.id } } ?: false,
                    showHeader = false,
                    isOwnPost = isCurrentUser,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) }
                )
            }
        }
    }
}

@Composable
fun ProfileAdn(
    state: ProfileUiState.Success,
    listState: LazyListState = rememberLazyListState(),
    onShowSensoryDetail: () -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        item {
            PremiumCard(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable(onClick = onShowSensoryDetail)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    SensoryRadarChart(
                        data = state.sensoryProfile,
                        lineColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Pulsa para descubrir tus preferencias",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text(
                text = "Tu ADN se elabora analizando tus cafés favoritos y tus reseñas.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ProfileFavorites(
    favoriteCoffees: List<CoffeeWithDetails>,
    viewModel: ProfileViewModel,
    onCoffeeClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState()
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(favoriteCoffees) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                CoffeeFavoritePremiumItem(
                    coffeeDetails = item,
                    onFavoriteClick = { viewModel.onToggleFavorite(item.coffee.id, false) },
                    onClick = { onCoffeeClick(item.coffee.id) }
                )
            }
        }
    }
}

@Composable
fun ProfileReviews(
    userReviews: List<UserReviewInfo>,
    isCurrentUser: Boolean,
    onCoffeeClick: (String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    onEditClick: (UserReviewInfo) -> Unit,
    onDeleteClick: (UserReviewInfo) -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp)) {
        items(userReviews) { item ->
            Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                UserReviewCard(
                    info = item,
                    showHeader = false,
                    isOwnReview = isCurrentUser,
                    onEditClick = { onEditClick(item) },
                    onDeleteClick = { onDeleteClick(item) },
                    onClick = { onCoffeeClick(item.coffeeDetails.coffee.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensoryDetailBottomSheet(profile: Map<String, Float>, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier
            .padding(24.dp)
            .padding(bottom = 48.dp)) {
            Text(
                text = "ANÁLISIS DE PREFERENCIAS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            val sorted = profile.toList().sortedByDescending { it.second }
            val highest = sorted.getOrNull(0)
            val second = sorted.getOrNull(1)

            if (highest != null) {
                val primaryName = highest.first.lowercase()
                val secondaryName = second?.first?.lowercase()

                Text(
                    text = if (secondaryName != null && second.second > 3f) {
                        "Tu paladar es una combinación experta de $primaryName y $secondaryName."
                    } else {
                        "Tu paladar destaca principalmente por preferir notas de $primaryName."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                val description = when (highest.first) {
                    "Acidez" -> "Buscas brillo en cada taza. Te apasionan los perfiles cítricos y vibrantes de cafés de altura (1500m+)."
                    "Dulzura" -> "Eres amante de lo meloso. Prefieres cafés con procesos naturales que resaltan notas de chocolate, caramelo y frutas maduras."
                    "Cuerpo" -> "Para ti, la textura lo es todo. Disfrutas de esa sensación aterciopelada y densa, típica de tuestes medios y perfiles clásicos."
                    "Aroma" -> "Tu ritual empieza con el olfato. Te atraen las fragancias complejas, florales y especiadas que inundan la habitación."
                    "Sabor" -> "Buscas la máxima intensidad y complejidad gustativa. Valoras la persistencia de las notas tras cada sorbo."
                    else -> "Disfrutas de una complejidad excepcional y un balance perfectamente equilibrado en tu ADN cafetero."
                }
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 22.sp)

                val (idealType, idealOrigin) = when (highest.first) {
                    "Acidez" -> "Lavados de alta montaña" to "Etiopía o Colombia (Nariño)"
                    "Dulzura" -> "Procesos Natural o Honey" to "Brasil o El Salvador"
                    "Cuerpo" -> "Tuestes medios / Naturales" to "Sumatra o Guatemala"
                    "Aroma" -> "Variedades florales / Geishas" to "Panamá o Ruanda"
                    "Sabor" -> "Micro-lotes de especialidad" to "Costa Rica o Kenia"
                    else -> "Blends equilibrados" to "Cualquier origen de especialidad"
                }

                Spacer(Modifier.height(24.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("RECOMENDACIÓN IDEAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(text = "Deberías probar: $idealType", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = "Orígenes sugeridos: $idealOrigin", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) { Text("CONTINUAR EXPLORANDO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

@Composable
fun ProfileStatsRow(
    posts: Int,
    followers: Int,
    following: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Posts", posts.toString())
        StatItem("Seguidores", followers.toString(), onFollowersClick)
        StatItem("Siguiendo", following.toString(), onFollowingClick)
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .let {
                if (onClick != null) it.clickable(onClick = onClick) else it
            }
            .padding(8.dp)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, letterSpacing = 1.sp)
    }
}

@Composable
fun CoffeeFavoritePremiumItem(
    coffeeDetails: CoffeeWithDetails,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    PremiumCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffeeDetails.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = coffeeDetails.coffee.marca, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = ElectricRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Fila de café en listado de favoritos (sin corazón). Para quitar de favoritos usar swipe. */
@Composable
fun CoffeeFavoriteListItem(
    coffeeDetails: CoffeeWithDetails,
    onClick: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffeeDetails.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = coffeeDetails.coffee.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = coffeeDetails.coffee.marca, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFavoriteItem(
    coffeeDetails: CoffeeWithDetails,
    onRemoveFromFavorites: () -> Unit,
    onClick: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onRemoveFromFavorites()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Quitar de favoritos",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        }
    ) {
        CoffeeFavoriteListItem(coffeeDetails = coffeeDetails, onClick = onClick)
    }
}

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(24.dp),
        border = if (isFollowing) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Text(if (isFollowing) "SIGUIENDO" else "SEGUIR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun EditProfileFields(
    username: String,
    fullName: String,
    bio: String,
    onUsernameChange: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onSave: () -> Unit,
    usernameError: String?
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)) {
        OutlinedTextField(
            value = username, onValueChange = onUsernameChange, label = { Text("Usuario") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            isError = usernameError != null, supportingText = { if (usernameError != null) Text(usernameError) },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = fullName, onValueChange = onFullNameChange, label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = bio, onValueChange = onBioChange, label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onSave, modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CaramelAccent), shape = RoundedCornerShape(28.dp)
        ) { Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold, color = Color.White) }
    }
}

// --- BREWLAB COMPONENTS ---

@Composable
fun ChooseMethodStep(methods: List<BrewMethod>, onSelect: (BrewMethod) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val chunkedMethods = methods.chunked(2)
        items(chunkedMethods) { rowMethods ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowMethods.forEach { method ->
                    MethodCard(method, Modifier.weight(1f)) { onSelect(method) }
                }
                if (rowMethods.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MethodCard(method: BrewMethod, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val context = LocalContext.current
    val resId = remember(method.iconResName) {
        context.resources.getIdentifier(method.iconResName, "drawable", context.packageName)
    }

    PremiumCard(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = method.name,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(Icons.Default.CoffeeMaker, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = method.name.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChooseCoffeeStep(items: List<PantryItemWithDetails>, onSelect: (PantryItemWithDetails) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (items.isEmpty()) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes café en tu despensa", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(items, key = { it.coffee.id }) { item ->
                PantrySelectionCard(item) { onSelect(item) }
            }
        }
    }
}

@Composable
fun ConfigStep(
    method: BrewMethod?,
    water: Float,
    ratio: Float,
    coffeeGrams: Float,
    valuation: String,
    viewModel: BrewLabViewModel,
    onNext: () -> Unit
) {
    val waterBlue = Color(0xFF2196F3)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))
            SectionHeader("AJUSTES TÉCNICOS")

            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        if (method?.hasWaterAdjustment == true) {
                            DataBlock("AGUA", "${water.roundToInt()} ml", waterBlue)
                        }
                        DataBlock("CAFÉ", "${String.format("%.1f", coffeeGrams)} g", MaterialTheme.colorScheme.primary)
                    }

                    if (method?.hasWaterAdjustment == true) {
                        Spacer(Modifier.height(32.dp))
                        Text("CANTIDAD DE AGUA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = water, onValueChange = { viewModel.setWaterAmount(it) }, valueRange = 50f..1000f,
                            colors = SliderDefaults.colors(thumbColor = waterBlue, activeTrackColor = waterBlue, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                        )
                    }

                    Spacer(Modifier.height(24.dp))
                    val label = if (method?.hasWaterAdjustment == true) "RATIO (INTENSIDAD)" else "DOSIS DE CAFÉ"
                    val range = if (method?.hasWaterAdjustment == true) 10f..20f else 14f..22f

                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = ratio, onValueChange = { viewModel.setRatio(it) }, valueRange = range,
                        colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.outline)
                    )

                    if (valuation.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = valuation,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("CONSEJOS DEL BARISTA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LocalDetailBlock("MOLIENDA", method?.grindSize ?: "Media", Icons.Default.Grain, Modifier.weight(1f))
                LocalDetailBlock("TEMPERATURA", method?.tempRange ?: "92-96°C", Icons.Default.Thermostat, Modifier.weight(1f))
            }
            Spacer(Modifier.height(40.dp))
        }

        BottomActionContainer {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("EMPEZAR PREPARACIÓN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
fun PreparationStep(
    timerSeconds: Int,
    remainingSeconds: Int,
    timeline: List<BrewPhaseInfo>,
    currentPhaseIndex: Int,
    isTimerRunning: Boolean,
    hasTimerStarted: Boolean,
    viewModel: BrewLabViewModel
) {
    val haptic = LocalHapticFeedback.current
    val currentPhase = timeline.getOrNull(currentPhaseIndex) ?: BrewPhaseInfo("Listo", "Proceso completado.", 0)
    val nextPhase = timeline.getOrNull(currentPhaseIndex + 1)
    val totalSeconds = timeline.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val totalProgress = (timerSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)

    val timerScale by animateFloatAsState(
        targetValue = if (remainingSeconds <= 5 && isTimerRunning) 1.1f else 1f,
        animationSpec = if (remainingSeconds <= 5 && isTimerRunning) {
            infiniteRepeatable(
                animation = tween(500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            tween(300)
        },
        label = "timerScale"
    )

    LaunchedEffect(currentPhaseIndex) {
        if (hasTimerStarted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Box {
                    WaterWaveAnimation(
                        progress = totalProgress,
                        color = CaramelAccent.copy(alpha = 0.05f),
                        modifier = Modifier.matchParentSize()
                    )

                    Column {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(
                                text = currentPhase.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = String.format("%02d:%02d", remainingSeconds / 60, remainingSeconds % 60),
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                                fontWeight = FontWeight.Black,
                                color = if (remainingSeconds <= 5 && isTimerRunning) ElectricRed else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .graphicsLayer(scaleX = timerScale, scaleY = timerScale)
                            )

                            val nextLabel = nextPhase?.label ?: "Finalizar"
                            Text(
                                text = "Siguiente: $nextLabel",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(24.dp))

                            BrewTimeline(phases = timeline, elapsedTotalSeconds = timerSeconds)

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("TOTAL %02d:%02d", totalSeconds / 60, totalSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%02d:%02d", timerSeconds / 60, timerSeconds % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(24.dp)
                        ) {
                            Text(
                                text = currentPhase.instruction,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        BottomActionContainer {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (hasTimerStarted) {
                    OutlinedButton(
                        onClick = { viewModel.resetTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("REINICIAR", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.toggleTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) ElectricRed else MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if (isTimerRunning) Color.White else MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTimerRunning) "PAUSAR" else "INICIAR", fontWeight = FontWeight.Bold, color = if (isTimerRunning) Color.White else MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun PhaseDurationLabel(phase: BrewPhaseInfo) {
    Text(
        text = "${phase.durationSeconds}s",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun BrewTimeline(phases: List<BrewPhaseInfo>, elapsedTotalSeconds: Int) {
    val totalSeconds = phases.sumOf { it.durationSeconds }.coerceAtLeast(1)
    val coffeeBrown = CaramelAccent
    val softGray = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    Column(Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                Box(
                    modifier = Modifier.weight(weight),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    PhaseDurationLabel(phase)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(Modifier
            .fillMaxWidth()
            .height(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            var elapsedBeforeThisPhase = 0
            phases.forEach { phase ->
                val weight = (phase.durationSeconds.toFloat() / totalSeconds).coerceAtLeast(0.05f)
                val phaseProgress = if (elapsedTotalSeconds <= elapsedBeforeThisPhase) {
                    0f
                } else if (elapsedTotalSeconds >= elapsedBeforeThisPhase + phase.durationSeconds) {
                    1f
                } else {
                    (elapsedTotalSeconds - elapsedBeforeThisPhase).toFloat() / phase.durationSeconds
                }

                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(softGray)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(phaseProgress)
                            .fillMaxHeight()
                            .background(coffeeBrown)
                    )
                }
                elapsedBeforeThisPhase += phase.durationSeconds
            }
        }
    }
}

@Composable
fun ResultStep(
    selectedTaste: String?,
    recommendation: String?,
    selectedItem: PantryItemWithDetails?,
    viewModel: BrewLabViewModel,
    onNavigateToDiary: () -> Unit
) {
    val tastes = listOf(
        "Amargo" to Icons.Default.LocalFireDepartment,
        "Ácido" to Icons.Default.Science,
        "Equilibrado" to Icons.Default.Verified,
        "Salado" to Icons.Default.Waves,
        "Acuoso" to Icons.Default.WaterDrop,
        "Aspero" to Icons.Default.Grain,
        "Dulce" to Icons.Default.Favorite
    )

    Column(Modifier.fillMaxSize()) {
        Column(Modifier
            .weight(1f)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(24.dp))

            PremiumCard {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = "¿QUÉ SABOR HAS OBTENIDO?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        tastes.chunked(2).forEach { rowTastes ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowTastes.forEach { (label, icon) ->
                                    TasteChip(
                                        label = label.uppercase(),
                                        icon = icon,
                                        isSelected = selectedTaste == label,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        viewModel.onTasteFeedback(label)
                                    }
                                }
                                if (rowTastes.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = recommendation != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(32.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(24.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "Recomendación",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = recommendation ?: "",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 24.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(40.dp))
        }

        BottomActionContainer {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.resetAll() },
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("REINICIAR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Button(
                    onClick = { viewModel.saveToDiary { onNavigateToDiary() } },
                    enabled = selectedItem != null && selectedTaste != null,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("GUARDAR EN DIARIO", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
fun TasteChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shadowElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PantrySelectionCard(item: PantryItemWithDetails, onClick: () -> Unit) {
    val progress = if (item.pantryItem.totalGrams > 0) item.pantryItem.gramsRemaining.toFloat() / item.pantryItem.totalGrams else 0f

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.coffee.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = item.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                Text(text = item.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun BottomActionContainer(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 100.dp),
        content = content
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun DataBlock(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = valueColor)
    }
}

@Composable
fun TasteOption(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WaterWaveAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2196F3).copy(alpha = 0.15f)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waveHeight = 10.dp.toPx()
        val baseFillHeight = height * (1f - progress.coerceIn(0f, 1f))

        val path = Path()
        path.moveTo(0f, baseFillHeight)

        for (x in 0..width.toInt()) {
            val progressX = x.toFloat() / width
            val y = baseFillHeight + waveHeight * sin((progressX * 2.0 * PI + waveOffset.toDouble())).toFloat()
            path.lineTo(x.toFloat(), y)
        }

        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        clipRect {
            drawPath(path, color = color)
        }
    }
}

@Composable
fun LocalDetailBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// --- TIMELINE COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    highlightedCommentId: Int? = null,
    viewModel: CommentsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    var text by remember { mutableStateOf("") }
    val comments by viewModel.comments.collectAsState()
    val suggestions by viewModel.mentionSuggestions.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var editingCommentId by remember { mutableStateOf<Int?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = comments.size > 5)

    LaunchedEffect(postId) {
        viewModel.setPostId(postId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Comentarios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (comments.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    Text("No hay comentarios todavía", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp, top = 8.dp)
                ) {
                    items(comments) { item ->
                        CommentRow(
                            commentWithAuthor = item,
                            isOwnComment = item.author.id == activeUser?.id,
                            isHighlighted = highlightedCommentId == item.comment.id,
                            onNavigateToProfile = onNavigateToProfile,
                            onDeleteClick = { viewModel.deleteComment(item.comment.id) },
                            onEditClick = {
                                editingCommentId = item.comment.id
                                text = item.comment.text
                            },
                            onMentionClick = { username ->
                                scope.launch {
                                    viewModel.getUserIdByUsername(username)?.let { id ->
                                        onNavigateToProfile(id)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        //.background(Color(0xFFF8F8F8)) // Removed gray strip
                        .padding(horizontal = 16.dp, vertical = 4.dp), // Reduced vertical padding
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { user ->
                        SuggestionChip(user) {
                            val parts = text.split(" ").toMutableList()
                            parts[parts.size - 1] = "@${user.username} "
                            text = parts.joinToString(" ")
                            viewModel.onTextChanged(text)
                        }
                    }
                }
            }

            Surface(
                color = Color.White
            ) {
                Column {
                    if (editingCommentId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CoffeeBrown.copy(alpha = 0.1f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Editando comentario", style = MaterialTheme.typography.labelSmall, color = CoffeeBrown)
                            IconButton(onClick = {
                                editingCommentId = null
                                text = ""
                            }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, null, tint = CoffeeBrown)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = text,
                            onValueChange = {
                                text = it
                                viewModel.onTextChanged(it)
                            },
                            placeholder = { Text("Añade un comentario...") },
                            modifier = Modifier.weight(1f),
                            shape = CircleShape,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CoffeeBrown)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (editingCommentId != null) {
                                    viewModel.updateComment(editingCommentId!!, text)
                                    editingCommentId = null
                                } else {
                                    onAddComment(text)
                                }
                                text = ""
                            },
                            enabled = text.isNotBlank(),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = CoffeeBrown)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(comments, highlightedCommentId) {
        if (highlightedCommentId == null) return@LaunchedEffect
        val index = comments.indexOfFirst { it.comment.id == highlightedCommentId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsBottomSheet(
    notifications: List<TimelineNotification>,
    unreadIds: Set<String>,
    followingIds: Set<Int>,
    onDismiss: () -> Unit,
    onFollowToggle: (Int) -> Unit,
    onNotificationClick: (TimelineNotification) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)) {
            Text(
                text = "NOTIFICACIONES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp)
            )

            if (notifications.isEmpty()) {
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text("No tienes notificaciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        val isUnread = notification.id in unreadIds
                        when (notification) {
                            is TimelineNotification.Follow -> {
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "${notification.user.fullName} empezó a seguirte.",
                                    subtitle = "@${notification.user.username}",
                                    isUnread = isUnread,
                                    trailingContent = {
                                        val isFollowing = followingIds.contains(notification.user.id)
                                        val buttonColors = if (isFollowing) {
                                            ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        }
                                        val buttonModifier = Modifier.height(32.dp)
                                        if (isFollowing) {
                                            OutlinedButton(
                                                onClick = { onFollowToggle(notification.user.id) },
                                                modifier = buttonModifier,
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                colors = buttonColors
                                            ) {
                                                Text("SIGUIENDO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        } else {
                                            Button(
                                                onClick = { onFollowToggle(notification.user.id) },
                                                modifier = buttonModifier,
                                                shape = RoundedCornerShape(12.dp),
                                                colors = buttonColors
                                            ) {
                                                Text("SEGUIR", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    },
                                    onClick = { onNotificationClick(notification) }
                                )
                            }
                            is TimelineNotification.Mention -> {
                                val preview = notification.commentText.replace("\n", " ").take(80)
                                NotificationRow(
                                    avatarUrl = notification.user.avatarUrl,
                                    title = "${notification.user.fullName} te mencionó en un comentario.",
                                    subtitle = preview,
                                    isUnread = isUnread,
                                    trailingContent = null,
                                    onClick = { onNotificationClick(notification) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    avatarUrl: String,
    title: String,
    subtitle: String,
    isUnread: Boolean,
    trailingContent: (@Composable () -> Unit)?,
    onClick: () -> Unit
) {
    val unreadColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(10.dp)
                            .background(unreadColor, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trailingContent != null) {
                Spacer(Modifier.width(12.dp))
                trailingContent()
            }
        }
    }
}

@Composable
private fun CommentRow(
    commentWithAuthor: CommentWithAuthor,
    isOwnComment: Boolean,
    isHighlighted: Boolean,
    onNavigateToProfile: (Int) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onMentionClick: (String) -> Unit
) {
    val author = commentWithAuthor.author
    val comment = commentWithAuthor.comment
    var showMenu by remember { mutableStateOf(false) }

    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                if (isHighlighted) highlightColor else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp)
    ) {
        AsyncImage(
            model = author.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { onNavigateToProfile(author.id) }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = author.username,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable { onNavigateToProfile(author.id) }
            )

            val annotatedContent = buildAnnotatedStringWithMentions(comment.text)
            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedContent,
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    annotatedContent.getStringAnnotations("MENTION", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            onMentionClick(annotation.item)
                        }
                }
            )
        }

        if (isOwnComment) {
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        onClick = {
                            showMenu = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Borrar", color = ElectricRed) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}

private fun buildAnnotatedStringWithMentions(text: String): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("@(\\w+)")
        var lastIndex = 0

        regex.findAll(text).forEach { matchResult ->
            append(text.substring(lastIndex, matchResult.range.first))
            val username = matchResult.groupValues[1]
            pushStringAnnotation(tag = "MENTION", annotation = username)
            withStyle(style = SpanStyle(color = CoffeeBrown, fontWeight = FontWeight.Bold)) {
                append(matchResult.value)
            }
            pop()
            lastIndex = matchResult.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
private fun SuggestionChip(user: UserEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color.LightGray)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier
                .size(20.dp)
                .clip(CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(user.username, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
fun PremiumTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(54.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .premiumBorder(RoundedCornerShape(28.dp))
                .padding(4.dp)
        ) {
            AnimatedTabIndicator(selectedTabIndex, tabs.size)
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = title, style = MaterialTheme.typography.labelLarge, color = contentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StockSliderSection(label: String, value: Float, maxValue: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${value.roundToInt()} g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
        Slider(
            value = value, 
            onValueChange = onValueChange, 
            valueRange = 0f..maxValue.coerceAtLeast(1f), 
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                Text(text = value.uppercase(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableDiaryItem(entry: DiaryEntryEntity, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    
    SwipeToDismissBox(
        state = dismissState, 
        enableDismissFromStartToEnd = false, 
        backgroundContent = { 
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) { 
                Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(end = 16.dp)) 
            } 
        }
    ) {
        DiaryEntryItem(entry)
    }
}

@Composable
fun DiaryEntryItem(entry: DiaryEntryEntity) {
    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
    Surface(
        modifier = Modifier.fillMaxWidth(), 
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (entry.type == "WATER") Color(0xFFE3F2FD).copy(alpha = if (isSystemInDarkTheme()) 0.2f else 1f) else MaterialTheme.colorScheme.primary.copy(
                            alpha = 0.15f
                        ),
                        CircleShape
                    ), 
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee, 
                    null, 
                    tint = if (entry.type == "WATER") Color(0xFF2196F3) else MaterialTheme.colorScheme.onSurface, 
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text("$dateStr • ${if (entry.type == "WATER") "${entry.amountMl}ml" else "${entry.caffeineAmount}mg - ${entry.preparationType}"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text(
                text = "Recomendaciones OMS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            InfoRow("Máximo Diario", "400 mg", "Aprox. 4 espressos. Límite seguro para adultos sanos.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)
            InfoRow("Embarazo", "200 mg", "Se recomienda reducir el consumo a la mitad.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)
            InfoRow("Hidratación", "2.5 L", "El agua es vital. El café deshidrata ligeramente.")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, desc: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(item: PantryItemWithDetails, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var total by remember { mutableStateOf(item.pantryItem.totalGrams.toFloat()) }
    var rem by remember { mutableStateOf(item.pantryItem.gramsRemaining.toFloat()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Editar Stock",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(32.dp))
            StockSliderSection("Bolsa total", total, 1000f) { total = it; if (rem > it) rem = it }
            Spacer(Modifier.height(24.dp))
            StockSliderSection("Restante", rem, total) { rem = it }
            Spacer(Modifier.height(40.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt()) }, 
                    Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) { 
                    Text("GUARDAR", fontWeight = FontWeight.Bold) 
                }
            }
        }
    }
}

/**
 * Componente estandarizado para opciones de menú en modales inferiores.
 */
@Composable
fun ModalMenuOption(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostOptionsBottomSheet(
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            ModalMenuOption(
                title = "Editar",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = onEditClick
            )
            ModalMenuOption(
                title = "Borrar",
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.primary,
                onClick = onDeleteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewOptionsBottomSheet(
    onDismiss: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            ModalMenuOption(
                title = "Editar",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = onEditClick
            )
            ModalMenuOption(
                title = "Borrar",
                icon = Icons.Default.Delete,
                color = MaterialTheme.colorScheme.primary,
                onClick = onDeleteClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    onDismiss: () -> Unit, 
    onEditClick: () -> Unit, 
    onLogoutClick: () -> Unit,
    isHealthConnectAvailable: Boolean = false,
    healthConnectEnabled: Boolean = false,
    onHealthConnectToggle: (Boolean) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = MaterialTheme.colorScheme.surface, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 48.dp, start = 24.dp, end = 24.dp)) {
            // Sección General
            Text(
                "GENERAL", 
                style = MaterialTheme.typography.labelMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            
            ModalMenuOption(
                title = "Editar Perfil",
                icon = Icons.Default.Edit,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onDismiss(); onEditClick() }
            )
            ModalMenuOption(
                title = "Cerrar Sesión",
                icon = Icons.AutoMirrored.Filled.Logout,
                color = MaterialTheme.colorScheme.primary,
                onClick = { onDismiss(); onLogoutClick() }
            )
            
            if (isHealthConnectAvailable) {
                Spacer(Modifier.height(24.dp))
                
                // Sección Servicios
                Text(
                    "SERVICIOS", 
                    style = MaterialTheme.typography.labelMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo Health Connect PNG
                        Image(
                            painter = painterResource(id = R.drawable.health_connect),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Health Connect", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Sincroniza cafeína y agua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { onHealthConnectToggle(!healthConnectEnabled) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (healthConnectEnabled) SuccessGreen else MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(36.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (healthConnectEnabled) "CONECTADO" else "CONECTA",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostBottomSheet(
    initialText: String,
    initialImage: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState, 
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Editar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descripción") },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onConfirm(text, imageUrl) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("GUARDAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReviewBottomSheet(
    initialRating: Float,
    initialComment: String,
    initialImage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Float, String, String?) -> Unit
) {
    var rating by remember { mutableFloatStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Editar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            // Simple Rating bar replacement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (rating > i) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { rating = (i + 1).toFloat() }
                            .size(32.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Tu opinión") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onConfirm(rating, comment, imageUrl) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ACTUALIZAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("ELIMINAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
