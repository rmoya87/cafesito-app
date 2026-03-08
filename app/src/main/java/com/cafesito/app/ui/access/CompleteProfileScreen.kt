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

@Composable
fun CompleteProfileScreen(
    googleId: String,
    userEmail: String,
    initialName: String,
    initialPhoto: String,
    onSuccess: () -> Unit,
    viewModel: CompleteProfileViewModel = hiltViewModel()
) {
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
                text = "Casi listo", 
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
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Añadir foto de perfil", Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = { launcher.launch("image/*") }) {
                Text("Cambiar foto", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Email (Solo lectura)
            OutlinedTextField(
                value = userEmail,
                onValueChange = {},
                label = { Text("Email de Google") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(12.dp),
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
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(text = usernameError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Sin espacios y todo en minúsculas")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Cuéntanos sobre ti") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                shape = RoundedCornerShape(16.dp),
                enabled = username.isNotBlank() && usernameError == null
            ) {
                Text("Finalizar Perfil", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
