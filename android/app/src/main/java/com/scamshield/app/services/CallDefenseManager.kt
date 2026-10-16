package com.scamshield.app.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import android.util.Log
import com.scamshield.app.network.CallSessionStartRequest
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SocketManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object CallDefenseManager {

    private const val TAG = "CallDefenseManager"
    // Cloud PBX bridge phone number (SIP / WebRTC listener bridge line)
    const val CLOUD_PBX_BRIDGE_NUMBER = "+6620001207"

    var currentSessionId: String? = null
        private set

    @SuppressLint("MissingPermission")
    fun activateCallShieldAndMerge(context: Context, callerNumber: String?) {
        val deviceId = SocketManager.getDeviceId()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Register ephemeral call session on the backend
                val response = RetrofitClient.api.startCallSession(
                    CallSessionStartRequest(deviceId = deviceId, callerNumber = callerNumber)
                )

                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    currentSessionId = result.sessionId
                    Log.i(TAG, "Initialized 3-Way Call Session on backend: ${result.sessionId}")

                    // 2. Automatically dial the Scam Shield Cloud PBX conference bot as line 2
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$CLOUD_PBX_BRIDGE_NUMBER")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(callIntent)

                    // 3. Update floating bubble into 3-way monitoring state
                    val updateIntent = Intent(context, FloatingShieldService::class.java).apply {
                        action = FloatingShieldService.ACTION_UPDATE_RISK
                        putExtra("riskScore", result.callerRisk.riskScore)
                        putExtra("riskLevel", result.callerRisk.action)
                    }
                    context.startService(updateIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start 3-way call session: ${e.message}")
            }
        }
    }

    fun disconnectCallShield(context: Context) {
        val sessionId = currentSessionId ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Wipe ephemeral session from RAM
                RetrofitClient.api.endCallSession(mapOf("sessionId" to sessionId))
                Log.i(TAG, "Session $sessionId successfully wiped from Cloud PBX RAM")
                currentSessionId = null
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call session: ${e.message}")
            }
        }
    }
}
