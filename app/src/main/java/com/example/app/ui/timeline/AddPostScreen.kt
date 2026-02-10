package com.cafesito.app.ui.timeline

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.UserEntity
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.theme.DarkCoffeeBean
import com.cafesito.app.ui.theme.DarkOutline
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBackClick: () -> Unit = {},
    viewModel: AddPostViewModel = hiltViewModel()
) {
    val isDarkTheme = isSystemInDarkTheme()
    val modalBackgroundColor = if (isDarkTheme) DarkOutline else MaterialTheme.colorScheme.surface

    val currentStep by viewModel.currentStep.collectAsState()
    val postType by viewModel.postType.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val comment by viewModel.comment.collectAsState()
    val rating by viewModel.rating.collectAsState()

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
            CenterAlignedTopAppBar(
                title = {
                    val titleText = when {
                        currentStep == 0 && postType == PostType.PUBLICATION -> "nuevo post"
                        currentStep == 0 && postType == PostType.OPINION -> "nueva reseña"
                        currentStep == 1 && postType == PostType.PUBLICATION -> "nuevo post"
                        currentStep == 2 && postType == PostType.OPINION -> "nueva reseña"
                        else -> "detalles"
                    }
                    Text(
                        text = titleText.uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 0) {
                            onBackClick()
                        } else {
                            if (postType == PostType.OPINION && currentStep == 2) {
                                viewModel.goToStep(0)
                            } else {
                                viewModel.goToStep(currentStep - 1)
                            }
                        }
                    }) {
                        Icon(
                            imageVector = if (currentStep == 0) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBackIos, 
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                actions = {
                    val isLastStep = when(postType) {
                        PostType.PUBLICATION -> currentStep == 1
                        PostType.OPINION -> currentStep == 2
                    }

                    if (isLastStep) {
                        TextButton(
                            onClick = { 
                                if (postType == PostType.PUBLICATION) viewModel.createPost(onBackClick)
                                else viewModel.submitReview(onBackClick)
                            },
                            enabled = if (postType == PostType.PUBLICATION) true else (rating > 0 && comment.isNotBlank())
                        ) {
                            Text(
                                "PUBLICAR", 
                                color = if (if (postType == PostType.PUBLICATION) true else (rating > 0 && comment.isNotBlank())) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        val canNext = when (postType) {
                            PostType.PUBLICATION -> currentStep == 0
                            PostType.OPINION -> (selectedCoffee != null && currentStep == 0)
                        }
                        if (canNext) {
                            IconButton(onClick = { 
                                if (postType == PostType.OPINION) viewModel.goToStep(2) else viewModel.goToStep(currentStep + 1) 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            
            AnimatedContent(
                targetState = currentStep, 
                transitionSpec = { fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it } },
                label = "FlowTransition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when {
                    postType == PostType.PUBLICATION && step == 0 -> PhotoSelectionStepPremium(
                        viewModel = viewModel, 
                        onCameraClick = { cameraLauncher.launch(null) },
                    )
                    postType == PostType.PUBLICATION && step == 1 -> PostDetailsStepPremium(viewModel = viewModel, activeUser = activeUser)
                    
                    postType == PostType.OPINION && step == 0 -> CoffeeSelectionStepPremium(viewModel)
                    postType == PostType.OPINION && step == 2 -> ReviewDetailsStepPremium(viewModel = viewModel, activeUser = activeUser)
                }
            }

            // ✅ ALINEACIÓN EXACTA CON EL MENÚ PRINCIPAL
            if (currentStep == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding() // Respeta la barra de sistema igual que el menú principal
                        .padding(bottom = 12.dp), // Margen vertical idéntico al del menú inferior (Surface horizontal padding)
                    contentAlignment = Alignment.Center
                ) {
                    PremiumTabRow(
                        selectedTabIndex = if(postType == PostType.PUBLICATION) 0 else 1,
                        tabs = listOf("POST", "RESEÑA"),
                        onTabSelected = { viewModel.setPostType(if(it == 0) PostType.PUBLICATION else PostType.OPINION) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSelectionStepPremium(
    viewModel: AddPostViewModel, 
    onCameraClick: () -> Unit,
) {
    val imageSource by viewModel.imageSource.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Main Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imageSource != null) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Photo, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Gallery Label and Camera Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Galería",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            IconButton(onClick = onCameraClick) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(galleryImages) { uri ->
                val isSelected = imageSource == uri
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.setImage(uri) }
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.2f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        ) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(20.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(2.dp)
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
private fun PostDetailsStepPremium(viewModel: AddPostViewModel, activeUser: UserEntity?) {
    val imageSource by viewModel.imageSource.collectAsState()
    val comment by viewModel.comment.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = activeUser?.avatarUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = activeUser?.fullName ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${activeUser?.username ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = comment,
            onValueChange = viewModel::onCommentChanged,
            placeholder = { Text("¿Qué estás pensando?") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(Modifier.height(16.dp))
        
        AsyncImage(
            model = imageSource,
            contentDescription = "Selected Photo",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.FillWidth
        )

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun CoffeeSelectionStepPremium(viewModel: AddPostViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = viewModel::onSearchQueryChanged, placeholder = { Text("¿Qué café estás catando?") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Spacer(Modifier.height(24.dp))
        Text("SUGERENCIAS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.weight(1f), 
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDetailsStepPremium(viewModel: AddPostViewModel, activeUser: UserEntity?) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val comment by viewModel.comment.collectAsState()
    val rating by viewModel.rating.collectAsState()
    
    var showPickerSheet by remember { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) viewModel.setCapturedImage(bitmap)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setImage(uri)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = activeUser?.avatarUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = activeUser?.fullName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${activeUser?.username ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            SemicircleRatingBar(rating = rating, onRatingChanged = viewModel::onRatingChanged)
        }
        
        OutlinedTextField(
            value = comment,
            onValueChange = viewModel::onCommentChanged,
            placeholder = { Text("¿Qué te ha parecido este café?") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                modifier = Modifier.size(56.dp).clickable { showPickerSheet = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                color = if (imageSource == null) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
            ) {
                if (imageSource != null) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            selectedCoffee?.let { coffeeDetails ->
                Surface(
                    modifier = Modifier.height(56.dp).widthIn(max = 220.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    color = if (isDarkTheme) DarkCoffeeBean else Color.White
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
                        AsyncImage(
                            model = coffeeDetails.coffee.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = coffeeDetails.coffee.nombre,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(end = 16.dp).weight(1f, fill = false)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    if (showPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            containerColor = modalBackgroundColor
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text("AÑADIR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                ModalMenuOption("Hacer Foto", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                    cameraLauncher.launch(null)
                    showPickerSheet = false
                }
                ModalMenuOption("Elegir de Galería", Icons.Default.Collections, MaterialTheme.colorScheme.primary) {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showPickerSheet = false
                }
            }
        }
    }
}
