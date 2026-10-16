package com.scamshield.app.services

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

object PiiFinancialBrakeGuard {

    private const val TAG = "PiiGuard"

    private val REGEX_THAI_NATIONAL_ID = Regex("\\b\\d{13}\\b")
    private val REGEX_OTP = Regex("\\b\\d{6}\\b")
    private val REGEX_SEED_PHRASE = Regex("seed\\s*phrase|recovery\\s*phrase|mnemonic|คำช่วยจำ", RegexOption.IGNORE_CASE)
    private val REGEX_PASSWORD = Regex("password|รหัสผ่าน|pin\\s*code", RegexOption.IGNORE_CASE)

    fun checkTextForPii(context: Context, text: String): Boolean {
        var triggered = false

        if (REGEX_THAI_NATIONAL_ID.containsMatchIn(text)) {
            Log.w(TAG, "PII Alert: Detected 13-digit Thai National ID attempt")
            triggerHapticAndAudioAlert(context, "13-digit Thai National ID")
            triggered = true
        } else if (REGEX_OTP.containsMatchIn(text)) {
            Log.w(TAG, "PII Alert: Detected 6-digit OTP share attempt")
            triggerHapticAndAudioAlert(context, "6-digit OTP code")
            triggered = true
        } else if (REGEX_SEED_PHRASE.containsMatchIn(text)) {
            Log.w(TAG, "PII Alert: Detected crypto seed phrase attempt")
            triggerHapticAndAudioAlert(context, "Crypto Wallet Seed Phrase")
            triggered = true
        } else if (REGEX_PASSWORD.containsMatchIn(text)) {
            Log.w(TAG, "PII Alert: Detected password / PIN share attempt")
            triggerHapticAndAudioAlert(context, "Password / PIN")
            triggered = true
        }

        return triggered
    }

    private fun triggerHapticAndAudioAlert(context: Context, piiType: String) {
        // 1. Audio Warning Chime
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Could not play alert chime: ${e.message}")
        }

        // 2. Heavy Vibration Alert (Waveform)
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Urgent SOS-style vibration waveform
                val timings = longArrayOf(0, 150, 80, 150, 80, 300)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }
}
