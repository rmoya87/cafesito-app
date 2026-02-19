package com.cafesito.app.ui.access

import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.cafesito.app.R
import com.cafesito.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector
import com.cafesito.app.ui.theme.CaramelAccent
import com.cafesito.app.platform.HapticSignal
import com.cafesito.app.platform.rememberNativeHaptics
import androidx.compose.material3.ExperimentalMaterial3Api

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (googleId: String, email: String, name: String, photoUrl: String, isNewUser: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    val loginScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val isLoading by viewModel.isLoading.collectAsState()
    var showLoginModal by remember { mutableStateOf(false) }
    val haptics = rememberNativeHaptics()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoUri = Uri.parse("android.resource://${context.packageName}/${R.raw.onboarding_bg}")
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
            loginScope.cancel()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to MaterialTheme.colorScheme.background,
                    0.05f to MaterialTheme.colorScheme.background,
                    0.45f to Color.Transparent
                )
            )
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.2f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.8f)
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text(stringResource(R.string.login_welcome_to), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Text(stringResource(R.string.login_brand), style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, lineHeight = 44.sp, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.login_subtitle), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), lineHeight = 26.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                FeatureRowUnified(icon = Icons.Default.CameraAlt, title = stringResource(R.string.login_feature_share_title), desc = stringResource(R.string.login_feature_share_desc))
                FeatureRowUnified(icon = Icons.Default.Coffee, title = stringResource(R.string.login_feature_explore_title), desc = stringResource(R.string.login_feature_explore_desc))
                FeatureRowUnified(icon = Icons.Default.Science, title = stringResource(R.string.login_feature_brew_title), desc = stringResource(R.string.login_feature_brew_desc))
                FeatureRowUnified(icon = Icons.Default.AutoGraph, title = stringResource(R.string.login_feature_track_title), desc = stringResource(R.string.login_feature_track_desc))
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = { showLoginModal = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(stringResource(R.string.login_start_now), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showLoginModal) {
        ModalBottomSheet(
            onDismissRequest = { showLoginModal = false },
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.login_sheet_description),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (isLoading) {
                    CircularProgressIndicator(color = CaramelAccent)
                } else {
                    Button(
                        onClick = {
                            loginScope.launch {
                                try {
                                    val googleIdOption = GetGoogleIdOption.Builder()
                                        .setFilterByAuthorizedAccounts(false)
                                        .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                                        .setAutoSelectEnabled(false)
                                        .build()

                                    val requestBuilder = GetCredentialRequest.Builder()
                                        .addCredentialOption(googleIdOption)

                                    if (BuildConfig.PASSKEY_REQUEST_JSON.isNotBlank()) {
                                        requestBuilder.addCredentialOption(
                                            GetPublicKeyCredentialOption(BuildConfig.PASSKEY_REQUEST_JSON)
                                        )
                                    }

                                    val request = requestBuilder.build()
                                    val result = credentialManager.getCredential(context = context, request = request)

                                    when (val credential = result.credential) {
                                        is PublicKeyCredential -> {
                                            haptics.perform(HapticSignal.Success)
                                            Toast.makeText(context, "Passkey validada. Completando inicio de sesión…", Toast.LENGTH_LONG).show()

                                            val fallbackGoogleRequest = GetCredentialRequest.Builder()
                                                .addCredentialOption(
                                                    GetGoogleIdOption.Builder()
                                                        .setFilterByAuthorizedAccounts(false)
                                                        .setServerClientId(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                                                        .setAutoSelectEnabled(true)
                                                        .build()
                                                )
                                                .build()

                                            val fallbackResult = credentialManager.getCredential(
                                                context = context,
                                                request = fallbackGoogleRequest
                                            )
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(fallbackResult.credential.data)
                                            viewModel.handleGoogleIdToken(
                                                idToken = googleIdTokenCredential.idToken,
                                                onSuccess = { supabaseUuid, isNewUser ->
                                                    haptics.perform(HapticSignal.Success)
                                                    onLoginSuccess(
                                                        supabaseUuid,
                                                        googleIdTokenCredential.id,
                                                        googleIdTokenCredential.displayName ?: "",
                                                        googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                                                        isNewUser
                                                    )
                                                },
                                                onError = { error ->
                                                    haptics.perform(HapticSignal.Error)
                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                        else -> {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            viewModel.handleGoogleIdToken(
                                                idToken = googleIdTokenCredential.idToken,
                                                onSuccess = { supabaseUuid, isNewUser ->
                                                    haptics.perform(HapticSignal.Success)
                                                    onLoginSuccess(
                                                        supabaseUuid,
                                                        googleIdTokenCredential.id,
                                                        googleIdTokenCredential.displayName ?: "",
                                                        googleIdTokenCredential.profilePictureUri?.toString() ?: "",
                                                        isNewUser
                                                    )
                                                },
                                                onError = { error ->
                                                    haptics.perform(HapticSignal.Error)
                                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    }
                                } catch (e: GetCredentialCancellationException) {
                                    Log.d("LoginScreen", "Cerrado")
                                } catch (e: NoCredentialException) {
                                    haptics.perform(HapticSignal.Error)
                                    Toast.makeText(context, context.getString(R.string.login_no_google_accounts), Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Error: ${e.message}", e)
                                    haptics.perform(HapticSignal.Critical)
                                    Toast.makeText(context, context.getString(R.string.login_connection_error, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("G", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 12.dp))
                            Text(stringResource(R.string.login_continue_with_google), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.login_terms_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun FeatureRowUnified(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
        }
    }
}
