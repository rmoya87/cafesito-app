package com.example.cafesito.ui.access

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.cafesito.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (googleId: String, email: String, name: String, photoUrl: String, isNewUser: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)
    val scope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()

    // Configuración de ExoPlayer para el video de fondo
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/raw/login_bg")
            setMediaItem(MediaItem.fromUri(videoUri))
            // ✅ Se desactiva el bucle para que se detenga al finalizar
            repeatMode = Player.REPEAT_MODE_OFF 
            volume = 0f // Silenciado
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val webClientId = "789398399906-468mj79uf2t4e485n7ilufv4eiouk3sm.apps.googleusercontent.com"

    Box(modifier = Modifier.fillMaxSize().background(SoftOffWhite)) {
        
        // Video de fondo ocupando toda la pantalla y centrado automáticamente (RESIZE_MODE_ZOOM)
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Gradiente superior MUCHO más corto para que el video sea visible casi desde arriba
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to SoftOffWhite,
                        0.05f to SoftOffWhite,
                        0.45f to Color.Transparent
                    )
                )
        )
        
        // Gradiente inferior para legibilidad del texto y el botón
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.6f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.8f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Marca superior
            Text(
                text = "CAFESITO",
                style = MaterialTheme.typography.titleMedium,
                color = EspressoDeep.copy(alpha = 0.7f),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Espaciador con peso para liberar el centro de la pantalla (donde está la mano/café)
            Spacer(modifier = Modifier.weight(2.2f))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "Tu viaje sensorial\ncomienza aquí.",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = androidx.compose.ui.geometry.Offset(2f, 4f),
                            blurRadius = 8f
                        )
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Únete a la comunidad de café para descubrir, elaborar y compartir tu pasión.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = CaramelAccent)
                } else {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(webClientId)
                                        .setAutoSelectEnabled(false)
                                        .build()

                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)
                                        .build()

                                    val result = credentialManager.getCredential(context = context, request = request)
                                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                                    
                                    viewModel.handleGoogleIdToken(
                                        idToken = googleIdTokenCredential.idToken,
                                        onSuccess = { supabaseUuid, isNewUser ->
                                            onLoginSuccess(
                                                supabaseUuid, 
                                                googleIdTokenCredential.id, 
                                                googleIdTokenCredential.displayName ?: "",
                                                googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                                                isNewUser
                                            )
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } catch (e: GetCredentialCancellationException) {
                                    Log.d("LoginScreen", "Cerrado")
                                } catch (e: NoCredentialException) {
                                    Toast.makeText(context, "Inicia sesión en Google primero", Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Error: ${e.message}", e)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = EspressoDeep
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "G",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = EspressoDeep,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                "Continuar con Google",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Al continuar, aceptas nuestros Términos y Condiciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
