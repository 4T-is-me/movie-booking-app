package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository to manage user profile data loading.
 */
class ProfileRepository {

    /**
     * Emits the user profile details.
     */
    fun getUserProfile(): Flow<UserProfile> = flow {
        // Mocking user profile data corresponding to the layout specs
        emit(
            UserProfile(
                name = "Nguyen Van A",
                phone = "0987654321",
                email = "abc@gmail.com"
            )
        )
    }
}
