package com.example.cafesito.ui.timeline

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.cafesito.ui.detail.SemicircleRatingBar
import com.example.cafesito.ui.theme.CoffeeBrown

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPostScreen(
    onBackClick: () -> Unit = {},
    viewModel: AddPostViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    val postType by viewModel.postType.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva publicación", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (currentStep == 0) {
                        TextButton(onClick = onBackClick) {
                            Text("Cancelar", color = Color.Gray, fontSize = 16.sp)
                        }
                    } else {
                        IconButton(onClick = { viewModel.goToStep(0) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                actions = {
                    val canGoNext = when (postType) {
                        PostType.PUBLICATION -> viewModel.imageSource.collectAsState().value != null
                        PostType.OPINION -> viewModel.selectedCoffee.collectAsState().value != null
                    }
                    if (currentStep == 0 && canGoNext) {
                        Surface(
                            onClick = { viewModel.goToStep(1) },
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp).size(40.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Siguiente", tint = Color(0xFF3C3C3C), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            
            AnimatedContent(
                targetState = postType to currentStep, 
                label = "FlowTransition",
                modifier = Modifier.fillMaxSize()
            ) { (type, step) ->
                when {
                    type == PostType.PUBLICATION && step == 0 -> PhotoSelectionStep(viewModel)
                    type == PostType.PUBLICATION && step == 1 -> PostDetailsStep(onSuccess = onBackClick, viewModel = viewModel)
                    type == PostType.OPINION && step == 0 -> CoffeeSelectionStep(viewModel)
                    type == PostType.OPINION && step == 1 -> ReviewDetailsStep(onSuccess = onBackClick, viewModel = viewModel)
                }
            }
            
            if (currentStep == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp), 
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.width(240.dp).height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = Color.White.copy(alpha = 0.95f),
                        shadowElevation = 8.dp
                    ) {
                        Row(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            PostTypeTab("Publicación", postType == PostType.PUBLICATION) { viewModel.setPostType(PostType.PUBLICATION) }
                            PostTypeTab("Opinión", postType == PostType.OPINION) { viewModel.setPostType(PostType.OPINION) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.PostTypeTab(label: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) CoffeeBrown else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isActive) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun PhotoSelectionStep(viewModel: AddPostViewModel) {
    val imageSource by viewModel.imageSource.collectAsState()
    val galleryImages by viewModel.galleryImages.collectAsState()
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { if (it != null) viewModel.setCapturedImage(it) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White), contentAlignment = Alignment.Center) {
            AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxWidth().wrapContentHeight(), contentScale = ContentScale.FillWidth)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), 
            modifier = Modifier.weight(1.2f).background(Color.White), 
            contentPadding = PaddingValues(1.dp, 1.dp, 1.dp, 100.dp), 
            horizontalArrangement = Arrangement.spacedBy(1.dp), 
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item {
                Box(modifier = Modifier.aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant).clickable { cameraLauncher.launch(null) }, contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(28.dp), tint = Color.Black)
                        Text("Cámara", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(galleryImages) { uri ->
                val isSelected = imageSource == uri
                Box(modifier = Modifier.aspectRatio(1f).clickable { viewModel.setImage(uri) }) {
                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    if (isSelected) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.3f)).border(2.dp, CoffeeBrown))
                }
            }
        }
    }
}

@Composable
private fun PostDetailsStep(onSuccess: () -> Unit, viewModel: AddPostViewModel) {
    val imageSource by viewModel.imageSource.collectAsState()
    var comment by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        AsyncImage(model = imageSource, contentDescription = null, modifier = Modifier.fillMaxWidth().wrapContentHeight().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.FillWidth)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = comment, 
            onValueChange = { comment = it }, 
            placeholder = { Text("Escribe lo que está pasando...") }, 
            modifier = Modifier.fillMaxWidth().weight(1f), 
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedBorderColor = CoffeeBrown
            )
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { viewModel.createPost(comment, onSuccess) }, modifier = Modifier.fillMaxWidth().height(54.dp), enabled = comment.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) {
            Text("Publicar", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun CoffeeSelectionStep(viewModel: AddPostViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coffeeList by viewModel.coffeeList.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("¿Sobre qué café quieres opinar?") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                unfocusedBorderColor = Color.LightGray,
                focusedBorderColor = CoffeeBrown
            )
        )
        Spacer(Modifier.height(16.dp))
        Text(text = if (searchQuery.isEmpty()) "Vistos recientemente" else "Resultados", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 100.dp)) {
            items(coffeeList) { coffee ->
                ListItem(
                    headlineContent = { Text(coffee.coffee.nombre, fontWeight = FontWeight.Bold) },
                    supportingContent = { Text(coffee.coffee.marca) },
                    leadingContent = { AsyncImage(model = coffee.coffee.imageUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop) },
                    modifier = Modifier.clickable { viewModel.selectCoffee(coffee) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun ReviewDetailsStep(onSuccess: () -> Unit, viewModel: AddPostViewModel) {
    val selectedCoffee by viewModel.selectedCoffee.collectAsState()
    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(model = selectedCoffee?.coffee?.imageUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(selectedCoffee?.coffee?.nombre ?: "", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                Text(selectedCoffee?.coffee?.marca ?: "", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Puntuación", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        
        SemicircleRatingBar(rating = rating, onRatingChanged = { rating = it })
        
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = comment, 
            onValueChange = { comment = it }, 
            label = { Text("Tu opinión es importante...") }, 
            modifier = Modifier.fillMaxWidth().weight(1f), 
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f),
                focusedBorderColor = CoffeeBrown
            )
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = { viewModel.submitReview(rating, comment, onSuccess) }, modifier = Modifier.fillMaxWidth().height(56.dp), enabled = rating > 0 && comment.isNotBlank(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = CoffeeBrown)) {
            Text("Enviar opinión", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
