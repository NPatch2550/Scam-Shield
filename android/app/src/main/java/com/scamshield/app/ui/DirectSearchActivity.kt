package com.scamshield.app.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scamshield.app.R
import com.scamshield.app.network.RetrofitClient
import com.scamshield.app.network.SearchRequest
import kotlinx.coroutines.launch

class DirectSearchActivity : AppCompatActivity() {

    private lateinit var etQuery: EditText
    private lateinit var btnSearch: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var scrollResult: View
    private lateinit var tvCompany: TextView
    private lateinit var tvVerdict: TextView
    private lateinit var tvRecommendation: TextView
    private lateinit var tvRegulatoryDetails: TextView
    private lateinit var tvSeoDetails: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        etQuery = findViewById(R.id.et_search_query)
        btnSearch = findViewById(R.id.btn_run_search)
        progressBar = findViewById(R.id.search_progress)
        scrollResult = findViewById(R.id.search_result_scroll)
        tvCompany = findViewById(R.id.tv_result_company)
        tvVerdict = findViewById(R.id.tv_result_verdict)
        tvRecommendation = findViewById(R.id.tv_result_recommendation)
        tvRegulatoryDetails = findViewById(R.id.tv_regulatory_details)
        tvSeoDetails = findViewById(R.id.tv_seo_details)

        btnSearch.setOnClickListener {
            val q = etQuery.text.toString().trim()
            if (q.isNotEmpty()) {
                executeSearch(q)
            } else {
                Toast.makeText(this, "Enter a company name, domain, or bank account", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.chip_alphagold).setOnClickListener {
            etQuery.setText("AlphaGold Securities")
            executeSearch("AlphaGold Securities")
        }

        findViewById<Button>(R.id.chip_kbank).setOnClickListener {
            etQuery.setText("KasikornBank")
            executeSearch("KasikornBank")
        }

        findViewById<Button>(R.id.chip_domain).setOnClickListener {
            etQuery.setText("cryptovaultpro.io")
            executeSearch("cryptovaultpro.io")
        }

        findViewById<Button>(R.id.chip_mule).setOnClickListener {
            etQuery.setText("1234567890")
            executeSearch("1234567890")
        }
    }

    private fun executeSearch(query: String) {
        progressBar.visibility = View.VISIBLE
        scrollResult.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.search(SearchRequest(query = query))
                progressBar.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    scrollResult.visibility = View.VISIBLE

                    tvCompany.text = "\"${data.query}\""
                    tvVerdict.text = "${data.verdict.uppercase()} (Risk: ${data.riskScore}%)"
                    tvRecommendation.text = data.recommendation?.message ?: "Exercise caution."

                    // Regulatory details
                    val regBuilder = StringBuilder()
                    if (!data.scamDatabaseMatches.isNullOrEmpty()) {
                        val match = data.scamDatabaseMatches[0]
                        regBuilder.append("🚨 BLACKLIST HIT: ${match.category.uppercase()}\n")
                        regBuilder.append("• ${match.description}\n")
                        regBuilder.append("• Reported by ${match.reportCount} victims\n")
                    } else if (!data.legitimateCompanyMatches.isNullOrEmpty()) {
                        val legit = data.legitimateCompanyMatches[0]
                        regBuilder.append("✅ OFFICIAL REGULATORY WHITELIST MATCH\n")
                        regBuilder.append("• License: ${legit.licenseNumber} (${legit.regulator})\n")
                        regBuilder.append("• Official Domain: ${legit.officialDomain}\n")
                    } else {
                        regBuilder.append("⚠️ Entity NOT found in SEC Thailand official registry.\n")
                    }
                    tvRegulatoryDetails.text = regBuilder.toString()

                    // SEO / Domain details
                    val seoBuilder = StringBuilder()
                    val domainInfo = data.domainAnalysis
                    if (domainInfo != null) {
                        seoBuilder.append("• Domain: ${domainInfo.domain}\n")
                        if (domainInfo.ageMonths != null) {
                            seoBuilder.append("• Domain Age: ${domainInfo.ageMonths} months old")
                            if (domainInfo.ageMonths < 6) seoBuilder.append(" (⚠️ Critical Young Domain Alert)\n")
                            else seoBuilder.append("\n")
                        }
                        if (domainInfo.isTyposquat) {
                            seoBuilder.append("• 🚨 TYPOSQUAT CLONE: Resembles ${domainInfo.resembles}\n")
                        }
                        if (domainInfo.fakePRSaturation) {
                            seoBuilder.append("• ⚠️ Fake PR saturation / bot review manipulation detected\n")
                        }
                    } else {
                        seoBuilder.append("• Domain verification not applicable or no domain queried.\n")
                    }
                    tvSeoDetails.text = seoBuilder.toString()

                } else {
                    Toast.makeText(this@DirectSearchActivity, "Search error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DirectSearchActivity, "Connection error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
