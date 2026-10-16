package com.scamshield.app.ui

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scamshield.app.R
import com.scamshield.app.network.ReportSubmitRequest
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SocketManager
import kotlinx.coroutines.launch

class ReportScamActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner
    private lateinit var etValue: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etDescription: EditText
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        spinnerType = findViewById(R.id.spinner_entity_type)
        etValue = findViewById(R.id.et_report_value)
        spinnerCategory = findViewById(R.id.spinner_scam_category)
        etDescription = findViewById(R.id.et_report_description)
        btnSubmit = findViewById(R.id.btn_submit_report)

        setupSpinners()

        btnSubmit.setOnClickListener {
            submitReport()
        }
    }

    private fun setupSpinners() {
        val types = arrayOf("company", "domain", "phone", "bank_account", "crypto_wallet", "telegram_group", "line_group", "apk_url")
        spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        val categories = arrayOf("fake_broker", "pig_butchering", "task_scam", "crypto_scam", "mule_account", "fake_ipo", "phishing")
        spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
    }

    private fun submitReport() {
        val type = spinnerType.selectedItem.toString()
        val value = etValue.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val desc = etDescription.text.toString().trim()

        if (value.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false
        btnSubmit.text = "Submitting..."

        lifecycleScope.launch {
            try {
                val req = ReportSubmitRequest(
                    submittedBy = SocketManager.getDeviceId(),
                    entityType = type,
                    entityValue = value,
                    description = desc,
                    scamCategory = category
                )
                val response = RetrofitClient.api.submitReport(req)
                if (response.isSuccessful && response.body() != null) {
                    Toast.makeText(this@ReportScamActivity, "Report received! Added to moderation queue.", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    btnSubmit.isEnabled = true
                    btnSubmit.text = "Submit to Scam Shield Watchlist"
                    Toast.makeText(this@ReportScamActivity, "Failed to submit: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                btnSubmit.isEnabled = true
                btnSubmit.text = "Submit to Scam Shield Watchlist"
                Toast.makeText(this@ReportScamActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
