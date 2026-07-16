package com.example.movie_booking_app.viewmodel

import androidx.annotation.StringRes

data class ChangePasswordUiState(
    val lengthValid: Boolean = false,
    val uppercaseValid: Boolean = false,
    val lowercaseValid: Boolean = false,
    val numberValid: Boolean = false,
    val specialValid: Boolean = false,
    val confirmMatches: Boolean = true,
    
    @StringRes val currentPasswordError: Int? = null,
    @StringRes val newPasswordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
    
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    @StringRes val error: Int? = null,
    val isUnauthorized: Boolean = false
) {
    val isNewPasswordValid: Boolean
        get() = lengthValid && uppercaseValid && lowercaseValid && numberValid && specialValid
}
