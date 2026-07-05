package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.api.ProfileApi
import com.example.movie_booking_app.data.model.entity.UserProfile
import com.example.movie_booking_app.data.model.request.UpdateProfileRequest
import com.example.movie_booking_app.data.model.response.UploadAvatarResponse
import com.example.movie_booking_app.data.model.response.toUserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UnauthorizedException(message: String) : Exception(message)

/**
 * Repository to manage user profile data loading.
 */
class ProfileRepository @Inject constructor(
    private val profileApi: ProfileApi
) {

    /**
     * Emits the user profile details.
     */
    fun getUserProfile(): Flow<UserProfile> = flow {
        val response = profileApi.getProfile()
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.toUserProfile())
        } else {
            val code = response.code()
            val errorMsg = response.errorBody()?.string() ?: "Failed to fetch profile"
            if (code == 401) {
                throw UnauthorizedException(errorMsg)
            } else {
                throw Exception(errorMsg)
            }
        }
    }

    /**
     * Updates and emits the user profile details.
     */
    fun updateProfile(request: UpdateProfileRequest): Flow<UserProfile> = flow {
        val response = profileApi.updateProfile(request)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!.toUserProfile())
        } else {
            val code = response.code()
            val errorMsg = response.errorBody()?.string() ?: "Failed to update profile"
            if (code == 401) {
                throw UnauthorizedException(errorMsg)
            } else {
                throw Exception(errorMsg)
            }
        }
    }

    /**
     * Uploads the user avatar.
     */
    fun uploadAvatar(avatarPart: okhttp3.MultipartBody.Part): Flow<UploadAvatarResponse> = flow {
        val response = profileApi.uploadAvatar(avatarPart)
        if (response.isSuccessful && response.body() != null) {
            emit(response.body()!!)
        } else {
            val code = response.code()
            val errorMsg = response.errorBody()?.string() ?: "Failed to upload avatar"
            if (code == 401) {
                throw UnauthorizedException(errorMsg)
            } else {
                throw Exception(errorMsg)
            }
        }
    }

    fun changePassword(request: com.example.movie_booking_app.data.model.request.ChangePasswordRequest): Flow<Unit> = flow {
        val response = profileApi.changePassword(request)
        if (response.isSuccessful) {
            emit(Unit)
        } else {
            val code = response.code()
            val errorMsg = response.errorBody()?.string() ?: "Failed to change password"
            if (code == 401) {
                throw UnauthorizedException(errorMsg)
            } else {
                throw Exception(errorMsg)
            }
        }
    }
}
