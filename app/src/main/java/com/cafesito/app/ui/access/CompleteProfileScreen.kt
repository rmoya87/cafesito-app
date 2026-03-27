package com.cafesito.app.ui.access

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.cafesito.app.ui.theme.CoffeeBrown
import com.cafesito.app.ui.theme.Shapes
import java.util.Locale

@Composable
fun CompleteProfileScreen(
    googleId: String,
    userEmail: String,
    initialName: String,
    initialPhoto: String,
    onSuccess: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel()
) {
    val isSpanish = remember { Locale.getDefault().language.startsWith("es") }
    var username by remember { mutableStateOf(initialName.replace(" ", "").lowercase()) }
    var bio by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(if (initialPhoto.isNotEmpty()) Uri.parse(initialPhoto) else null) }
    
    val usernameError by viewModel.usernameError.collectAsState()
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { imageUri = it }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Text(
                text = if (isSpanish) "Casi listo" else "Almost there",
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Foto de Perfil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = if (isSpanish) "Foto de perfil" else "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.PhotoCamera, contentDescription = if (isSpanish) "Añadir foto de perfil" else "Add profile photo", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = { launcher.launch("image/*") }) {
                Text(if (isSpanish) "Cambiar foto" else "Change photo", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email (Solo lectura)
            OutlinedTextField(
                value = userEmail,
                onValueChange = {},
                label = { Text(if (isSpanish) "Email de Google" else "Google email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = Shapes.cardSmall,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre de usuario (REGLA: Sin espacios y Único)
            OutlinedTextField(
                value = username,
                onValueChange = { 
                    username = it.replace(" ", "").lowercase()
                    viewModel.clearError() 
                },
                label = { Text(if (isSpanish) "Nombre de usuario" else "Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.cardSmall,
                singleLine = true,
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(text = usernameError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(if (isSpanish) "Sin espacios y todo en minúsculas" else "No spaces, lowercase only")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text(if (isSpanish) "Cuéntanos sobre ti" else "Tell us about yourself") },
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.cardSmall,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { 
                    viewModel.saveUserProfile(
                        googleId = googleId,
                        email = userEmail,
                        username = username,
                        bio = bio,
                        avatarUrl = imageUri?.toString() ?: "",
                        onSuccess = onSuccess
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = Shapes.card,
                enabled = username.isNotBlank() && usernameError == null
            ) {
                Text(if (isSpanish) "Finalizar Perfil" else "Finish profile", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
