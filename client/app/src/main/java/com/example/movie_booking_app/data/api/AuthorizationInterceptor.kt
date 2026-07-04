package com.example.movie_booking_app.data.api

import android.content.Context
import com.example.movie_booking_app.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that injects authorization token (JWT) to OkHttp request headers.
 */
class AuthorizationInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = TokenManager.getAccessToken(context)
        val originalRequest = chain.request()
        
        // If there's a token, append it to the request as Bearer Authorization header
        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }
        
        return chain.proceed(requestBuilder.build())
    }
}
