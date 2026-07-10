package com.example.movie_booking_app.viewmodel

data class ChangePasswordUiState(
    val lengthValid: Boolean = false,
    val uppercaseValid: Boolean = false,
    val lowercaseValid: Boolean = false,
    val numberValid: Boolean = false,
    val specialValid: Boolean = false,
    val confirmMatches: Boolean = false,
    
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val isUnauthorized: Boolean = false
) {
    val isNewPasswordValid: Boolean
        get() = lengthValid && uppercaseValid && lowercaseValid && numberValid && specialValid
}
