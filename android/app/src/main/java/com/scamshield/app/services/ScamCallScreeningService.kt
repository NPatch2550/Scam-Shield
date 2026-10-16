package com.scamshield.app.services

import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.annotation.RequiresApi
import com.scamshield.app.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.N)
class ScamCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "CallScreeningService"
        // In-memory high-speed cache of verified scam numbers
        private val localScamCache = hashSetOf(
            "0891234567", // AlphaGold Cold-Call Line
            "0987654321", // TaskMaster Earn Recruiter
            "0812345678"  // Prestige Capital VIP Line
        )
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val handle = callDetails.handle
        val rawNumber = handle?.schemeSpecificPart ?: return
        val normalizedNumber = normalizePhoneNumber(rawNumber)

        Log.d(TAG, "Intercepted incoming call before ringing: $normalizedNumber")

        // 1. Fast local cache lookup (sub-millisecond)
        if (localScamCache.contains(normalizedNumber)) {
            Log.w(TAG, "Fast cache HIT: Blocking scam number $normalizedNumber")
            respondToScamCall(callDetails, reason = "Known investment scam broker")
            return
        }

        // 2. Query Scam Shield backend in background coroutine
        serviceScope.launch {
            try {
                val response = RetrofitClient.api.screenCall(normalizedNumber)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    withContext(Dispatchers.Main) {
                        if (result.action == "REJECT" || result.riskScore >= 70) {
                            Log.w(TAG, "Backend verified scam call: Rejecting $normalizedNumber (${result.reason})")
                            respondToScamCall(callDetails, result.reason ?: "Unlicensed financial cold-caller")
                        } else {
                            Log.i(TAG, "Number screened as safe/low-risk. Allowing call.")
                            respondToSafeCall(callDetails)
                        }
                    }
                } else {
                    respondToSafeCall(callDetails)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Screening lookup failed: ${e.message}. Defaulting to allow.")
                respondToSafeCall(callDetails)
            }
        }
    }

    private fun respondToScamCall(callDetails: Call.Details, reason: String) {
        val response = CallResponse.Builder()
            .setDisallowCall(true)       // Reject incoming call
            .setRejectCall(true)         // Hang up immediately
            .setSkipCallLog(false)       // Keep record in call log for user awareness
            .setSkipNotification(false)  // Show missed call warning
            .build()

        respondToCall(callDetails, response)

        // Broadcast warning to FloatingShieldService or notification
        val intent = Intent(this, FloatingShieldService::class.java).apply {
            action = FloatingShieldService.ACTION_SHOW_CALL_WARNING
            putExtra("number", callDetails.handle?.schemeSpecificPart)
            putExtra("reason", reason)
            putExtra("riskScore", 100)
        }
        startService(intent)
    }

    private fun respondToSafeCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .build()
        respondToCall(callDetails, response)
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace("+66", "0")
            .replace("[^0-9]".toRegex(), "")
    }
}
