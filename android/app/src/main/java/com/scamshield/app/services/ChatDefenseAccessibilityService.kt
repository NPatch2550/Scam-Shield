package com.scamshield.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SearchRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatDefenseAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var lastFlaggedChatTime: Long = 0
    private var lastFlaggedAccount: String? = null
    private var lastFlaggedEntity: String? = null

    companion object {
        private const val TAG = "ChatDefenseService"

        // Banking application packages to monitor for the Financial Transfer Brake
        private val BANKING_PACKAGES = hashSetOf(
            "com.kasikorn.retail.mbanking", // K PLUS (KasikornBank)
            "com.scb.phone",                // SCB EASY (Siam Commercial Bank)
            "ktbcs.netbank",                // Krungthai NEXT
            "com.bbl.mobilebanking",        // Bangkok Bank Mobile Banking
            "com.tmb.mbanking"              // ttb touch
        )

        // Messaging app packages monitored for Fast Node Reading
        private val CHAT_PACKAGES = hashSetOf(
            "jp.naver.line.android",        // LINE OpenChat / Direct
            "org.telegram.messenger",       // Telegram
            "com.instagram.android",        // Instagram Direct
            "com.facebook.orca",            // Messenger
            "com.whatsapp",                 // WhatsApp
            "com.android.chrome",           // Web Browsers
            "com.scamshield.app"            // In-app Chat Defense Simulator
        )

        // Local Regex Engine (Zero-latency On-Device Extraction)
        val REGEX_BANK_ACCOUNT = Regex("\\b\\d{10,15}\\b")
        val REGEX_URL = Regex("https?://[^\\s,<>\"'()]+")
        val REGEX_APK_LINK = Regex("https?://[^\\s]+\\.apk", RegexOption.IGNORE_CASE)
        val REGEX_THAI_SCAM_KEYWORDS = Regex(
            "รับรองกำไร|การันตีผลตอบแทน|ไม่มีความเสี่ยง|กำไร\\s*\\d+%" +
            "|กลุ่ม\\s*VIP|หุ้นเด็ด|หลอกให้รัก|งาน\\s*online|ทำงานออนไลน์|ค่าภาษีก่อนถอน" +
            "|Guaranteed\\s*return|zero\\s*risk|VIP\\s*group|insider\\s*trading",
            RegexOption.IGNORE_CASE
        )

        var isServiceRunning = false

        fun triggerManualScan(context: Context, text: String, onResult: ((Int, String, String) -> Unit)? = null) {
            // Check for PII leaks
            PiiFinancialBrakeGuard.checkTextForPii(context, text)

            val hasScamScript = REGEX_THAI_SCAM_KEYWORDS.containsMatchIn(text)
            val bankAccountMatch = REGEX_BANK_ACCOUNT.find(text)?.value
            val apkMatch = REGEX_APK_LINK.find(text)?.value
            val urlMatch = REGEX_URL.find(text)?.value

            val queryParam = apkMatch ?: urlMatch ?: bankAccountMatch ?: "AlphaGold Securities"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.api.search(SearchRequest(query = queryParam, rawText = text))
                    if (response.isSuccessful && response.body() != null) {
                        val scorecard = response.body()!!

                        // Update floating bubble
                        val updateIntent = Intent(context, FloatingShieldService::class.java).apply {
                            action = FloatingShieldService.ACTION_UPDATE_RISK
                            putExtra("riskScore", scorecard.riskScore)
                            putExtra("riskLevel", scorecard.verdict)
                        }
                        context.startService(updateIntent)

                        onResult?.invoke(scorecard.riskScore, scorecard.verdict, scorecard.recommendation?.message ?: "")
                    }
                } catch (e: Exception) {
                    Log.e("ChatDefenseService", "Manual scan failed: ${e.message}")
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        Log.i(TAG, "ChatDefenseAccessibilityService connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // ── 1. Check for Mobile Banking Switch (Transfer Brake Trigger) ──────
        if (BANKING_PACKAGES.contains(packageName)) {
            val timeSinceFlaggedChat = System.currentTimeMillis() - lastFlaggedChatTime
            // If user switched to mobile banking within 5 minutes of viewing a flagged chat
            if (timeSinceFlaggedChat < 300_000 && lastFlaggedAccount != null) {
                Log.w(TAG, "Transfer Brake Triggered! User switched into $packageName after viewing flagged account: $lastFlaggedAccount")
                triggerTransferBrake(lastFlaggedAccount, lastFlaggedEntity)
            }
            return
        }

        // ── 2. Fast Node Reading in Messaging Apps & Browsers ─────────────────
        if (CHAT_PACKAGES.contains(packageName) || packageName.contains("line") || packageName.contains("telegram") || packageName.contains("chat") || packageName == applicationContext.packageName) {
            val rootNode = rootInActiveWindow ?: return
            val textBuilder = StringBuilder()
            extractTextRecursively(rootNode, textBuilder)
            val fullText = textBuilder.toString()

            if (fullText.isNotBlank()) {
                analyzeExtractedScreenText(fullText)
            }
        }
    }

    private fun extractTextRecursively(node: AccessibilityNodeInfo?, builder: StringBuilder) {
        if (node == null) return

        if (!node.text.isNullOrBlank()) {
            builder.append(node.text).append(" ")
        }
        if (!node.contentDescription.isNullOrBlank()) {
            builder.append(node.contentDescription).append(" ")
        }

        for (i in 0 until node.childCount) {
            extractTextRecursively(node.getChild(i), builder)
        }
    }

    private fun analyzeExtractedScreenText(text: String) {
        // 1. Run local on-device regex
        val hasScamScript = REGEX_THAI_SCAM_KEYWORDS.containsMatchIn(text)
        val bankAccountMatch = REGEX_BANK_ACCOUNT.find(text)?.value
        val apkMatch = REGEX_APK_LINK.find(text)?.value
        val urlMatch = REGEX_URL.find(text)?.value

        // Check for PII leaks (13-digit national ID, OTP)
        PiiFinancialBrakeGuard.checkTextForPii(this, text)

        if (hasScamScript || apkMatch != null || bankAccountMatch != null) {
            Log.d(TAG, "On-device regex match: scamScript=$hasScamScript, apk=$apkMatch, acc=$bankAccountMatch")

            if (bankAccountMatch != null) {
                lastFlaggedAccount = bankAccountMatch
                lastFlaggedChatTime = System.currentTimeMillis()
            }

            // 2. Query backend for deep corporate verification & risk scoring
            serviceScope.launch {
                try {
                    val queryParam = apkMatch ?: urlMatch ?: bankAccountMatch
                    val response = RetrofitClient.api.search(SearchRequest(query = queryParam, rawText = text))
                    if (response.isSuccessful && response.body() != null) {
                        val scorecard = response.body()!!
                        lastFlaggedEntity = scorecard.query

                        // Send intent to update Floating Bubble color and score
                        val updateIntent = Intent(this@ChatDefenseAccessibilityService, FloatingShieldService::class.java).apply {
                            action = FloatingShieldService.ACTION_UPDATE_RISK
                            putExtra("riskScore", scorecard.riskScore)
                            putExtra("riskLevel", scorecard.verdict)
                        }
                        startService(updateIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Backend analysis call failed: ${e.message}")
                }
            }
        }
    }

    private fun triggerTransferBrake(muleAccount: String?, company: String?) {
        val brakeIntent = Intent(this, FloatingShieldService::class.java).apply {
            action = FloatingShieldService.ACTION_SHOW_TRANSFER_BRAKE
            putExtra("muleAccount", muleAccount)
            putExtra("companyName", company)
        }
        startService(brakeIntent)
    }

    override fun onInterrupt() {
        Log.i(TAG, "ChatDefenseAccessibilityService interrupted")
    }
}
