package com.example.cafesito.ui.timeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.cafesito.data.CoffeeWithDetails
import com.example.cafesito.ui.components.*
import com.example.cafesito.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBackClick: () -> Unit = {},
    viewModel: AddPostViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val postType by viewModel.postType.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()

    Scaffold(
        containerColor = SoftOffWhite,
        topBar = {
            GlassyTopBar(
                title = if (currentStep == 0) "NUEVA PUBLICACIÓN" else "DETALLES",
                onBackClick = if (currentStep == 0) onBackClick else { { viewModel.goToStep(0) } },
                actions = {
                    val canNext = when (postType) {
                        PostType.PUBLICATION -> imageSource != null
                        PostType.OPINION -> selectedCoffee != null
                    }
                    if (currentStep == 0 && canNext) {
                        IconButton(onClick = { viewModel.goToStep(1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = EspressoDeep, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            AnimatedContent(
                targetState = currentStep, 
                transitionSpec = { fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it } },
                label = "FlowTransition"
            ) { step ->
                when {
                    postType == PostType.PUBLICATION && step == 0 -> PhotoSelectionStepPremium(viewModel)
                    postType == PostType.PUBLICATION && step == 1 -> PostDetailsStepPremium(onSuccess = onBackClick, viewModel = viewModel)
                    postType == PostType.OPINION && step == 0 -> CoffeeSelectionStepPremium(viewModel)
                    postType == PostType.OPINION && step == 1 -> ReviewDetailsStepPremium(onSuccess = onBackClick, viewModel = viewModel)
                }
            }
            
            if (currentStep == 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 32.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    PremiumTabRow(
                        selectedTabIndex = if(postType == PostType.PUBLICATION) 0 else 1,
                        tabs = listOf("MOMENTO", "RESEÑA"),
                        onTabSelected = { viewModel.setPostType(if(it == 0) PostType.PUBLICATION else PostType.OPINION) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSelectionStepPremium(viewModel: AddPostViewModel) {
    val imageSource by viewModel.imageSource.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { if (it != null) viewModel.setCapturedImage(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(SoftOffWhite), contentAlignment = Alignment.Center) {
            if (imageSource != null) {
                AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("SELECCIONA UNA FOTO", style = MaterialTheme.typography.labelLarge, color = Color.LightGray)
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1.2f).background(Color.White), 
            contentPadding = PaddingValues(1.dp), 
            horizontalArrangement = Arrangement.spacedBy(1.dp), 
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                Box(modifier = Modifier.aspectRatio(1f).background(CreamLight).clickable { cameraLauncher.launch(null) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, null, tint = EspressoDeep)
                }
            }
            items(galleryImages) { uri ->
                val isSelected = imageSource == uri
                Box(modifier = Modifier.aspectRatio(1f).clickable { viewModel.setImage(uri) }) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f)).border(2.dp, CaramelAccent))
                }
            }
        }
    }
}

@Composable
private fun PostDetailsStepPremium(onSuccess: () -> Unit, viewModel: AddPostViewModel) {
    val imageSource by viewModel.imageSource.collectAsState()
    var comment by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        PremiumCard {
            Column(Modifier.padding(12.dp)) {
                AsyncImage(
                    model = imageSource, 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)), 
                    contentScale = ContentScale.Crop
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Text("TU HISTORIA", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = comment, 
            onValueChange = { comment = it }, 
            placeholder = { Text("Comparte tu experiencia...") }, 
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), 
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent)
        )
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { viewModel.createPost(comment, onSuccess) }, 
            modifier = Modifier.fillMaxWidth().height(60.dp), 
            enabled = comment.isNotBlank(), 
            shape = RoundedCornerShape(30.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep)
        ) {
            Text("PUBLICAR", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
private fun CoffeeSelectionStepPremium(viewModel: AddPostViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("¿Qué café estás catando?") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EspressoDeep) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent)
        )
        
        Spacer(Modifier.height(24.dp))
        Text("RECIENTES", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(coffeeList) { coffee ->
                PremiumCard(modifier = Modifier.clickable { viewModel.selectCoffee(coffee) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = coffee.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(coffee.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(coffee.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = CaramelAccent, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDetailsStepPremium(onSuccess: () -> Unit, viewModel: AddPostViewModel) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { if (it != null) viewModel.setCapturedImage(it) }
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("CAFÉ SELECCIONADO", style = MaterialTheme.typography.labelLarge, color = CaramelAccent)
        Spacer(Modifier.height(16.dp))
        
        PremiumCard {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = selectedCoffee?.coffee?.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(selectedCoffee?.coffee?.nombre ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(selectedCoffee?.coffee?.marca ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(32.dp)).background(CreamLight).clickable { cameraLauncher.launch(null) }.border(1.dp, BorderLight, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageSource != null && !imageSource.toString().contains("picsum")) {
                AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(32.dp), tint = CaramelAccent)
                    Spacer(Modifier.height(8.dp))
                    Text("AÑADIR FOTO", style = MaterialTheme.typography.labelSmall, color = CaramelAccent)
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("CALIFICACIÓN", style = MaterialTheme.typography.labelLarge, color = CaramelAccent, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        SemicircleRatingBar(rating = rating, onRatingChanged = { rating = it })
        
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = comment, onValueChange = { comment = it }, label = { Text("Nota de cata...") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CaramelAccent)
        )
        
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { viewModel.submitReview(rating, comment, onSuccess) }, 
            modifier = Modifier.fillMaxWidth().height(60.dp), 
            enabled = rating > 0 && comment.isNotBlank(), 
            shape = RoundedCornerShape(30.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = CaramelAccent)
        ) {
            Text("GUARDAR RESEÑA", fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(40.dp))
    }
}
