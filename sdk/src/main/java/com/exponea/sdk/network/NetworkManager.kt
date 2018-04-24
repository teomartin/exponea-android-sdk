package com.exponea.sdk.network

import com.exponea.sdk.models.ExponeaConfiguration
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor

class NetworkManager(private var exponeaConfiguration: ExponeaConfiguration) {
    private val mediaTypeJson: MediaType = MediaType.parse("application/json")!!
    private lateinit var networkClient: OkHttpClient

    init {
        setupNetworkClient()
    }

    private fun getNetworkInterceptor(): Interceptor {
        return Interceptor {
            var request = it.request()

            request = request.newBuilder()
                    .addHeader("Authorization", "${exponeaConfiguration.authorization}")
                    .build()

            return@Interceptor it.proceed(request)
        }
    }

    private fun getNetworkLogger(): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()

        interceptor.level = HttpLoggingInterceptor.Level.BODY

        return interceptor
    }

    private fun setupNetworkClient() {
        val networkInterceptor = getNetworkInterceptor()

        networkClient = OkHttpClient.Builder()
                .addInterceptor(getNetworkLogger())
                .addInterceptor(networkInterceptor)
                .build()
    }

    fun post(endpoint: String, body: String): Call {
        val request = Request.Builder()
                .url(exponeaConfiguration.baseURL + endpoint)
                .post(
                        RequestBody.create(mediaTypeJson, body)
                )
                .build()

        return networkClient.newCall(request)
    }
}