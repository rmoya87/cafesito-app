package com.example.cafesito.ui.access

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.components.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (googleId: String, email: String, name: String, photoUrl: String, isNewUser: Boolean) -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val credentialManager = CredentialManager.create(context)
    val scope = rememberCoroutineScope()
    val isLoading by viewModel.isLoading.collectAsState()

    val webClientId = "789398399906-468mj79uf2t4e485n7ilufv4eiouk3sm.apps.googleusercontent.com"

    Scaffold(containerColor = SoftOffWhite) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PremiumCard(modifier = Modifier.size(120.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("☕", fontSize = 60.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "CAFESITO",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = EspressoDeep,
                letterSpacing = 4.sp
            )
            Text(
                text = "TU COMUNIDAD DE CAFÉ",
                style = MaterialTheme.typography.labelLarge,
                color = CaramelAccent,
                letterSpacing = 2.sp
            )
            
            Spacer(modifier = Modifier.height(80.dp))

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
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = EspressoDeep),
                    border = BorderStroke(1.dp, BorderLight),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("CONTINUAR CON GOOGLE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
