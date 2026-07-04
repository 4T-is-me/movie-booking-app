package com.example.movie_booking_app.data.mock

import com.example.movie_booking_app.data.model.response.AuthResponse
import com.example.movie_booking_app.data.model.response.UserProfileResponse
import retrofit2.Response

/**
 * Mock data source for authentication for UI testing and offline development.
 */
object MockAuthDataSource {

    fun loginSuccess(): Response<AuthResponse> {
        val mockResponse = AuthResponse(
            token = "mock_access_token_12345",
            accountId = "6a3a485ada69f0c494281ba1",
            username = "mock_user",
            role = "Member"
        )
        return Response.success(mockResponse)
    }

    fun registerSuccess(): Response<AuthResponse> {
        val mockResponse = AuthResponse(
            token = "mock_access_token_67890",
            accountId = "6a3a485ada69f0c494281ba2",
            username = "new_mock_user",
            role = "Member"
        )
        return Response.success(mockResponse)
    }

    fun getProfile(): Response<UserProfileResponse> {
        val mockProfile = UserProfileResponse(
            username = "mock_user",
            fullName = "Mock User Account",
            email = "mock@example.com",
            phone = "0123456789",
            address = "123 Mock Street, Android City",
            score = 100,
            role = "Member"
        )
        return Response.success(mockProfile)
    }
}
