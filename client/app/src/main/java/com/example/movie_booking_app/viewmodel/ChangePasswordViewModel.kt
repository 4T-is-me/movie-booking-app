package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.ChangePasswordRequest
import com.example.movie_booking_app.data.repository.ProfileRepository
import com.example.movie_booking_app.data.repository.UnauthorizedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private var currentPassword = ""
    private var newPassword = ""
    private var confirmPassword = ""

    private var currentPasswordTouched = false
    private var newPasswordTouched = false
    private var confirmPasswordTouched = false
    private var hasAttemptedSubmit = false

    fun onCurrentPasswordChanged(password: String) {
        currentPassword = password.trim()
        currentPasswordTouched = true
        validateForm()
    }

    fun onNewPasswordChanged(password: String) {
        newPassword = password.trim()
        newPasswordTouched = true
        validateForm()
    }

    fun onConfirmPasswordChanged(password: String) {
        confirmPassword = password.trim()
        confirmPasswordTouched = true
        validateForm()
    }

    private fun validateForm(showAll: Boolean = false): Boolean {
        val lengthValid = newPassword.length >= 6
        val uppercaseValid = newPassword.any { it.isUpperCase() }
        val lowercaseValid = newPassword.any { it.isLowerCase() }
        val numberValid = newPassword.any { it.isDigit() }
        val specialValid = newPassword.any { !it.isLetterOrDigit() }
        val confirmMatches = newPassword == confirmPassword && confirmPassword.isNotEmpty()

        // 1. Current Password Empty
        val isCurrentPasswordEmpty = currentPassword.isEmpty()
        var currentPasswordError: String? = null
        if (showAll || currentPasswordTouched) {
            if (isCurrentPasswordEmpty) {
                currentPasswordError = "Current password cannot be empty."
            }
        }

        // 2. New Password Empty
        // 4. New Password Length
        // 6. Same As Current Password
        val isNewPasswordEmpty = newPassword.isEmpty()
        var newPasswordError: String? = null
        if (showAll || newPasswordTouched) {
            if (isNewPasswordEmpty) {
                newPasswordError = "New password cannot be empty."
            } else if (newPassword.length < 6) {
                newPasswordError = "New password must be at least 6 characters."
            } else if (newPassword == currentPassword) {
                newPasswordError = "New password must be different from your current password."
            }
        }

        // 3. Confirm Password Empty
        val isConfirmPasswordEmpty = confirmPassword.isEmpty()
        var confirmPasswordError: String? = null
        if (showAll || confirmPasswordTouched) {
            if (isConfirmPasswordEmpty) {
                confirmPasswordError = "Please confirm your new password."
            }
        }

        // 5. Password Confirmation mismatch
        val isMismatch = newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword
        if (isMismatch) {
            if (showAll || (newPasswordTouched && confirmPasswordTouched)) {
                newPasswordError = "New password and confirmation password do not match."
                confirmPasswordError = "New password and confirmation password do not match."
            }
        }

        _uiState.value = _uiState.value.copy(
            lengthValid = lengthValid,
            uppercaseValid = uppercaseValid,
            lowercaseValid = lowercaseValid,
            numberValid = numberValid,
            specialValid = specialValid,
            confirmMatches = confirmMatches,
            currentPasswordError = currentPasswordError,
            newPasswordError = newPasswordError,
            confirmPasswordError = confirmPasswordError
        )

        return isCurrentPasswordEmpty ||
                isNewPasswordEmpty ||
                newPassword.length < 6 ||
                newPassword == currentPassword ||
                isConfirmPasswordEmpty ||
                newPassword != confirmPassword
    }

    fun isSubmitEnabled(): Boolean {
        return currentPassword.isNotEmpty() &&
                newPassword.isNotEmpty() &&
                newPassword.length >= 6 &&
                newPassword != currentPassword &&
                confirmPassword.isNotEmpty() &&
                newPassword == confirmPassword
    }

    fun submitChangePassword() {
        hasAttemptedSubmit = true
        val hasErrors = validateForm(showAll = true)
        if (hasErrors) return

        viewModelScope.launch {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            repository.changePassword(request)
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)
                }
                .catch { exception ->
                    val errorMsg = when (exception) {
                        is UnauthorizedException -> {
                            "Your session has expired. Please log in again."
                        }
                        is java.io.IOException -> {
                            "Unable to connect to the server. Please try again."
                        }
                        else -> {
                            val rawMessage = exception.message ?: ""
                            val parsedMessage = try {
                                if (rawMessage.startsWith("{")) {
                                    JSONObject(rawMessage).optString("message", "")
                                } else {
                                    rawMessage
                                }
                            } catch (e: Exception) {
                                rawMessage
                            }

                            if (parsedMessage.contains("Mật khẩu hiện tại không đúng", ignoreCase = true) ||
                                parsedMessage.contains("current password is incorrect", ignoreCase = true) ||
                                parsedMessage.contains("incorrect password", ignoreCase = true)) {
                                "The current password is incorrect."
                            } else if (parsedMessage.isNotEmpty()) {
                                parsedMessage
                            } else {
                                "Password could not be updated. Please try again."
                            }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUnauthorized = exception is UnauthorizedException,
                        error = errorMsg
                    )
                }
                .collect {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
        }
    }

    fun resetState() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
        currentPasswordTouched = false
        newPasswordTouched = false
        confirmPasswordTouched = false
        hasAttemptedSubmit = false
        _uiState.value = ChangePasswordUiState()
    }
}
