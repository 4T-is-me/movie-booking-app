package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.userProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository to manage user profile data loading.
 */
class profileRepository {

    /**
     * Emits the user profile details.
     */
    fun getUserProfile(): Flow<userProfile> = flow {
        // Mocking user profile data corresponding to the layout specs
        emit(
            userProfile(
                name = "Nguyen Van A",
                phone = "0987654321",
                email = "abc@gmail.com"
            )
        )
    }
}
