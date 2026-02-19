package com.cafesito.app.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class HapticSignal {
    Success,
    Error,
    Critical
}

class NativeHaptics(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun perform(signal: HapticSignal) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = when (signal) {
                HapticSignal.Success -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                HapticSignal.Error -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
                HapticSignal.Critical -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val millis = when (signal) {
                HapticSignal.Success -> 18L
                HapticSignal.Error -> 40L
                HapticSignal.Critical -> 60L
            }
            @Suppress("DEPRECATION")
            v.vibrate(millis)
        }
    }
}

@Composable
fun rememberNativeHaptics(): NativeHaptics {
    val context = LocalContext.current
    return remember(context) { NativeHaptics(context) }
}
