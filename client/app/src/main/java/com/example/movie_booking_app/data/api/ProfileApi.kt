package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.data.model.entity.UserProfile
import com.example.movie_booking_app.data.model.request.UpdateProfileRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

interface ProfileApi {
    @GET("api/accounts/profile")
    suspend fun getProfile(): Response<UserProfile>

    @PUT("api/accounts/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfile>

    @Multipart
    @POST("api/accounts/upload-avatar")
    suspend fun uploadAvatar(
        @Part avatar: MultipartBody.Part
    ): Response<com.example.movie_booking_app.data.model.response.UploadAvatarResponse>
}

