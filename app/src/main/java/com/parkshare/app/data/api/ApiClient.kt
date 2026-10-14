package com.parkshare.app.data.api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Default to Android Emulator loopback to host PC.
    // For physical device testing, update this to your machine's LAN IP (e.g., "http://192.168.1.100:5000/api/")
    var baseUrl: String = "http://10.0.2.2:5000/api/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            retrofitInstance = null
            apiServiceInstance = null
        }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var retrofitInstance: Retrofit? = null
    private var apiServiceInstance: ParkShareApiService? = null

    private fun getRetrofit(): Retrofit {
        return retrofitInstance ?: synchronized(this) {
            retrofitInstance ?: Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { retrofitInstance = it }
        }
    }

    val apiService: ParkShareApiService
        get() = apiServiceInstance ?: synchronized(this) {
            apiServiceInstance ?: getRetrofit().create(ParkShareApiService::class.java).also {
                apiServiceInstance = it
            }
        }
}
