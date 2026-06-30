package com.example.movie_booking_app.viewmodel

import com.example.movie_booking_app.data.model.entity.UserProfile

data class EditProfileUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val error: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val avatarUrl: String? = null,
    val nameError: String? = null,
    val phoneError: String? = null,
    val identityCardError: String? = null,
    val dateOfBirthError: String? = null,
    val addressError: String? = null
) {
    val hasValidationErrors: Boolean
        get() = nameError != null || phoneError != null || identityCardError != null || dateOfBirthError != null || addressError != null
}
