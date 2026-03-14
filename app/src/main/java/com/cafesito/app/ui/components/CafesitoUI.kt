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
import coil.compose.AsyncImage
import com.cafesito.app.R
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
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
    shape: RoundedCornerShape = Shapes.shapePremium,
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
fun Modifier.premiumBorder(shape: RoundedCornerShape = Shapes.shapePremium) = 
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
                .background(MaterialTheme.colorScheme.primary, Shapes.pill)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassyTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    navigationContent: (@Composable () -> Unit)? = null,
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
            if (navigationContent != null) {
                navigationContent()
            } else if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    val isDark = isSystemInDarkTheme()
    val baseAlpha = if (isDark) 0.12f else 0.08f
    val animAlpha = baseAlpha + alpha
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = animAlpha.coerceIn(0.05f, 0.3f)), Shapes.card)
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
fun ProfileActivityCardShimmer() {
    PremiumCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerItem(modifier = Modifier.size(56.dp).clip(Shapes.cardSmall))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ShimmerItem(modifier = Modifier.height(18.dp).fillMaxWidth(0.6f))
                Spacer(Modifier.height(6.dp))
                ShimmerItem(modifier = Modifier.height(14.dp).fillMaxWidth(0.4f))
            }
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
        shape = Shapes.card, 
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
    PremiumCard(shape = Shapes.shapeXl) {
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

/** Skeleton de carga para Home (Timeline): carrusel métodos 100x100, Tu despensa (160dp cards), Cafés recomendados (220x180). */
@Composable
fun TimelineLoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(5) {
                        ShimmerItem(Modifier.size(100.dp).clip(Shapes.shapeCardMedium))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                ShimmerItem(Modifier.padding(horizontal = 16.dp).height(22.dp).width(140.dp))
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        ShimmerItem(Modifier.width(160.dp).height(220.dp).clip(Shapes.pill))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                ShimmerItem(Modifier.padding(horizontal = 16.dp).height(22.dp).width(180.dp))
                Spacer(Modifier.height(12.dp))
                LazyRow(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(3) {
                        ShimmerItem(Modifier.width(220.dp).height(180.dp).clip(Shapes.cardSmall))
                    }
                }
            }
        }
    }
}

/** Una fila de skeleton que imita CoffeePremiumListItem: 86dp altura, imagen 60dp + texto. */
@Composable
private fun SearchListItemShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerItem(Modifier.size(60.dp).clip(Shapes.cardSmall))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            ShimmerItem(Modifier.height(18.dp).fillMaxWidth(0.85f))
            Spacer(Modifier.height(8.dp))
            ShimmerItem(Modifier.height(14.dp).fillMaxWidth(0.5f))
        }
    }
}

/** Skeleton de carga para Explorar (Search): lista de filas tipo CoffeePremiumListItem. */
@Composable
fun SearchLoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(10) {
            Surface(
                shape = Shapes.card,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                SearchListItemShimmer()
            }
        }
    }
}

/** Skeleton de carga para Elaboración (BrewLab): carrusel métodos, card café, tipo/tamaño, config. */
@Composable
fun BrewLabLoadingContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(5) {
                    ShimmerItem(Modifier.size(140.dp).clip(Shapes.pill))
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Row(
                    Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerItem(Modifier.size(56.dp).clip(Shapes.cardSmall))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        ShimmerItem(Modifier.height(18.dp).fillMaxWidth(0.7f))
                        Spacer(Modifier.height(8.dp))
                        ShimmerItem(Modifier.height(14.dp).fillMaxWidth(0.4f))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ShimmerItem(Modifier.height(16.dp).width(80.dp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { ShimmerItem(Modifier.height(36.dp).width(90.dp).clip(Shapes.cardSmall)) }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ShimmerItem(Modifier.height(16.dp).width(100.dp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { ShimmerItem(Modifier.height(36.dp).width(80.dp).clip(Shapes.cardSmall)) }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Column(Modifier.padding(24.dp)) {
                    ShimmerItem(Modifier.height(18.dp).width(180.dp))
                    Spacer(Modifier.height(16.dp))
                    ShimmerItem(Modifier.fillMaxWidth().height(80.dp).clip(Shapes.cardSmall))
                }
            }
        }
    }
}

/** Skeleton de carga para detalle de café: imagen, título, bloques de datos. */
@Composable
fun DetailLoadingContent() {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            ShimmerItem(Modifier.fillMaxWidth().height(280.dp))
            Spacer(Modifier.height(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                ShimmerItem(Modifier.height(28.dp).fillMaxWidth(0.85f))
                Spacer(Modifier.height(8.dp))
                ShimmerItem(Modifier.height(20.dp).fillMaxWidth(0.5f))
                Spacer(Modifier.height(24.dp))
                repeat(4) {
                    ShimmerItem(Modifier.height(48.dp).fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.height(16.dp))
                ShimmerItem(Modifier.height(24.dp).width(120.dp))
                Spacer(Modifier.height(12.dp))
                ShimmerItem(Modifier.height(80.dp).fillMaxWidth())
                ShimmerItem(Modifier.height(80.dp).fillMaxWidth())
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
