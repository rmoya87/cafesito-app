package com.example.cafesito.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.cafesito.data.DiaryEntryEntity
import com.example.cafesito.data.PantryItemWithDetails
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.utils.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(32.dp),
    containerColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.4f))
    ) {
        Column(content = content)
    }
}

fun Modifier.premiumBorder(shape: RoundedCornerShape = RoundedCornerShape(32.dp)) = 
    this.border(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.4f), shape)

@Composable
fun borderLight() = BorderStroke(1.dp, Color(0xFFE0E0E0).copy(alpha = 0.4f))

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
                    brush = Brush.linearGradient(listOf(CaramelAccent, EspressoDeep)),
                    shape = CircleShape
                )
        )
        
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(size - 8.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
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
                .background(EspressoDeep, RoundedCornerShape(24.dp))
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBackIos, contentDescription = "Back", modifier = Modifier.size(20.dp))
                }
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.White.copy(alpha = 0.85f),
            scrolledContainerColor = Color.White.copy(alpha = 0.95f)
        )
    )
}

@Composable
fun ShimmerItem(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    )
}

@Composable
fun PremiumTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color.Transparent)) {
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .height(54.dp)
                .background(Color.White, RoundedCornerShape(28.dp))
                .premiumBorder(RoundedCornerShape(28.dp))
                .padding(4.dp)
        ) {
            AnimatedTabIndicator(selectedTabIndex, tabs.size)
            Row(modifier = Modifier.fillMaxSize()) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val contentColor by animateColorAsState(if (isSelected) Color.White else Color.Gray)
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).clickable { onTabSelected(index) },
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text("${value.roundToInt()} g", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = EspressoDeep)
        Slider(
            value = value, 
            onValueChange = onValueChange, 
            valueRange = 0f..maxValue.coerceAtLeast(1f), 
            colors = SliderDefaults.colors(
                thumbColor = CaramelAccent,
                activeTrackColor = CaramelAccent,
                inactiveTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun DetailPremiumBlock(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    PremiumCard(modifier = modifier) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).background(CaramelAccent.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CaramelAccent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontSize = 8.sp)
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
                    .background(Color.Red.copy(alpha = 0.8f)), 
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
        color = Color.White, 
        shape = RoundedCornerShape(16.dp), 
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (entry.type == "WATER") Color(0xFFE3F2FD) else CaramelAccent.copy(alpha = 0.15f),
                        CircleShape
                    ), 
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (entry.type == "WATER") Icons.Default.WaterDrop else Icons.Default.Coffee, 
                    null, 
                    tint = if (entry.type == "WATER") Color(0xFF2196F3) else EspressoDeep, 
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.coffeeName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = Color.Black)
                Text("$dateStr • ${if (entry.type == "WATER") "${entry.amountMl}ml" else "${entry.caffeineAmount}mg - ${entry.preparationType}"}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
            Text("Recomendaciones OMS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            InfoRow("Máximo Diario", "400 mg", "Aprox. 4 espressos. Límite seguro para adultos sanos.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            InfoRow("Embarazo", "200 mg", "Se recomienda reducir el consumo a la mitad.")
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            InfoRow("Hidratación", "2.5 L", "El agua es vital. El café deshidrata ligeramente.")
        }
    }
}

@Composable
fun InfoRow(label: String, value: String, desc: String) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(value, color = CaramelAccent, fontWeight = FontWeight.Black)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditBottomSheet(item: PantryItemWithDetails, onDismiss: () -> Unit, onSave: (Int, Int) -> Unit) {
    var total by remember { mutableFloatStateOf(item.pantryItem.totalGrams.toFloat()) }
    var rem by remember { mutableFloatStateOf(item.pantryItem.gramsRemaining.toFloat()) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
            Text("Editar Stock", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CaramelAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CaramelAccent)
                ) {
                    Text("CANCELAR", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { onSave(total.roundToInt(), rem.roundToInt()) }, 
                    Modifier.weight(1f).height(56.dp), 
                    colors = ButtonDefaults.buttonColors(containerColor = CaramelAccent),
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
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
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
                color = Color.DarkGray
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit, onEditClick: () -> Unit, onLogoutClick: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        containerColor = Color.White, 
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
            Text(
                "AJUSTES", 
                style = MaterialTheme.typography.labelLarge, 
                color = CaramelAccent,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            ModalMenuOption(
                title = "Editar Perfil",
                icon = Icons.Default.Edit,
                color = EspressoDeep,
                onClick = { onDismiss(); onEditClick() }
            )
            ModalMenuOption(
                title = "Cerrar Sesión",
                icon = Icons.AutoMirrored.Filled.Logout,
                color = ErrorRed,
                onClick = { onDismiss(); onLogoutClick() }
            )
        }
    }
}

@Composable
fun EditPostDialog(
    initialText: String, 
    initialImage: String,
    onDismiss: () -> Unit, 
    onConfirm: (String, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var imageUrl by remember { mutableStateOf(initialImage) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Publicación", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
            }
        },
        confirmButton = { Button(onClick = { onConfirm(text, imageUrl) }, colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep)) { Text("GUARDAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) } },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun EditReviewDialog(
    initialRating: Float, 
    initialComment: String, 
    initialImage: String?,
    onDismiss: () -> Unit, 
    onConfirm: (Float, String, String?) -> Unit
) {
    var rating by remember { mutableFloatStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    var imageUrl by remember { mutableStateOf(initialImage) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { imageUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Opinión", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, null, tint = Color.Gray)
                    }
                }
                Spacer(Modifier.height(16.dp))
                // Simple Rating bar replacement
                Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (rating > i) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = CaramelAccent,
                            modifier = Modifier.clickable { rating = (i + 1).toFloat() }
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
            }
        },
        confirmButton = { Button(onClick = { onConfirm(rating, comment, imageUrl) }, colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep)) { Text("ACTUALIZAR") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR", color = Color.Gray) } },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}
