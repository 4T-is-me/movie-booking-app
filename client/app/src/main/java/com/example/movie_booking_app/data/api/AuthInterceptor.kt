package com.example.movie_booking_app.data.api

import android.content.Context
import com.example.movie_booking_app.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor that adds Bearer token to headers and handles 401 Unauthorized responses.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        var token = TokenManager.getAccessToken(context)
        if (token.isNullOrEmpty()) {
            val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            token = sharedPref.getString("auth_token", null)
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            // Token is invalid or expired
            TokenManager.clearTokens(context)
            
            // Notify AuthViewModel or other listeners
            runBlocking {
                AuthEventBus.emit(AuthEvent.SessionExpired)
            }
        }

        return response
    }
}
