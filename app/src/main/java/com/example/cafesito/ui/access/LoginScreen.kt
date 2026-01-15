package com.example.cafesito.ui.access

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cafesito.ui.theme.CoffeeBrown
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Iniciar sesión",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = CoffeeBrown
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            CircularProgressIndicator(color = CoffeeBrown)
        } else {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(webClientId)
                                .setAutoSelectEnabled(true)
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
                                        googleIdTokenCredential.id, // email
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
                            Log.d("LoginScreen", "Selector cerrado")
                        } catch (e: NoCredentialException) {
                            Toast.makeText(context, "No hay cuentas de Google configuradas en este dispositivo", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Log.e("LoginScreen", "Error: ${e.message}", e)
                            Toast.makeText(context, "Error de Google: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF747775)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFF1F1F1F)),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = Color(0xFFF2F2F2), border = BorderStroke(0.5.dp, Color.LightGray)) {
                        Text("G", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color(0xFF4285F4), modifier = Modifier.wrapContentHeight())
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Continuar con Google", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
