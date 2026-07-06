package com.example.movie_booking_app.data.repository

import android.content.Context
import com.example.movie_booking_app.data.api.AuthApi
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.model.request.RegisterRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: AuthApi,
    @ApplicationContext private val context: Context
) {

    suspend fun login(request: LoginRequest) =
        api.login(request)

    suspend fun register(request: RegisterRequest) =
        api.register(request)

    suspend fun verifyOtp(request: com.example.movie_booking_app.data.model.request.VerifyOtpRequest) =
        api.verifyOtp(request)

    suspend fun verifyRegisterOtp(request: com.example.movie_booking_app.data.model.request.VerifyOtpRequest) =
        api.verifyRegisterOtp(request)

    suspend fun googleLogin(idToken: String) =
        api.googleLogin(mapOf("idToken" to idToken))


    fun saveLoginData(token: String, refreshToken: String, username: String, role: String) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("auth_token", token)
            putString("username", username)
            putString("role", role)
            apply()
        }
        TokenManager.saveTokens(context, token, refreshToken)
    }
}