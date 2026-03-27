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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
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
import androidx.compose.ui.res.stringResource
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
import com.cafesito.app.data.Coffee
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.data.DiaryEntryEntity
import com.cafesito.app.data.PantryItemWithDetails
import com.cafesito.app.data.UserEntity
import com.cafesito.app.data.ProfileActivityItem
import com.cafesito.app.data.UserListRow
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

/** Formato igual que webapp toRelativeMinutes: "X MIN", "X H", "X D" (más reciente a menos). */
private fun formatRelativeTime(timestampMillis: Long): String {
    val diffMs = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val mins = (diffMs / 60_000).coerceAtLeast(1L).toInt()
    return when {
        mins < 60 -> "${mins} MIN"
        else -> {
            val hrs = mins / 60
            if (hrs < 24) "${hrs} H" else "${hrs / 24} D"
        }
    }
}

// --- PROFILE COMPONENTS ---

@Composable
fun ProfileAdn(
    state: ProfileUiState.Success,
    listState: LazyListState = rememberLazyListState(),
    onShowSensoryDetail: () -> Unit
) {
    LazyColumn(state = listState, contentPadding = PaddingValues(top = Spacing.space4, bottom = 120.dp)) {
        item {
            PremiumCard(
                modifier = Modifier
                    .padding(horizontal = Spacing.space4, vertical = Spacing.space2)
                    .clickable(onClick = onShowSensoryDetail),
                containerColor = if (isSystemInDarkTheme()) PureBlack else MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Spacing.space6), horizontalAlignment = Alignment.CenterHorizontally) {
                    SensoryRadarChart(
                        data = state.sensoryProfile,
                        lineColor = MaterialTheme.colorScheme.primary,
                        fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.space2))
                    Text(
                        stringResource(id = R.string.profile_taste_discover),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(id = R.string.profile_taste_summary),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space8, vertical = Spacing.space2)
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
    LazyColumn(state = listState, contentPadding = PaddingValues(top = Spacing.space4, bottom = 120.dp)) {
        items(favoriteCoffees) { item ->
            Box(Modifier.padding(horizontal = Spacing.space4, vertical = Spacing.space1)) {
                CoffeeFavoritePremiumItem(
                    coffeeDetails = item,
                    onFavoriteClick = { viewModel.onToggleFavorite(item.coffee.id, false) },
                    onClick = { onCoffeeClick(item.coffee.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensoryDetailBottomSheet(profile: Map<String, Float>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space6)
        ) {
            Text(
                text = stringResource(id = R.string.profile_preferences_analysis_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.space6))

            val sorted = profile.toList().sortedByDescending { it.second }
            val highest = sorted.getOrNull(0)
            val second = sorted.getOrNull(1)

            if (highest != null) {
                val primaryName = highest.first.lowercase()
                val secondaryName = second?.first?.lowercase()

                Text(
                    text = if (secondaryName != null && second.second > 3f) {
                        stringResource(id = R.string.profile_palate_combo, primaryName, secondaryName)
                    } else {
                        stringResource(id = R.string.profile_palate_primary, primaryName)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(Spacing.space3))

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

                Spacer(Modifier.height(Spacing.space6))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                    shape = Shapes.card,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(id = R.string.brew_recommendation), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(Spacing.space2))
                            Text(stringResource(id = R.string.profile_ideal_recommendation).uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(Spacing.space3))
                        Text(text = stringResource(id = R.string.profile_should_try, idealType), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = stringResource(id = R.string.profile_suggested_origins, idealOrigin), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.space8))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = Shapes.shapeXl
            ) { Text(stringResource(id = R.string.profile_continue_exploring).uppercase(), fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary) }
        }
    }
}

/** Tarjeta de actividad unificada: mismo diseño, textos y disposición que webapp (Perfil > Actividad). */
@Composable
fun ProfileActivityCard(
    item: ProfileActivityItem,
    activeUserId: Int?,
    onUserClick: (Int) -> Unit,
    onCoffeeClick: (String) -> Unit,
    onListClick: ((Int, String) -> Unit)?,
    isCurrentUser: Boolean
) {
    val displayName = if (activeUserId != null && item.userId == activeUserId) "Tú" else item.userName
    val type = when (item) {
        is ProfileActivityItem.Review -> "review"
        is ProfileActivityItem.FirstTimeCoffee -> "diary"
        is ProfileActivityItem.AddedToList -> "favorite"
    }
    val isOwn = activeUserId != null && item.userId == activeUserId
    val displayLabel = when (type) {
        "review" -> if (isOwn) stringResource(id = R.string.profile_activity_review_own) else stringResource(id = R.string.profile_activity_review_other)
        "diary" -> if (isOwn) stringResource(id = R.string.profile_activity_first_own) else stringResource(id = R.string.profile_activity_first_other)
        else -> if (isOwn) stringResource(id = R.string.profile_activity_list_own) else stringResource(id = R.string.profile_activity_list_other)
    }

    val coffeeId: String
    val coffeeName: String
    val coffeeImageUrl: String
    val coffeeBrand: String
    val listId: String?
    val listName: String?
    val rating: Float?
    val comment: String?
    val showListBadge: Boolean
    when (item) {
        is ProfileActivityItem.Review -> {
            coffeeId = item.reviewInfo.coffeeDetails.coffee.id
            coffeeName = item.reviewInfo.coffeeDetails.coffee.nombre.ifBlank { stringResource(id = R.string.home_coffee_fallback) }
            coffeeImageUrl = item.reviewInfo.coffeeDetails.coffee.imageUrl
            coffeeBrand = item.reviewInfo.coffeeDetails.coffee.marca.ifBlank { stringResource(id = R.string.detail_brand_fallback) }
            listId = null
            listName = null
            rating = item.reviewInfo.review.rating
            comment = item.reviewInfo.review.comment.takeIf { it.isNotBlank() }
            showListBadge = false
        }
        is ProfileActivityItem.FirstTimeCoffee -> {
            coffeeId = item.coffeeId
            coffeeName = item.coffeeName.ifBlank { stringResource(id = R.string.home_coffee_fallback) }
            coffeeImageUrl = item.coffeeImageUrl
            coffeeBrand = item.coffeeBrand.ifBlank { stringResource(id = R.string.detail_brand_fallback) }
            listId = null
            listName = null
            rating = null
            comment = null
            showListBadge = false
        }
        is ProfileActivityItem.AddedToList -> {
            coffeeId = item.coffeeId
            coffeeName = item.coffeeName.ifBlank { stringResource(id = R.string.home_coffee_fallback) }
            coffeeImageUrl = item.coffeeImageUrl
            coffeeBrand = item.coffeeBrand.ifBlank { stringResource(id = R.string.detail_brand_fallback) }
            listId = item.listId
            listName = item.listName
            rating = null
            comment = null
            showListBadge = true
        }
    }

    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) PureBlack else MaterialTheme.colorScheme.surface
    val coffeeCardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)

    PremiumCard(modifier = Modifier.fillMaxWidth(), containerColor = cardBg) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.space4, vertical = Spacing.space3),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar (clickable → perfil usuario)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onUserClick(item.userId) },
                contentAlignment = Alignment.Center
            ) {
                if (!item.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.avatarUrl,
                        contentDescription = stringResource(id = R.string.profile_avatar_cd, displayName),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = displayName.take(2).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(Spacing.space3))
            Column(Modifier.weight(1f)) {
                // Línea de texto: "Tú" / nombre + label (segunda/tercera persona)
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                            append(displayName)
                        }
                        append(" $displayLabel")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Meta: tiempo relativo en mayúsculas (como webapp)
                Text(
                    text = formatRelativeTime(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.space1)
                )
                // Bloque café (mismo ancho que el resto): por tipo → lista (icono+nombre) / reseña (★ X/5 + comentario) / fila principal (imagen, nombre, marca, flecha)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.space3)
                        .clip(Shapes.cardSmall)
                        .then(
                            if (type == "review") Modifier.clickable { onCoffeeClick(coffeeId) }
                            else Modifier
                        ),
                    color = coffeeCardBg
                ) {
                    Column(modifier = Modifier.padding(Spacing.space3)) {
                        // Tipo lista (favorite): icono lista + nombre lista; tap lleva a esa lista. Debajo separador y fila café.
                        if (showListBadge && listId != null && onListClick != null) {
                            val isFavoritesList = listId == "favorites" || (listName?.equals("Favoritos", ignoreCase = true) == true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onListClick(item.userId, listId) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isFavoritesList) Icons.Default.Favorite else Icons.AutoMirrored.Filled.FormatListBulleted,
                                    contentDescription = if (isFavoritesList) stringResource(id = R.string.profile_favorites_title) else stringResource(id = R.string.profile_tab_lists),
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isFavoritesList) ElectricRed else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.width(Spacing.space2))
                                Text(
                                    text = listName?.ifBlank { stringResource(id = R.string.profile_tab_lists) } ?: stringResource(id = R.string.profile_tab_lists),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(Modifier.padding(vertical = Spacing.space2), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        // Tipo opinión (review): chip ★ X/5 (amarillo-naranja) + texto nota negro (noche) / blanco (día); comentario mismo criterio
                        if (rating != null || !comment.isNullOrBlank()) {
                            val noteTextColor = if (isDark) PureBlack else PureWhite
                            val reviewCommentColor = if (isDark) Color(0xFFf5f5f5) else Color(0xFF1a1a1a)
                            val noteChipOrange = Color(0xFFFFB74D)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (rating != null) {
                                    Surface(
                                        shape = Shapes.pillFull,
                                        color = noteChipOrange
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = Spacing.space1),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Star, contentDescription = stringResource(id = R.string.common_rating), modifier = Modifier.size(Spacing.space3), tint = noteTextColor)
                                            Spacer(Modifier.width(Spacing.space1))
                                            Text(
                                                text = "${rating.toInt()}/5",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = noteTextColor,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (!comment.isNullOrBlank()) Spacer(Modifier.width(Spacing.space2))
                                }
                                if (!comment.isNullOrBlank()) {
                                    Text(
                                        text = comment,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = reviewCommentColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = Spacing.space2), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                        // Fila principal (todos los tipos): imagen 48dp, nombre, marca en mayúsculas, flecha → detalle café (en review el tap lo maneja la Surface)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (type != "review") Modifier.clickable { onCoffeeClick(coffeeId) }
                                    else Modifier
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(Shapes.small)
                                    .background(LocalCaramelAccent.current.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (coffeeImageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = coffeeImageUrl,
                                        contentDescription = coffeeName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = coffeeName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = LocalCaramelAccent.current
                                    )
                                }
                            }
                            Spacer(Modifier.width(Spacing.space3))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = coffeeName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = coffeeBrand.uppercase(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open_coffee), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun ProfileStatsRow(
    activityCount: Int,
    followers: Int,
    following: Int,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space4),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(stringResource(id = R.string.profile_followers_label), followers.toString(), onFollowersClick)
        StatItem(stringResource(id = R.string.profile_following_label), following.toString(), onFollowingClick)
    }
}

@Composable
fun StatItem(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(Shapes.cardSmall)
            .let {
                if (onClick != null) it.clickable(onClick = onClick) else it
            }
            .padding(Spacing.space2)
    ) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
    }
}

