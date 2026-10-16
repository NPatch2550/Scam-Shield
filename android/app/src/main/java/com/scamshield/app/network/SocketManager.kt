package com.scamshield.app.network

import android.content.Context
import android.provider.Settings
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

object SocketManager {

    private const val TAG = "SocketManager"
    private var socket: Socket? = null
    private var deviceId: String = "android_device_default"

    var onRiskUpdateListener: ((JSONObject) -> Unit)? = null

    fun init(context: Context) {
        try {
            deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "android_device_001"
            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 10
                reconnectionDelay = 2000
            }

            // Using 10.0.2.2 for default Android emulator networking to host machine port 3000
            socket = IO.socket(RetrofitClient.baseUrl, options)

            socket?.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Connected to Scam Shield Socket.io server")
                // Register device ID for targeted risk alerts
                val regData = JSONObject().apply {
                    put("deviceId", deviceId)
                }
                socket?.emit("register", regData)
            }

            socket?.on("registered") { args ->
                Log.d(TAG, "Device registered: ${args.getOrNull(0)}")
            }

            socket?.on("risk:update") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    Log.d(TAG, "Received real-time risk update: $data")
                    onRiskUpdateListener?.invoke(data)
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                Log.d(TAG, "Disconnected from Scam Shield Socket.io server")
            }

            socket?.connect()

        } catch (e: URISyntaxException) {
            Log.e(TAG, "Failed to initialize Socket.io client: ${e.message}")
        }
    }

    fun getDeviceId(): String = deviceId

    fun disconnect() {
        socket?.disconnect()
    }
}
