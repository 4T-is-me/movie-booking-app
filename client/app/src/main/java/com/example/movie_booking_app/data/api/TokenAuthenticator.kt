package com.example.movie_booking_app.data.api

import android.content.Context
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.request.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val context: Context,
    private val getAuthApi: () -> AuthApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Tránh vòng lặp vô hạn: Nếu request dùng để refresh token cũng bị 401 thì dừng lại
        if (response.request.url.encodedPath.contains("refresh-token")) {
            return null
        }

        synchronized(TokenManager) {
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
            val currentToken = TokenManager.getAccessToken(context)

            // Kiểm tra xem token đã được refresh bởi một thread khác chưa
            if (currentToken != null && currentToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = TokenManager.getRefreshToken(context)

            if (refreshToken.isNullOrEmpty()) {
                return logout(context)
            }

            // Gọi API refresh token một cách ĐỒNG BỘ (chặn thread hiện tại)
            val authApi = getAuthApi()
            val refreshCall = authApi.refreshToken(RefreshTokenRequest(refreshToken))

            try {
                val refreshResponse = refreshCall.execute() // Synchronous call

                if (refreshResponse.isSuccessful) {
                    val newTokens = refreshResponse.body()
                    
                    if (newTokens != null && newTokens.accessToken != null && newTokens.refreshToken != null) {
                        val newAccessToken = newTokens.accessToken
                        val newRefreshToken = newTokens.refreshToken
                        
                        // Lưu tokens mới vào TokenManager
                        TokenManager.saveTokens(context, newAccessToken, newRefreshToken)

                        // Tạo lại request ban đầu nhưng với Access Token mới
                        return response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Nếu gọi api refresh thất bại hoặc refresh token hết hạn
            return logout(context)
        }
    }

    private fun logout(context: Context): Request? {
        TokenManager.clearTokens(context)
        runBlocking {
            AuthEventBus.emit(AuthEvent.SessionExpired)
        }
        return null // Trả về null để OkHttp không retry request nữa
    }
}