/** Fila "Crea una lista nueva" en la pestaña Listas (mismo ancho completo que Favoritos). Bordes más cuadrados. */
@Composable
fun ListRowCreateList(onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = Shapes.card) {
        Row(
            modifier = Modifier.padding(Spacing.space4).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.profile_create_list), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Spacing.space6))
            Spacer(Modifier.width(Spacing.space4))
            Text(stringResource(id = R.string.profile_create_list), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Fila "Favoritos" en la pestaña Listas: corazón + nombre + flecha. Bordes más cuadrados. */
@Composable
fun ListRowFavoritos(onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = Shapes.card) {
        Row(
            modifier = Modifier.padding(Spacing.space4).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Favorite, contentDescription = stringResource(id = R.string.profile_favorites_title), tint = ElectricRed, modifier = Modifier.size(Spacing.space6))
            Spacer(Modifier.width(Spacing.space4))
            Text(stringResource(id = R.string.profile_favorites_title).lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Fila de lista personalizada: icono lista + nombre + flecha. Bordes más cuadrados. */
@Composable
fun ListRowCustomList(name: String, onClick: () -> Unit) {
    PremiumCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = Shapes.card) {
        Row(
            modifier = Modifier.padding(Spacing.space4).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberListAltSvgPainter(),
                contentDescription = stringResource(id = R.string.profile_list_default_name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Spacing.space6)
            )
            Spacer(Modifier.width(Spacing.space4))
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Modal para crear/editar lista: título centrado, nombre, privacidad (3 opciones) y "Permitir editar lista". Paridad con WebApp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, privacy: String, membersCanEdit: Boolean) -> Unit,
    listIdForEdit: String? = null,
    initialName: String = "",
    initialIsPublic: Boolean = false,
    initialPrivacy: String? = null,
    initialMembersCanEdit: Boolean = false,
    onUpdate: ((name: String, privacy: String, membersCanEdit: Boolean) -> Unit)? = null
) {
    var name by remember(listIdForEdit, initialName) { mutableStateOf(initialName) }
    val initialPr = initialPrivacy ?: if (initialIsPublic) "public" else "private"
    var privacy by remember(listIdForEdit, initialPr) { mutableStateOf(initialPr) }
    var membersCanEdit by remember(listIdForEdit, initialMembersCanEdit) { mutableStateOf(initialMembersCanEdit) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isSystemInDarkTheme()
    val fieldBackground = if (isDark) PureBlack else PureWhite
    val fieldTextColor = if (isDark) PureWhite else PureBlack
    val editFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldBackground,
        unfocusedContainerColor = fieldBackground,
        focusedBorderColor = LocalCaramelAccent.current,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    )
    val editFieldTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Medium,
        color = fieldTextColor
    )
    val isEditMode = listIdForEdit != null
    val canSave = name.trim().isNotEmpty()
    val listPrivacyOptions = listOf(
        Triple("public", stringResource(id = R.string.list_privacy_public), stringResource(id = R.string.list_privacy_public_desc)),
        Triple("invitation", stringResource(id = R.string.list_privacy_invitation), stringResource(id = R.string.list_privacy_invitation_desc)),
        Triple("private", stringResource(id = R.string.list_privacy_private), stringResource(id = R.string.list_privacy_private_desc))
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space2).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    if (isEditMode) stringResource(id = R.string.list_edit) else stringResource(id = R.string.profile_new_list),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
                TextButton(
                    onClick = {
                        if (canSave) {
                            if (isEditMode) onUpdate?.invoke(name.trim(), privacy, membersCanEdit) else onCreate(name.trim(), privacy, membersCanEdit)
                            onDismiss()
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(if (isEditMode) stringResource(id = R.string.brew_save) else stringResource(id = R.string.profile_create_list), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(Spacing.space6))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(id = R.string.profile_list_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = editFieldTextStyle,
                colors = editFieldColors,
                shape = Shapes.card
            )
            Spacer(Modifier.height(Spacing.space4))
            Text(
                stringResource(id = R.string.list_privacy_section),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.space2))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(12.dp)) {
                    listPrivacyOptions.forEach { (value, label, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = privacy == value,
                                onClick = { privacy = value }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (privacy == "public" || privacy == "invitation") {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(id = R.string.list_allow_members_edit), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = membersCanEdit,
                                onCheckedChange = { membersCanEdit = it },
                                colors = SwitchDefaults.colors(
                                    uncheckedTrackColor = if (isSystemInDarkTheme()) Color(0xFFB0B0B0) else Color(0xFF757575),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

/** Fila de opción estilo modal (icono + texto + chevron), bordes redondeados como en modal de opciones. */
@Composable
private fun AddToListOptionRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space4, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(Spacing.space6), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(Spacing.space3))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.common_open), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private const val ADD_TO_LIST_FAVORITES_ID = "__favorites"

/** Dueño de la lista o miembro con permiso de edición (listas públicas / por invitación). */
private fun UserListRow.canUserEditListItems(currentUserId: Int): Boolean {
    val me = currentUserId.toLong()
    val owner = userId == me
    return owner || membersCanEdit == true
}

private fun String.normalizedListId(): String = trim().lowercase()

private fun Set<String>.containsListId(listId: String): Boolean =
    any { it.normalizedListId() == listId.normalizedListId() }

@Composable
private fun AddToListCheckboxRow(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    Surface(
        onClick = { if (enabled) onCheckedChange() },
        modifier = Modifier.fillMaxWidth().then(if (!enabled) Modifier else Modifier),
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.6f else 0.35f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.space4, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (checked) stringResource(id = R.string.profile_selected) else stringResource(id = R.string.profile_not_selected),
                tint = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(Spacing.space3))
            Box(Modifier.size(Spacing.space6), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(Spacing.space3))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Modal bottom sheet "Añadir a lista": listas editables con check si el café ya está; desmarcar quita. Listas solo lectura: check fijo si está el café. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToListBottomSheet(
    onDismiss: () -> Unit,
    currentUserId: Int,
    userLists: List<UserListRow>,
    listIdsContainingCoffee: Set<String>,
    isFavorite: Boolean,
    onCreateListRequest: () -> Unit,
    onApply: (toAdd: Set<String>, toRemove: Set<String>, favoriteShouldBe: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialEditableWithCoffee = remember(userLists, listIdsContainingCoffee, currentUserId) {
        userLists.filter { list ->
            list.canUserEditListItems(currentUserId) && listIdsContainingCoffee.containsListId(list.id)
        }.map { it.id }.toSet()
    }
    var selectedIds by remember(userLists, listIdsContainingCoffee, isFavorite, currentUserId) {
        mutableStateOf(
            userLists.filter { list ->
                list.canUserEditListItems(currentUserId) && listIdsContainingCoffee.containsListId(list.id)
            }.map { it.id }.toSet() +
                if (isFavorite) setOf(ADD_TO_LIST_FAVORITES_ID) else emptySet()
        )
    }
    var saving by remember { mutableStateOf(false) }
    val curLists = selectedIds.filter { it != ADD_TO_LIST_FAVORITES_ID }.toSet()
    val curFav = ADD_TO_LIST_FAVORITES_ID in selectedIds
    val hasChanges = curLists != initialEditableWithCoffee || curFav != isFavorite
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space2).navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(id = R.string.profile_add_to_list),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                TextButton(
                    onClick = {
                        if (!hasChanges || saving) return@TextButton
                        saving = true
                        val toRemove = initialEditableWithCoffee - curLists
                        val toAdd = curLists - initialEditableWithCoffee
                        onApply(toAdd, toRemove, curFav)
                        onDismiss()
                    },
                    enabled = hasChanges && !saving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        if (saving) stringResource(id = R.string.profile_saving) else stringResource(id = R.string.common_apply),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            AddToListOptionRow(
                icon = {
                    Icon(
                        painter = rememberListAltAddSvgPainter(),
                        contentDescription = stringResource(id = R.string.profile_create_list),
                        modifier = Modifier.size(Spacing.space6),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                label = stringResource(id = R.string.profile_create_new_list),
                onClick = onCreateListRequest
            )
            userLists.forEach { list ->
                val canEdit = list.canUserEditListItems(currentUserId)
                val inList = listIdsContainingCoffee.containsListId(list.id)
                val checked = if (canEdit) list.id in selectedIds else inList
                Spacer(Modifier.height(Spacing.space2))
                AddToListCheckboxRow(
                    checked = checked,
                    enabled = canEdit,
                    onCheckedChange = {
                        selectedIds = if (list.id in selectedIds) selectedIds - list.id else selectedIds + list.id
                    },
                    icon = {
                        Icon(
                            painter = rememberListAltSvgPainter(),
                            contentDescription = stringResource(id = R.string.profile_list_default_name),
                            modifier = Modifier.size(Spacing.space6),
                            tint = if (canEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                label = list.name + if (!canEdit && inList) " (${stringResource(id = R.string.profile_read_only)})" else ""
                )
            }
            Spacer(Modifier.height(Spacing.space2))
            AddToListCheckboxRow(
                checked = ADD_TO_LIST_FAVORITES_ID in selectedIds,
                onCheckedChange = {
                    selectedIds = if (ADD_TO_LIST_FAVORITES_ID in selectedIds) selectedIds - ADD_TO_LIST_FAVORITES_ID else selectedIds + ADD_TO_LIST_FAVORITES_ID
                },
                icon = {
                    Icon(Icons.Default.Favorite, contentDescription = stringResource(id = R.string.profile_favorites_title), modifier = Modifier.size(Spacing.space6), tint = ElectricRed)
                },
                label = stringResource(id = R.string.profile_favorites_title).lowercase().replaceFirstChar { it.uppercase() }
            )
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

/** Modal de opciones de lista (Editar / Eliminar / Compartir): mismo estilo que la modal de opciones del perfil. Sin título. Iconos mismo color (onSurface). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListOptionsBottomSheet(
    onDismiss: () -> Unit,
    onEditList: () -> Unit,
    onDeleteList: () -> Unit,
    onShareList: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space2).navigationBarsPadding()) {
            AddToListOptionRow(
                icon = { Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.list_edit), modifier = Modifier.size(Spacing.space6), tint = MaterialTheme.colorScheme.onSurface) },
                label = stringResource(id = R.string.list_edit),
                onClick = { onDismiss(); onEditList() }
            )
            if (onShareList != null) {
                Spacer(Modifier.height(Spacing.space2))
                AddToListOptionRow(
                    icon = { Icon(Icons.Default.Share, contentDescription = stringResource(id = R.string.list_share), modifier = Modifier.size(Spacing.space6), tint = MaterialTheme.colorScheme.onSurface) },
                    label = stringResource(id = R.string.list_share),
                    onClick = { onDismiss(); onShareList() }
                )
            }
            Spacer(Modifier.height(Spacing.space2))
            AddToListOptionRow(
                icon = { Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.list_delete), modifier = Modifier.size(Spacing.space6), tint = MaterialTheme.colorScheme.onSurface) },
                label = stringResource(id = R.string.list_delete),
                onClick = { onDismiss(); onDeleteList() }
            )
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

/** Modal para invitar a usuarios a la lista: lista de usuarios con botón Invitar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListBottomSheet(
    users: List<UserEntity>,
    onDismiss: () -> Unit,
    onInvite: (Int) -> Unit,
    onLoadUsers: () -> Unit
) {
    LaunchedEffect(Unit) { onLoadUsers() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(Modifier.padding(top = 8.dp, start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space2).navigationBarsPadding()) {
            Text(
                stringResource(id = R.string.list_invite_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.space3)
            )
            if (users.isEmpty()) {
                Text(stringResource(id = R.string.profile_loading_users), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(Spacing.space2), modifier = Modifier.heightIn(max = 320.dp)) {
                    items(users, key = { it.id }) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = stringResource(id = R.string.profile_avatar_of, user.username),
                                modifier = Modifier.size(Spacing.space6 * 2f).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Spacer(Modifier.width(Spacing.space3))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                user.fullName.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Button(
                                onClick = { onInvite(user.id) },
                                modifier = Modifier.height(Spacing.space8),
                                shape = Shapes.cardSmall,
                                contentPadding = PaddingValues(horizontal = Spacing.space3)
                            ) {
                                Text(stringResource(id = R.string.list_invite_action), fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space6))
        }
    }
}

/** Modal de confirmación eliminar lista: mismos estilos que eliminar actividad (DeleteConfirmationDialog). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDeleteConfirmBottomSheet(
    listName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val cancelColor = if (isDark) PureWhite else PureBlack
    val deleteContainer = ElectricRed
    val deleteContent = if (isDark) PureBlack else PureWhite
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = CafesitoModalSheetDefaults.shape,
        containerColor = CafesitoModalSheetDefaults.containerColor(),
        scrimColor = CafesitoModalSheetDefaults.scrimColor,
        dragHandle = { CafesitoModalSheetDefaults.dragHandle() }
    ) {
        Column(
            modifier = Modifier.padding(start = Spacing.space6, end = Spacing.space6, top = 8.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(id = R.string.list_delete),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.space2))
            Text(
                stringResource(id = R.string.profile_delete_list_message, listName),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.space6))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.space3)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = Shapes.pillFull,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = cancelColor
                    )
                ) {
                    Text(stringResource(id = R.string.search_cancel).uppercase(), fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { onConfirm(); onDismiss() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = deleteContainer, contentColor = deleteContent),
                    shape = Shapes.pillFull
                ) {
                    Text(stringResource(id = R.string.list_delete).uppercase(), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun CoffeeFavoritePremiumItem(
    coffeeDetails: CoffeeWithDetails,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    PremiumCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(modifier = Modifier.padding(Spacing.space3), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = coffeeDetails.coffee.imageUrl,
                contentDescription = coffeeDetails.coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(Shapes.shapeCardMedium)
            )
            Spacer(Modifier.width(Spacing.space4))
            Column(modifier = Modifier.weight(1f).padding(end = Spacing.space2)) {
                Text(
                    text = coffeeDetails.coffee.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = coffeeDetails.coffee.marca.uppercase(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(painter = rememberListAltCheckSvgPainter(), contentDescription = stringResource(id = R.string.profile_tab_lists), tint = ElectricGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}


/** Fila de café en listado de favoritos/listas. Para quitar usar swipe. showListIcon = false en detalle de lista. */
@Composable
fun CoffeeFavoriteListItem(
    coffeeDetails: CoffeeWithDetails,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    showListIcon: Boolean = true
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.space3)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coffeeDetails.coffee.imageUrl,
                contentDescription = coffeeDetails.coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(Shapes.cardSmall)
            )

            Spacer(Modifier.width(Spacing.space4))

            Column(modifier = Modifier.weight(1f).padding(end = if (showListIcon) 40.dp else Spacing.space2)) {
                Text(
                    text = coffeeDetails.coffee.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coffeeDetails.coffee.marca.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showListIcon) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(24.dp)
                ) {
                    Icon(
                        painter = rememberListAltCheckSvgPainter(),
                        contentDescription = stringResource(id = R.string.common_remove_from_lists),
                        tint = ElectricGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Misma fila que favoritos pero con flecha a la derecha (sin corazón). Para historial, etc. */
@Composable
fun CoffeeListRowWithChevron(
    coffee: Coffee,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.card,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(Spacing.space3)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = coffee.imageUrl,
                contentDescription = coffee.nombre,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(Shapes.cardSmall)
            )
            Spacer(Modifier.width(Spacing.space4))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coffee.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = coffee.marca.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(id = R.string.common_open),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Spacing.space6)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFavoriteItem(
    coffeeDetails: CoffeeWithDetails,
    onRemoveFromFavorites: () -> Unit,
    onClick: () -> Unit,
    enableSwipeToRemove: Boolean = true
) {
    if (!enableSwipeToRemove) {
        CoffeeFavoriteListItem(
            coffeeDetails = coffeeDetails,
            onClick = onClick,
            onFavoriteClick = { },
            showListIcon = false
        )
        return
    }
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
                    .clip(Shapes.card)
                    .background(ElectricRed),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.common_remove_from_favorites),
                    tint = if (isSystemInDarkTheme()) PureBlack else PureWhite,
                    modifier = Modifier.padding(end = Spacing.space4)
                )
            }
        }
    ) {
        CoffeeFavoriteListItem(
            coffeeDetails = coffeeDetails,
            onClick = onClick,
            onFavoriteClick = onRemoveFromFavorites,
            showListIcon = false
        )
    }
}

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val followBg = if (isDark) CaramelSoft else CaramelAccent
    val followingBg = Color.Transparent
    val followingBorder = if (isDark) BorderStroke(1.dp, PureWhite) else BorderStroke(1.dp, PureBlack)
    val followingColor = if (isDark) PureWhite else PureBlack
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFollowing) followingBg else followBg,
            contentColor = if (isFollowing) followingColor else if (isDark) PureBlack else PureWhite
        ),
        shape = Shapes.pill,
        border = if (isFollowing) followingBorder else null
    ) {
        Text(
            if (isFollowing) stringResource(id = R.string.profile_following_label).uppercase() else stringResource(id = R.string.notifications_follow).uppercase(),
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
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
    val isDark = isSystemInDarkTheme()
    val fieldBackground = if (isDark) PureBlack else PureWhite
    val fieldTextColor = if (isDark) PureWhite else PureBlack
    val saveBackground = LocalCaramelAccent.current
    val saveTextColor = if (isDark) PureBlack else PureWhite
    val editFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = fieldBackground,
        unfocusedContainerColor = fieldBackground,
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = PureWhite,
        focusedTextColor = fieldTextColor,
        unfocusedTextColor = fieldTextColor,
        focusedLabelColor = fieldTextColor.copy(alpha = 0.78f),
        unfocusedLabelColor = fieldTextColor.copy(alpha = 0.78f)
    )
    val editFieldTextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Medium,
        color = fieldTextColor
    )

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Spacing.space2)) {
        OutlinedTextField(
            value = username,
            onValueChange = { onUsernameChange(it.take(40)) },
            label = { Text(stringResource(id = R.string.profile_edit_username)) },
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.small,
            isError = usernameError != null, supportingText = { if (usernameError != null) Text(usernameError) },
            singleLine = true,
            textStyle = editFieldTextStyle,
            colors = editFieldColors
        )
        Spacer(Modifier.height(Spacing.space3))
        OutlinedTextField(
            value = fullName,
            onValueChange = { onFullNameChange(it.take(120)) },
            label = { Text(stringResource(id = R.string.profile_edit_name)) },
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.small,
            singleLine = true,
            textStyle = editFieldTextStyle,
            colors = editFieldColors
        )
        Spacer(Modifier.height(Spacing.space3))
        OutlinedTextField(
            value = bio,
            onValueChange = { onBioChange(it.take(500)) },
            label = { Text(stringResource(id = R.string.profile_edit_bio)) },
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.small,
            minLines = 3,
            maxLines = 6,
            textStyle = editFieldTextStyle,
            colors = editFieldColors
        )
        Spacer(Modifier.height(Spacing.space6))
        Button(
            onClick = onSave, modifier = Modifier
                .fillMaxWidth(0.74f)
                .align(Alignment.CenterHorizontally)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = saveBackground,
                contentColor = saveTextColor
            ),
            shape = Shapes.shapeXl
        ) { Text(stringResource(id = R.string.brew_save).uppercase(), fontWeight = FontWeight.SemiBold, color = saveTextColor) }
    }
}
