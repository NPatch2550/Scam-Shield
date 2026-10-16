package com.scamshield.app.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ScamShieldApi {

    // "Before You Invest" Universal Trust Search
    @POST("api/v1/verify/search")
    suspend fun search(@Body request: SearchRequest): Response<SearchResponse>

    // Call Screening Pre-Check
    @GET("api/v1/call/screen/{number}")
    suspend fun screenCall(@Path("number") number: String): Response<CallScreenResponse>

    // 3-Way Call Session Start
    @POST("api/v1/call/session/start")
    suspend fun startCallSession(@Body request: CallSessionStartRequest): Response<CallSessionStartResponse>

    // 3-Way Call Audio Transcript Chunk (PBX STT Feed)
    @POST("api/v1/call/transcript")
    suspend fun sendTranscriptChunk(@Body request: TranscriptChunkRequest): Response<TranscriptChunkResponse>

    // End & Wipe Call Session
    @POST("api/v1/call/session/end")
    suspend fun endCallSession(@Body body: Map<String, String>): Response<Map<String, Any>>

    // Submit Crowdsourced Report
    @POST("api/v1/report/submit")
    suspend fun submitReport(@Body request: ReportSubmitRequest): Response<ReportSubmitResponse>
}
