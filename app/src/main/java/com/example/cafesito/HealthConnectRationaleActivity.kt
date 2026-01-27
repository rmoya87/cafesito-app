package com.example.cafesito

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cafesito.ui.theme.CafesitoTheme
import com.example.cafesito.ui.theme.EspressoDeep

class HealthConnectRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafesitoTheme {
                RationaleScreen(
                    onOpenPrivacyPolicy = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://cafesito.app/privacy"))
                        startActivity(intent)
                    },
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun RationaleScreen(onOpenPrivacyPolicy: () -> Unit, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Health Connect & Cafesito",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = EspressoDeep
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Sincronizamos tus datos de consumo de cafeína (Nutrición) e hidratación con Health Connect para darte una visión completa de tu salud.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onOpenPrivacyPolicy) {
                Text("VER POLÍTICA DE PRIVACIDAD")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onClose) {
                Text("CERRAR")
            }
        }
    }
}
