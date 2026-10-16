package com.scamshield.app.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Default to localhost for Android emulator (10.0.2.2) or public Cloudflare tunnel
    const val DEFAULT_PUBLIC_URL = "https://royalty-deutsch-fantasy-consult.trycloudflare.com/"
    const val DEFAULT_EMULATOR_URL = "http://10.0.2.2:3000/"

    var baseUrl: String = DEFAULT_EMULATOR_URL
        set(value) {
            val formatted = if (value.endsWith("/")) value else "$value/"
            field = formatted
            _api = createApi(formatted)
        }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun createApi(url: String): ScamShieldApi {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ScamShieldApi::class.java)
    }

    private var _api: ScamShieldApi = createApi(baseUrl)

    val api: ScamShieldApi
        get() = _api
}
