package com.cafesito.app.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.cafesito.app.ui.theme.CafesitoTheme
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class NativeBarcodeScannerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BARCODE_VALUE = "barcode_value"
    }

    private val hasPermission = mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (!granted) finishWithCancel()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasPermission.value = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission.value) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            CafesitoTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    if (hasPermission.value) {
                        NativeBarcodeScannerContent(
                            onDetected = { value -> finishWithResult(value) },
                            onCancel = { finishWithCancel() }
                        )
                    } else {
                        PermissionWaiting(onCancel = { finishWithCancel() })
                    }
                }
            }
        }
    }

    private fun finishWithResult(value: String) {
        setResult(Activity.RESULT_OK, Intent().putExtra(EXTRA_BARCODE_VALUE, value))
        finish()
    }

    private fun finishWithCancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }
}

@Composable
private fun PermissionWaiting(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Se necesita permiso de cámara para escanear códigos.")
        Button(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
            Text("Cerrar")
        }
    }
}

@Composable
private fun NativeBarcodeScannerContent(
    onDetected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A)
            .build()
        BarcodeScanning.getClient(options)
    }
    val consumed = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(cameraExecutor) { proxy ->
            val mediaImage = proxy.image
            if (mediaImage == null || consumed.value) {
                proxy.close()
                return@setAnalyzer
            }
            val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { codes ->
                    val value = codes.firstOrNull { it.rawValue != null }?.rawValue
                    if (!value.isNullOrBlank() && !consumed.value) {
                        consumed.value = true
                        onDetected(value)
                    }
                }
                .addOnCompleteListener { proxy.close() }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            context as ComponentActivity,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.weight(1f))
        Button(onClick = onCancel, modifier = Modifier.padding(16.dp)) {
            Text("Cancelar")
        }
    }
}
