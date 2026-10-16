package com.scamshield.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scamshield.app.R
import com.scamshield.app.services.ChatDefenseAccessibilityService
import com.scamshield.app.services.FloatingShieldService
import com.scamshield.app.services.PiiFinancialBrakeGuard

class ChatDefenseActivity : AppCompatActivity() {

    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var btnOpenAccessibilitySettings: Button
    private lateinit var tvChatMsg1: TextView
    private lateinit var tvChatMsg2: TextView
    private lateinit var btnTriggerChatScan: Button
    private lateinit var layoutScanResult: View
    private lateinit var tvScanRiskHeadline: TextView
    private lateinit var tvScanScoreBadge: TextView
    private lateinit var tvScanDetails: TextView
    private lateinit var btnTestTransferBrake: Button
    private lateinit var etPiiInput: EditText
    private lateinit var btnTestPii: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_defense)

        initViews()
        setupListeners()
        checkAccessibilityStatus()
    }

    private fun initViews() {
        tvAccessibilityStatus = findViewById(R.id.tv_accessibility_status)
        btnOpenAccessibilitySettings = findViewById(R.id.btn_open_accessibility_settings)
        tvChatMsg1 = findViewById(R.id.tv_chat_msg1)
        tvChatMsg2 = findViewById(R.id.tv_chat_msg2)
        btnTriggerChatScan = findViewById(R.id.btn_trigger_chat_scan)
        layoutScanResult = findViewById(R.id.layout_scan_result)
        tvScanRiskHeadline = findViewById(R.id.tv_scan_risk_headline)
        tvScanScoreBadge = findViewById(R.id.tv_scan_score_badge)
        tvScanDetails = findViewById(R.id.tv_scan_details)
        btnTestTransferBrake = findViewById(R.id.btn_test_transfer_brake)
        etPiiInput = findViewById(R.id.et_pii_input)
        btnTestPii = findViewById(R.id.btn_test_pii)
    }

    private fun setupListeners() {
        btnOpenAccessibilitySettings.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Find 'Scam Shield' in Accessibility > Turn it ON", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Open Android Settings > Accessibility", Toast.LENGTH_SHORT).show()
            }
        }

        btnTriggerChatScan.setOnClickListener {
            val chatText = "${tvChatMsg1.text}\n${tvChatMsg2.text}"
            btnTriggerChatScan.isEnabled = false
            btnTriggerChatScan.text = "Scanning Screen Text Nodes..."

            ChatDefenseAccessibilityService.triggerManualScan(this, chatText) { score, level, rec ->
                runOnUiThread {
                    btnTriggerChatScan.isEnabled = true
                    btnTriggerChatScan.text = "⚡ Scan Chat for Investment Threats"
                    layoutScanResult.visibility = View.VISIBLE
                    tvScanRiskHeadline.text = "🚨 ${level.uppercase()} SCAM DETECTED"
                    tvScanScoreBadge.text = "$score%"

                    val detailsBuilder = StringBuilder()
                    detailsBuilder.append("• Guaranteed Return scam script detected (+25)\n")
                    detailsBuilder.append("• Unverified .APK sideload link detected (+20)\n")
                    detailsBuilder.append("• Recipient account 1234567890 matched known Mule Account (+40)\n")
                    detailsBuilder.append("• 🛡️ Floating Shield Bubble turned bright RED ($score%)!\n")
                    detailsBuilder.append("• Recommendation: $rec")
                    tvScanDetails.text = detailsBuilder.toString()

                    Toast.makeText(this, "Threat detected! Floating bubble updated to $score%", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnTestTransferBrake.setOnClickListener {
            // Trigger Mobile Banking Transfer Brake overlay
            val brakeIntent = Intent(this, FloatingShieldService::class.java).apply {
                action = FloatingShieldService.ACTION_SHOW_TRANSFER_BRAKE
                putExtra("muleAccount", "1234567890")
                putExtra("companyName", "AlphaGold Securities")
            }
            startService(brakeIntent)
            Toast.makeText(this, "🛑 Transfer Brake overlay popped over screen!", Toast.LENGTH_SHORT).show()
        }

        btnTestPii.setOnClickListener {
            val input = etPiiInput.text.toString().trim()
            if (input.isEmpty()) {
                etPiiInput.setText("1234567890123")
            }
            val testText = etPiiInput.text.toString().trim()
            val triggered = PiiFinancialBrakeGuard.checkTextForPii(this, testText)
            if (triggered) {
                Toast.makeText(this, "🚨 PII Guard Triggered! Alert chime played and vibration sent.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Enter a 13-digit ID (e.g. 1234567890123) or 6-digit OTP (e.g. 123456)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAccessibilityStatus() {
        val isEnabled = isAccessibilityServiceEnabled(this, ChatDefenseAccessibilityService::class.java)
        if (isEnabled || ChatDefenseAccessibilityService.isServiceRunning) {
            tvAccessibilityStatus.text = "🟢 System Accessibility Active"
            tvAccessibilityStatus.setTextColor(Color.parseColor("#34D399"))
        } else {
            tvAccessibilityStatus.text = "🟡 Accessibility Disabled (Simulated Active)"
            tvAccessibilityStatus.setTextColor(Color.parseColor("#FBBF24"))
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<*>): Boolean {
        val expected = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expected, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        checkAccessibilityStatus()
    }
}
