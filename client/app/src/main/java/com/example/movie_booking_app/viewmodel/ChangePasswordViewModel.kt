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

    fun onCurrentPasswordChanged(password: String) {
        currentPassword = password.trim()
        validateForm()
    }

    fun onNewPasswordChanged(password: String) {
        newPassword = password.trim()
        validateForm()
    }

    fun onConfirmPasswordChanged(password: String) {
        confirmPassword = password.trim()
        validateForm()
    }

    private fun validateForm() {
        val lengthValid = newPassword.length >= 6
        val uppercaseValid = newPassword.any { it.isUpperCase() }
        val lowercaseValid = newPassword.any { it.isLowerCase() }
        val numberValid = newPassword.any { it.isDigit() }
        val specialValid = newPassword.any { !it.isLetterOrDigit() }
        val confirmMatches = newPassword == confirmPassword && confirmPassword.isNotEmpty()

        var currentPasswordError: String? = null
        var confirmPasswordError: String? = null

        if (newPassword.isNotEmpty() && currentPassword.isNotEmpty() && newPassword == currentPassword) {
            currentPasswordError = "Mật khẩu mới không được giống mật khẩu cũ"
        }

        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            confirmPasswordError = "Mật khẩu xác nhận không trùng khớp"
        }

        _uiState.value = _uiState.value.copy(
            lengthValid = lengthValid,
            uppercaseValid = uppercaseValid,
            lowercaseValid = lowercaseValid,
            numberValid = numberValid,
            specialValid = specialValid,
            confirmMatches = confirmMatches,
            currentPasswordError = currentPasswordError,
            confirmPasswordError = confirmPasswordError
        )
    }

    fun isSubmitEnabled(): Boolean {
        return currentPassword.isNotEmpty() &&
                _uiState.value.isNewPasswordValid &&
                _uiState.value.confirmMatches &&
                newPassword != currentPassword
    }

    fun submitChangePassword() {
        if (!isSubmitEnabled()) return

        viewModelScope.launch {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            repository.changePassword(request)
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSuccess = false)
                }
                .catch { exception ->
                    val rawMessage = exception.message ?: "Failed to update password"
                    val parsedMessage = try {
                        if (rawMessage.startsWith("{")) {
                            JSONObject(rawMessage).optString("message", "Error updating password")
                        } else {
                            rawMessage
                        }
                    } catch (e: Exception) {
                        rawMessage
                    }

                    if (exception is UnauthorizedException) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isUnauthorized = true,
                            error = parsedMessage
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = parsedMessage
                        )
                    }
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
        _uiState.value = ChangePasswordUiState()
    }
}
