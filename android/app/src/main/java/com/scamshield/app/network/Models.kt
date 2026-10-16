package com.scamshield.app.network

import com.google.gson.annotations.SerializedName

// Verify / Trust Search
data class SearchRequest(
    @SerializedName("query") val query: String? = null,
    @SerializedName("rawText") val rawText: String? = null
)

data class SearchResponse(
    @SerializedName("query") val query: String,
    @SerializedName("verdict") val verdict: String,
    @SerializedName("riskScore") val riskScore: Int,
    @SerializedName("pillars") val pillars: PillarsBreakdown?,
    @SerializedName("scamDatabaseMatches") val scamDatabaseMatches: List<ScamMatch>?,
    @SerializedName("legitimateCompanyMatches") val legitimateCompanyMatches: List<LegitMatch>?,
    @SerializedName("domainAnalysis") val domainAnalysis: DomainAnalysisResult?,
    @SerializedName("recommendation") val recommendation: RecommendationInfo?
)

data class PillarsBreakdown(
    @SerializedName("regulatory") val regulatory: PillarDetail?,
    @SerializedName("seo") val seo: PillarDetail?,
    @SerializedName("behavioral") val behavioral: PillarDetail?
)

data class PillarDetail(
    @SerializedName("score") val score: Int,
    @SerializedName("max") val max: Int,
    @SerializedName("breakdown") val breakdown: List<String>?
)

data class ScamMatch(
    @SerializedName("type") val type: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String?,
    @SerializedName("reportCount") val reportCount: Int,
    @SerializedName("verified") val verified: Boolean
)

data class LegitMatch(
    @SerializedName("name") val name: String,
    @SerializedName("nameTh") val nameTh: String?,
    @SerializedName("licenseNumber") val licenseNumber: String?,
    @SerializedName("licenseType") val licenseType: String?,
    @SerializedName("regulator") val regulator: String?,
    @SerializedName("officialDomain") val officialDomain: String?,
    @SerializedName("officialPhone") val officialPhone: List<String>?,
    @SerializedName("isActive") val isActive: Boolean
)

data class DomainAnalysisResult(
    @SerializedName("domain") val domain: String,
    @SerializedName("ageMonths") val ageMonths: Int?,
    @SerializedName("isTyposquat") val isTyposquat: Boolean,
    @SerializedName("resembles") val resembles: String?,
    @SerializedName("fakePRSaturation") val fakePRSaturation: Boolean,
    @SerializedName("inBlacklist") val inBlacklist: Boolean
)

data class RecommendationInfo(
    @SerializedName("action") val action: String,
    @SerializedName("message") val message: String,
    @SerializedName("color") val color: String?
)

// Call Screening
data class CallScreenResponse(
    @SerializedName("action") val action: String, // ALLOW | REJECT
    @SerializedName("riskScore") val riskScore: Int,
    @SerializedName("reason") val reason: String?,
    @SerializedName("category") val category: String?
)

// Call Session
data class CallSessionStartRequest(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("callerNumber") val callerNumber: String?
)

data class CallSessionStartResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("callerRisk") val callerRisk: CallScreenResponse,
    @SerializedName("message") val message: String
)

data class TranscriptChunkRequest(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("speaker") val speaker: String, // caller | user
    @SerializedName("text") val text: String,
    @SerializedName("deviceId") val deviceId: String?
)

data class TranscriptChunkResponse(
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("riskScore") val riskScore: Int,
    @SerializedName("riskLevel") val riskLevel: String,
    @SerializedName("newFlags") val newFlags: List<String>?,
    @SerializedName("piiAlerts") val piiAlerts: List<PiiAlert>?,
    @SerializedName("entitiesDetected") val entitiesDetected: List<EntityDetected>?
)

data class PiiAlert(
    @SerializedName("flag") val flag: String,
    @SerializedName("severity") val severity: String
)

data class EntityDetected(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String
)

// Crowdsourced Report
data class ReportSubmitRequest(
    @SerializedName("submittedBy") val submittedBy: String,
    @SerializedName("entityType") val entityType: String,
    @SerializedName("entityValue") val entityValue: String,
    @SerializedName("description") val description: String,
    @SerializedName("scamCategory") val scamCategory: String,
    @SerializedName("evidenceText") val evidenceText: String? = null
)

data class ReportSubmitResponse(
    @SerializedName("reportId") val reportId: String,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)
