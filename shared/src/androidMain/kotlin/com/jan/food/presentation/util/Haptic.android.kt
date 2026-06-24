package com.jan.food.presentation.util

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.annotation.RequiresPermission

actual class Haptic(context: Context) {

    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @RequiresPermission(Manifest.permission.VIBRATE)
    @Suppress("DEPRECATION")
    private fun vibrateOneShot(milliseconds: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, amplitude))
        } else {
            vibrator.vibrate(milliseconds)
        }
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    actual fun performLightImpact() {
        vibrateOneShot(20, 100)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    actual fun performMediumImpact() {
        vibrateOneShot(20, 175)
    }

    @RequiresPermission(Manifest.permission.VIBRATE)
    actual fun performHeavyImpact() {
        vibrateOneShot(20, 255)
    }
}
