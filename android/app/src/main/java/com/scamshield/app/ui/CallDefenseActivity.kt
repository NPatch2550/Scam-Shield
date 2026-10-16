package com.scamshield.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scamshield.app.R
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SocketManager
import com.scamshield.app.network.TranscriptChunkRequest
import com.scamshield.app.services.CallDefenseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallDefenseActivity : AppCompatActivity() {

    private lateinit var tvCallStatus: TextView
    private lateinit var tvCallRiskBadge: TextView
    private lateinit var tvCallerInfo: TextView
    private lateinit var tvLiveTranscript: TextView
    private lateinit var btnTriggerMerge: Button
    private lateinit var btnDisconnectBot: Button

    private var activeSessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_defense)

        tvCallStatus = findViewById(R.id.tv_call_status)
        tvCallRiskBadge = findViewById(R.id.tv_call_risk_badge)
        tvCallerInfo = findViewById(R.id.tv_caller_info)
        tvLiveTranscript = findViewById(R.id.tv_live_transcript)
        btnTriggerMerge = findViewById(R.id.btn_trigger_merge)
        btnDisconnectBot = findViewById(R.id.btn_disconnect_bot)

        btnTriggerMerge.setOnClickListener {
            // Activate Call Shield & dial the Cloud PBX 3-way conference line
            CallDefenseManager.activateCallShieldAndMerge(this, "0891234567")
            Toast.makeText(this, "Dialing Cloud PBX (+6620001207)... Tap Merge on dialer!", Toast.LENGTH_LONG).show()
            startSimulatedSpeechStreaming()
        }

        btnDisconnectBot.setOnClickListener {
            CallDefenseManager.disconnectCallShield(this)
            tvCallStatus.text = "⏹️ Bot Disconnected"
            tvCallStatus.setTextColor(android.graphics.Color.GRAY)
            Toast.makeText(this, "3-Way audio RAM permanently wiped.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startSimulatedSpeechStreaming() {
        val transcriptFeed = StringBuilder()

        val sampleChunks = listOf(
            Pair("caller", "สวัสดีครับ ผมโทรจาก AlphaGold Securities มีโครงการลงทุนทองคำผลตอบแทน 40% ต่อเดือน"),
            Pair("caller", "ไม่มีความเสี่ยงแน่นอนครับ รับรองกำไร โควต้าพิเศษเหลือ 5 ที่สุดท้ายเท่านั้น"),
            Pair("caller", "โหลดแอปของเราเพื่อเริ่มเทรดได้เลยครับที่ alphagold-th.com/download/alphagold-trading-v2.apk"),
            Pair("user", "ถ้าได้กำไรแล้วจะถอนยังไงครับ?"),
            Pair("caller", "ก่อนถอนกำไรต้องโอนชำระค่าภาษีปลดล็อก 25% เข้าบัญชี 1234567890 ก่อนนะครับ")
        )

        lifecycleScope.launch {
            activeSessionId = CallDefenseManager.currentSessionId ?: "demo_session_${System.currentTimeMillis()}"

            for ((speaker, text) in sampleChunks) {
                delay(3000)

                val req = TranscriptChunkRequest(
                    sessionId = activeSessionId!!,
                    speaker = speaker,
                    text = text,
                    deviceId = SocketManager.getDeviceId()
                )

                try {
                    val resp = RetrofitClient.api.sendTranscriptChunk(req)
                    if (resp.isSuccessful && resp.body() != null) {
                        val result = resp.body()!!
                        transcriptFeed.append("\n[$speaker]: $text\n")
                        if (!result.newFlags.isNullOrEmpty()) {
                            transcriptFeed.append("🚩 [NLP Threat Engine]: ${result.newFlags.joinToString(", ")}\n")
                        }
                        tvLiveTranscript.text = transcriptFeed.toString()

                        tvCallRiskBadge.text = "${result.riskLevel.uppercase()} ${result.riskScore}%"
                    }
                } catch (e: Exception) {
                    transcriptFeed.append("\n[$speaker]: $text\n")
                    tvLiveTranscript.text = transcriptFeed.toString()
                }
            }
        }
    }
}
