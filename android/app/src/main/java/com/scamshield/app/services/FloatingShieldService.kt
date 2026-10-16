package com.scamshield.app.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.scamshield.app.R
import com.scamshield.app.ScamShieldApplication
import com.scamshield.app.network.SocketManager
import kotlin.math.abs

class FloatingShieldService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingBubbleView: View? = null
    private var trustCardView: View? = null
    private var transferBrakeView: View? = null

    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var isTrustCardExpanded = false

    companion object {
        const val ACTION_SHOW_CALL_WARNING = "ACTION_SHOW_CALL_WARNING"
        const val ACTION_UPDATE_RISK = "ACTION_UPDATE_RISK"
        const val ACTION_SHOW_TRANSFER_BRAKE = "ACTION_SHOW_TRANSFER_BRAKE"
        const val ACTION_HIDE_TRANSFER_BRAKE = "ACTION_HIDE_TRANSFER_BRAKE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, createForegroundNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1001, createForegroundNotification())
            }
        } catch (e: Exception) {
            // Android 14+ foreground service safe fallback
        }

        setupFloatingBubble()
        listenForRealTimeRiskUpdates()
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, ScamShieldApplication.CHANNEL_ID_PROTECTION)
            .setContentTitle("Scam Shield Active Defense")
            .setContentText("Real-time floating overlay & screen shield online")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun setupFloatingBubble() {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 300
        }

        val inflater = LayoutInflater.from(this)
        floatingBubbleView = inflater.inflate(R.layout.floating_bubble_layout, null)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        val bubbleRoot = floatingBubbleView?.findViewById<FrameLayout>(R.id.bubble_root)

        bubbleRoot?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = bubbleParams.x
                    initialY = bubbleParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    bubbleParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    bubbleParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(floatingBubbleView, bubbleParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val diffX = abs(event.rawX - initialTouchX)
                    val diffY = abs(event.rawY - initialTouchY)
                    // If touch movement is tiny, treat as a click to expand Trust Card
                    if (diffX < 10 && diffY < 10) {
                        toggleTrustCard()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            if (Settings.canDrawOverlays(this)) {
                windowManager.addView(floatingBubbleView, bubbleParams)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingShield", "Error adding floating bubble: ${e.message}")
        }
    }

    private fun listenForRealTimeRiskUpdates() {
        SocketManager.onRiskUpdateListener = { json ->
            val score = json.optInt("riskScore", 0)
            val level = json.optString("riskLevel", "safe")
            val newFlags = json.optJSONArray("newFlags")
            val flagsList = mutableListOf<String>()
            if (newFlags != null) {
                for (i in 0 until newFlags.length()) {
                    flagsList.add(newFlags.getString(i))
                }
            }

            floatingBubbleView?.post {
                updateBubbleRiskState(score, level)
                if (isTrustCardExpanded) {
                    updateTrustCardContent("Active Screen Analysis", score, level, flagsList.joinToString(", "))
                }
            }
        }
    }

    fun updateBubbleRiskState(riskScore: Int, riskLevel: String) {
        val ringView = floatingBubbleView?.findViewById<View>(R.id.bubble_ring) ?: return
        val badgeView = floatingBubbleView?.findViewById<TextView>(R.id.bubble_risk_badge) ?: return

        val color = when {
            riskScore >= 70 || riskLevel == "critical" -> Color.parseColor("#EF4444") // RED
            riskScore >= 40 || riskLevel == "suspicious" -> Color.parseColor("#F97316") // ORANGE
            riskScore >= 15 || riskLevel == "caution" -> Color.parseColor("#EAB308") // YELLOW
            else -> Color.parseColor("#22C55E") // GREEN
        }

        (ringView.background as? GradientDrawable)?.setColor(color)
        badgeView.text = "$riskScore%"
        badgeView.visibility = if (riskScore > 0) View.VISIBLE else View.GONE
    }

    @SuppressLint("InflateParams")
    private fun toggleTrustCard() {
        if (isTrustCardExpanded) {
            hideTrustCard()
        } else {
            showTrustCard()
        }
    }

    private fun showTrustCard() {
        if (trustCardView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val cardParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val inflater = LayoutInflater.from(this)
        trustCardView = inflater.inflate(R.layout.trust_card_overlay, null)

        trustCardView?.findViewById<Button>(R.id.btn_dismiss_card)?.setOnClickListener {
            hideTrustCard()
        }

        trustCardView?.findViewById<Button>(R.id.btn_ocr_scan)?.setOnClickListener {
            // Trigger MediaProjection visual OCR fallback
            val ocrIntent = Intent(this, MediaProjectionOcrService::class.java).apply {
                action = MediaProjectionOcrService.ACTION_CAPTURE_SCREEN
            }
            startService(ocrIntent)
            hideTrustCard()
        }

        try {
            if (Settings.canDrawOverlays(this)) {
                windowManager.addView(trustCardView, cardParams)
                isTrustCardExpanded = true
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingShield", "Error adding trust card: ${e.message}")
        }
    }

    private fun hideTrustCard() {
        trustCardView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            trustCardView = null
        }
        isTrustCardExpanded = false
    }

    private fun updateTrustCardContent(title: String, score: Int, level: String, flags: String) {
        trustCardView?.let { view ->
            view.findViewById<TextView>(R.id.trust_card_title)?.text = title
            val badge = view.findViewById<TextView>(R.id.trust_card_risk_badge)
            badge?.text = "${level.uppercase()} $score%"
            view.findViewById<TextView>(R.id.trust_card_flags)?.text = "🚩 $flags"
        }
    }

    // ── Mobile Banking Transfer Brake Overlay ─────────────────────────────────
    @SuppressLint("InflateParams")
    fun showTransferBrake(muleAccount: String?, companyName: String?) {
        if (transferBrakeView != null) return

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val brakeParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        val inflater = LayoutInflater.from(this)
        transferBrakeView = inflater.inflate(R.layout.transfer_brake_overlay, null)

        val msgView = transferBrakeView?.findViewById<TextView>(R.id.transfer_brake_message)
        if (muleAccount != null) {
            msgView?.text = "You switched into Mobile Banking to transfer to: $muleAccount ($companyName). This account is flagged as a known MULE ACCOUNT (บัญชีม้า)!"
        }

        transferBrakeView?.findViewById<Button>(R.id.btn_cancel_transfer)?.setOnClickListener {
            hideTransferBrake()
        }

        transferBrakeView?.findViewById<Button>(R.id.btn_override_brake)?.setOnClickListener {
            hideTransferBrake()
        }

        try {
            if (Settings.canDrawOverlays(this)) {
                windowManager.addView(transferBrakeView, brakeParams)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingShield", "Error adding transfer brake: ${e.message}")
        }
    }

    fun hideTransferBrake() {
        transferBrakeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
            transferBrakeView = null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_RISK -> {
                val score = intent.getIntExtra("riskScore", 0)
                val level = intent.getStringExtra("riskLevel") ?: "safe"
                updateBubbleRiskState(score, level)
            }
            ACTION_SHOW_TRANSFER_BRAKE -> {
                val account = intent.getStringExtra("muleAccount")
                val company = intent.getStringExtra("companyName")
                showTransferBrake(account, company)
            }
            ACTION_HIDE_TRANSFER_BRAKE -> {
                hideTransferBrake()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingBubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {}
        }
        hideTrustCard()
        hideTransferBrake()
    }
}
