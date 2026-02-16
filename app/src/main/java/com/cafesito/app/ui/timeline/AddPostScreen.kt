package com.cafesito.app.ui.timeline

import android.Manifest
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.data.UserEntity
import com.cafesito.app.ui.components.*
import com.cafesito.app.ui.search.BarcodeActionIcon
import com.cafesito.app.ui.theme.DarkCoffeeBean
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import java.io.File
import java.util.UUID

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBackClick: () -> Unit = {},
    onPublishSuccess: () -> Unit = {},
    initialPostType: PostType? = null,
    viewModel: AddPostViewModel = hiltViewModel()
) {
    val isDarkTheme = isSystemInDarkTheme()
    val modalBackgroundColor = if (isDarkTheme) MaterialTheme.colorScheme.surfaceContainer else MaterialTheme.colorScheme.surface

    val currentStep by viewModel.currentStep.collectAsState()
    val postType by viewModel.postType.collectAsState()
    val imageSource by viewModel.imageSource.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val comment by viewModel.comment.collectAsState()
    val rating by viewModel.rating.collectAsState()

    LaunchedEffect(initialPostType) {
        initialPostType?.let(viewModel::setPostType)
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val context = LocalContext.current
    val mediaPermissionsState = rememberMultiplePermissionsState(permissions)
    val allPermissionsGranted = mediaPermissionsState.allPermissionsGranted
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) pendingCameraUri?.let(viewModel::setImage)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.setImage(it) }
    }

    LaunchedEffect(allPermissionsGranted) {
        if (allPermissionsGranted) {
            viewModel.loadGalleryImages()
        } else {
            mediaPermissionsState.launchMultiplePermissionRequest()
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
                            imageVector = if (currentStep == 0) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
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
                                if (postType == PostType.PUBLICATION) viewModel.createPost {
                                    onPublishSuccess()
                                    onBackClick()
                                }
                                else viewModel.submitReview {
                                    onPublishSuccess()
                                    onBackClick()
                                }
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
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
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
                        onCameraClick = {
                            val uri = createTempImageUri(context)
                            pendingCameraUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                    postType == PostType.PUBLICATION && step == 1 -> PostDetailsStepPremium(
                        viewModel = viewModel, 
                        activeUser = activeUser,
                        galleryLauncher = galleryLauncher,
                        cameraLauncher = cameraLauncher
                    )
                    
                    postType == PostType.OPINION && step == 0 -> CoffeeSelectionStepPremium(viewModel)
                    postType == PostType.OPINION && step == 2 -> ReviewDetailsStepPremium(
                        viewModel = viewModel, 
                        activeUser = activeUser,
                        isDarkTheme = isDarkTheme,
                        modalBackgroundColor = modalBackgroundColor,
                        galleryLauncher = galleryLauncher,
                        cameraLauncher = cameraLauncher
                    )
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
                .aspectRatio(1.2f)
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Photo, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Selecciona una foto", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Gallery Label and Camera/More Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(4.dp)
            ) {
                Text(
                    text = "Galería",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onCameraClick) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 140.dp, top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (galleryImages.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                   Box(
                       modifier = Modifier.fillMaxSize().padding(top = 40.dp), 
                       contentAlignment = Alignment.Center
                   ) {
                       Text(
                           text = "No hay imágenes para mostrar.", 
                           style = MaterialTheme.typography.bodySmall, 
                           color = MaterialTheme.colorScheme.onSurfaceVariant, 
                           textAlign = TextAlign.Center
                       )
                   }
                }
            }

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
                                .background(Color.Black.copy(alpha = 0.3f))
                                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
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
private fun PostDetailsStepPremium(
    viewModel: AddPostViewModel, 
    activeUser: UserEntity?,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
) {
    val imageSource = viewModel.imageSource.collectAsState().value as? Uri
    val comment by viewModel.comment.collectAsState()
    val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    var commentValue by remember(comment) {
        mutableStateOf(TextFieldValue(comment, selection = TextRange(comment.length)))
    }
    var showPickerSheet by remember { mutableStateOf(false) }
    var showCoffeeSelector by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

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

        Box(modifier = Modifier.fillMaxWidth()) {
            // Main Composer Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSystemInDarkTheme()) Color.Black else Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                    OutlinedTextField(
                        value = commentValue,
                        onValueChange = {
                            commentValue = it
                            viewModel.onCommentChanged(it.text)
                            if (it.text.endsWith("@")) showEmojiPanel = false
                        },
                        placeholder = { Text("¿Qué estás pensando?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = if (isSystemInDarkTheme()) Color.White else Color.Black,
                            unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                        )
                    )

                    // Suggestions/Emojis moved inside above icons
                    AnimatedVisibility(
                        visible = (mentionSuggestions.isNotEmpty() && !showEmojiPanel && !commentValue.text.trim().endsWith("@")) || showEmojiPanel,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                            if (showEmojiPanel) {
                                FadingLazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    items(listOf("😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎")) { emoji ->
                                        Surface(
                                            onClick = {
                                                val updated = commentValue.text + emoji
                                                commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                                viewModel.onCommentChanged(updated)
                                                focusRequester.requestFocus()
                                                keyboardController?.show()
                                            },
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                                            color = MaterialTheme.colorScheme.surface
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(emoji, fontSize = 16.sp)
                                            }
                                        }
                                    }
                                }
                            } else if (mentionSuggestions.isNotEmpty() && !commentValue.text.trim().endsWith("@")) {
                                FadingLazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    items(mentionSuggestions) { user ->
                                        SuggestionChip(user = user) {
                                            val updated = insertOrReplaceMentionToken(commentValue.text, user.username)
                                            commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                            viewModel.onCommentChanged(updated)
                                            focusRequester.requestFocus()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ComposerActionRow(
                        onToggleEmoji = {
                            showEmojiPanel = !showEmojiPanel
                            if (!showEmojiPanel) keyboardController?.show()
                        },
                        onInsertMention = {
                            val updated = commentValue.text + "@"
                            commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                            viewModel.onCommentChanged(updated)
                            showEmojiPanel = false
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

        Spacer(Modifier.height(16.dp))

        Surface(
            onClick = { showCoffeeSelector = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Coffee, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Añadir café", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = selectedCoffee?.coffee?.nombre ?: "Seleccionar café",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(end = 20.dp)
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (imageSource != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = "Post Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clickable { viewModel.setImage(null) },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
        } else {
             Surface(
                onClick = { showPickerSheet = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Añadir foto", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(100.dp))
    }

    if (showCoffeeSelector) {
        ModalBottomSheet(
            onDismissRequest = { showCoffeeSelector = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text("SELECCIONAR CAFÉ", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Buscar café") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                val options = GmsBarcodeScannerOptions.Builder()
                                    .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                                    .build()
                                GmsBarcodeScanning.getClient(context, options)
                                    .startScan()
                                    .addOnSuccessListener { barcode ->
                                        barcode.rawValue?.let { value ->
                                            viewModel.onSearchQueryChanged(value)
                                        }
                                    }
                            }) {
                                BarcodeActionIcon(tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = if (isSystemInDarkTheme()) Color.Black else Color.White,
                        focusedContainerColor = if (isSystemInDarkTheme()) Color.Black else Color.White
                    )
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(coffeeList.take(10)) { coffee ->
                        Surface(
                            onClick = {
                                viewModel.selectCoffee(coffee)
                                showCoffeeSelector = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = coffee.coffee.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(coffee.coffee.nombre, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(coffee.coffee.marca, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (coffee.averageRating > 0f) {
                                    AssistChip(onClick = {}, enabled = false, label = { Text(String.format(java.util.Locale.getDefault(), "%.1f", coffee.averageRating)) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPickerSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(Modifier.padding(bottom = 40.dp, start = 24.dp, end = 24.dp)) {
                Text("AÑADIR FOTO", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                ModalMenuOption("Hacer Foto", Icons.Default.PhotoCamera, MaterialTheme.colorScheme.primary) {
                    val uri = createTempImageUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
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

@Composable
private fun CoffeeSelectionStepPremium(viewModel: AddPostViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        OutlinedTextField(
            value = searchQuery, 
            onValueChange = viewModel::onSearchQueryChanged, 
            placeholder = { Text("Busca café") },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val options = GmsBarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                            .build()
                        GmsBarcodeScanning.getClient(context, options)
                            .startScan()
                            .addOnSuccessListener { barcode ->
                                barcode.rawValue?.let { value ->
                                    viewModel.onSearchQueryChanged(value)
                                }
                            }
                    }) {
                        BarcodeActionIcon(tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            },
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = if (isSystemInDarkTheme()) Color.Black else Color.White,
                focusedContainerColor = if (isSystemInDarkTheme()) Color.Black else Color.White
            )
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
                        AsyncImage(model = coffee.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Fit)
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
private fun ReviewDetailsStepPremium(
    viewModel: AddPostViewModel, 
    activeUser: UserEntity?,
    isDarkTheme: Boolean,
    modalBackgroundColor: Color,
    galleryLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    val imageSource = viewModel.imageSource.collectAsState().value as? Uri
    val comment by viewModel.comment.collectAsState()
    val rating by viewModel.rating.collectAsState()

    val mentionSuggestions by viewModel.mentionSuggestions.collectAsState()

    var commentValue by remember(comment) {
        mutableStateOf(TextFieldValue(comment, selection = TextRange(comment.length)))
    }
    var showPickerSheet by remember { mutableStateOf(false) }
    var showEmojiPanel by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    
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

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSystemInDarkTheme()) Color.Black else Color.White, RoundedCornerShape(16.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                OutlinedTextField(
                    value = commentValue,
                    onValueChange = {
                        commentValue = it
                        viewModel.onCommentChanged(it.text)
                        if (it.text.endsWith("@")) showEmojiPanel = false
                    },
                    placeholder = { Text("¿Qué te ha parecido este café?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                        .focusRequester(focusRequester),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = if (isSystemInDarkTheme()) Color.White else Color.Black,
                        unfocusedTextColor = if (isSystemInDarkTheme()) Color.White else Color.Black
                    )
                )

                // Suggestions/Emojis moved inside above icons
                AnimatedVisibility(
                    visible = (mentionSuggestions.isNotEmpty() && !showEmojiPanel && !commentValue.text.trim().endsWith("@")) || showEmojiPanel,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        if (showEmojiPanel) {
                            FadingLazyRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                items(listOf("😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎")) { emoji ->
                                    Surface(
                                        onClick = {
                                            val updated = commentValue.text + emoji
                                            commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                            viewModel.onCommentChanged(updated)
                                            focusRequester.requestFocus()
                                            keyboardController?.show()
                                        },
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
                                        color = MaterialTheme.colorScheme.surface
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(emoji, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        } else if (mentionSuggestions.isNotEmpty() && !commentValue.text.trim().endsWith("@")) {
                            FadingLazyRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                items(mentionSuggestions) { user ->
                                    SuggestionChip(user = user) {
                                        val updated = insertOrReplaceMentionToken(commentValue.text, user.username)
                                        commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                                        viewModel.onCommentChanged(updated)
                                        focusRequester.requestFocus()
                                    }
                                }
                            }
                        }
                    }
                }

                ComposerActionRow(
                    onToggleEmoji = {
                        showEmojiPanel = !showEmojiPanel
                        if (!showEmojiPanel) keyboardController?.show()
                    },
                    onInsertMention = {
                        val updated = commentValue.text + "@"
                        commentValue = TextFieldValue(updated, selection = TextRange(updated.length))
                        viewModel.onCommentChanged(updated)
                        showEmojiPanel = false
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (imageSource != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = "Review Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clickable { viewModel.setImage(null) },
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
        } else {
             Surface(
                onClick = { showPickerSheet = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text("Añadir foto", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        selectedCoffee?.let { coffeeDetails ->
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                color = if (isDarkTheme) Color.Black else Color.White
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight()) {
                    AsyncImage(
                        model = coffeeDetails.coffee.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)),
                        contentScale = ContentScale.Fit
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
                    val uri = createTempImageUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposerActionRow(
    onToggleEmoji: () -> Unit,
    onInsertMention: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onInsertMention,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Surface(
            onClick = onToggleEmoji,
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.45f)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("😊", fontSize = 16.sp)
            }
        }
    }
}

private fun createTempImageUri(context: Context): Uri {
    val file = File(context.cacheDir, "captured_${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun insertOrReplaceMentionToken(currentText: String, username: String): String {
    val parts = currentText.trimEnd().split(" ").toMutableList()
    if (parts.isEmpty() || parts.firstOrNull().isNullOrBlank()) {
        return "@$username "
    }

    val lastIndex = parts.lastIndex
    parts[lastIndex] = if (parts[lastIndex].startsWith("@")) "@$username" else "${parts[lastIndex]} @$username"
    return parts.joinToString(" ") + " "
}
