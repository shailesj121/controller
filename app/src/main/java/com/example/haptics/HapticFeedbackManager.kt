package com.example.haptics

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var isEnabled: Boolean = true

    fun click() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(15)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    fun tick() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(8)
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    fun heavyClick() {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun rumble(durationMs: Long = 120, intensity: Float = 0.7f) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            val clampedDuration = durationMs.coerceIn(20, 1000)
            val amplitude = (intensity.coerceIn(0.1f, 1.0f) * 255).toInt().coerceIn(1, 255)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(clampedDuration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(clampedDuration)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun accelerationPulse(progress: Float) {
        if (!isEnabled || vibrator == null || !vibrator.hasVibrator()) return
        try {
            val clamped = progress.coerceIn(0f, 1f)
            // Subtle, non-intrusive amplitude scaling from 35 (gentle idle purr) to 80 (light rev humming)
            val amplitude = (35 + clamped * 45).toInt().coerceIn(1, 255)
            val duration = (10 + clamped * 8).toLong() // 10ms to 18ms crisp micro-pulse
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun stop() {
        if (vibrator == null || !vibrator.hasVibrator()) return
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
