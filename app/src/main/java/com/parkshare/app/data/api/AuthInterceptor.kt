package com.parkshare.app.data.api

import com.parkshare.app.data.auth.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val authRepository: AuthRepository = AuthRepository()
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // If the user is authenticated with Firebase, retrieve current ID token
        val token: String? = if (authRepository.isUserLoggedIn()) {
            try {
                runBlocking {
                    authRepository.getIdToken(forceRefresh = false)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
