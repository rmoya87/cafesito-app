package com.cafesito.app.ui.timeline

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import com.cafesito.app.data.CoffeeWithDetails
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBackClick: () -> Unit = {},
    viewModel: AddPostViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val postType by viewModel.postType.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    var showMainGallerySheet by remember { mutableStateOf(false) }

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permissionState = rememberPermissionState(permission)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap -> 
        viewModel.setCapturedImage(bitmap)
    }

    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            viewModel.loadGalleryImages()
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GlassyTopBar(
                title = if (currentStep == 0) "NUEVA PUBLICACIÓN" else "DETALLES",
                onBackClick = if (currentStep == 0) onBackClick else { { viewModel.goToStep(currentStep - 1) } },
                actions = {
                    val canNext = when (postType) {
                        PostType.PUBLICATION -> imageSource != null && currentStep == 0
                        PostType.OPINION -> (selectedCoffee != null && currentStep == 0) || (imageSource != null && currentStep == 1)
                    }
                    if (canNext) {
                        IconButton(onClick = { viewModel.goToStep(currentStep + 1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp), 
                contentAlignment = Alignment.Center
            ) {
                val comment by viewModel.comment.collectAsState()
                val rating by viewModel.rating.collectAsState()

                val isLastStep = when(postType) {
                    PostType.PUBLICATION -> currentStep == 1
                    PostType.OPINION -> currentStep == 2
                }

                if (currentStep == 0) {
                    PremiumTabRow(
                        selectedTabIndex = if(postType == PostType.PUBLICATION) 0 else 1,
                        tabs = listOf("POST", "OPINIÓN"),
                        onTabSelected = { viewModel.setPostType(if(it == 0) PostType.PUBLICATION else PostType.OPINION) }
                    )
                } else if (isLastStep) {
                    Button(
                        onClick = { 
                            if (postType == PostType.PUBLICATION) viewModel.createPost(onBackClick)
                            else viewModel.submitReview(onBackClick)
                        }, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 24.dp), 
                        enabled = if (postType == PostType.PUBLICATION) comment.isNotBlank() else (rating > 0 && comment.isNotBlank()), 
                        shape = RoundedCornerShape(30.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "PUBLICAR",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            AnimatedContent(
                targetState = currentStep, 
                transitionSpec = { fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it } },
                label = "FlowTransition"
            ) { step ->
                when {
                    postType == PostType.PUBLICATION && step == 0 -> PhotoSelectionStepPremium(
                        viewModel = viewModel, 
                        onCameraClick = { cameraLauncher.launch(null) },
                        onShowGalleryManual = { showMainGallerySheet = true }
                    )
                    postType == PostType.PUBLICATION && step == 1 -> PostDetailsStepPremium(viewModel = viewModel)
                    
                    postType == PostType.OPINION && step == 0 -> CoffeeSelectionStepPremium(viewModel)
                    postType == PostType.OPINION && step == 1 -> ReviewPhotoSelectionStep(
                        viewModel = viewModel,
                        onCameraClick = { cameraLauncher.launch(null) },
                        onShowGalleryManual = { showMainGallerySheet = true }
                    )
                    postType == PostType.OPINION && step == 2 -> ReviewDetailsStepPremium(viewModel = viewModel)
                }
            }
        }
    }

    if (showMainGallerySheet) {
        val galleryImages by viewModel.galleryImages.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { showMainGallerySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().height(600.dp).padding(16.dp)) {
                Text("SELECCIONA UNA FOTO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(galleryImages) { uri ->
                        Box(
                            modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).clickable { 
                                viewModel.setImage(uri)
                                showMainGallerySheet = false
                            }
                        ) {
                            AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoSelectionStepPremium(
    viewModel: AddPostViewModel, 
    onCameraClick: () -> Unit,
    onShowGalleryManual: () -> Unit
) {
    val imageSource by viewModel.imageSource.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .clickable { onShowGalleryManual() }, 
            contentAlignment = Alignment.Center
        ) {
            if (imageSource != null) {
                AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("SELECCIONA UNA FOTO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                }
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1.2f).background(MaterialTheme.colorScheme.surface), 
            contentPadding = PaddingValues(1.dp), 
            horizontalArrangement = Arrangement.spacedBy(1.dp), 
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                Box(modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onCameraClick() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            items(galleryImages) { uri: Uri ->
                val isSelected = imageSource == uri
                Box(modifier = Modifier.aspectRatio(1f).clickable { viewModel.setImage(uri) }) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f)).border(2.dp, MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@Composable
private fun ReviewPhotoSelectionStep(
    viewModel: AddPostViewModel, 
    onCameraClick: () -> Unit,
    onShowGalleryManual: () -> Unit
) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Mantenemos el café seleccionado arriba
        PremiumCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = selectedCoffee?.coffee?.imageUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(selectedCoffee?.coffee?.nombre ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(selectedCoffee?.coffee?.marca ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable { onShowGalleryManual() }, 
            contentAlignment = Alignment.Center
        ) {
            if (imageSource != null) {
                AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddPhotoAlternate, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("FOTO DE TU CAFÉ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                }
            }
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1.2f).background(MaterialTheme.colorScheme.surface), 
            contentPadding = PaddingValues(1.dp), 
            horizontalArrangement = Arrangement.spacedBy(1.dp), 
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                Box(modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onCameraClick() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            items(galleryImages) { uri: Uri ->
                val isSelected = imageSource == uri
                Box(modifier = Modifier.aspectRatio(1f).clickable { viewModel.setImage(uri) }) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.4f)).border(2.dp, MaterialTheme.colorScheme.primary))
                }
            }
        }
    }
}

@Composable
private fun PostDetailsStepPremium(viewModel: AddPostViewModel) {
    val imageSource by viewModel.imageSource.collectAsState()
    val comment by viewModel.comment.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        PremiumCard {
            Column(Modifier.padding(12.dp)) {
                AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("TU HISTORIA", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = comment, onValueChange = viewModel::onCommentChanged, placeholder = { Text("Comparte tu experiencia...") }, 
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun CoffeeSelectionStepPremium(viewModel: AddPostViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = viewModel::onSearchQueryChanged, placeholder = { Text("¿Qué café estás catando?") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(Modifier.height(24.dp))
        Text("SUGERENCIAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(coffeeList) { coffee ->
                PremiumCard(modifier = Modifier.fillMaxWidth().clickable { viewModel.selectCoffee(coffee) }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = coffee.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(coffee.coffee.nombre, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(coffee.coffee.marca.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewDetailsStepPremium(viewModel: AddPostViewModel) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val comment by viewModel.comment.collectAsState()
    val rating by viewModel.rating.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Text("RESUMEN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = selectedCoffee?.coffee?.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(selectedCoffee?.coffee?.nombre ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(selectedCoffee?.coffee?.marca ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        if (imageSource != null) {
            Spacer(Modifier.height(24.dp))
            PremiumCard {
                AsyncImage(
                    model = imageSource, 
                    contentDescription = null, 
                    modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(24.dp)), 
                    contentScale = ContentScale.FillWidth
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("CALIFICACIÓN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        SemicircleRatingBar(rating = rating, onRatingChanged = viewModel::onRatingChanged)
        
        Spacer(Modifier.height(32.dp))
        Text("TU OPINIÓN", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = comment, onValueChange = viewModel::onCommentChanged, placeholder = { Text("¿Qué te ha parecido este café?") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(Modifier.height(120.dp))
    }
}
