package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
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
        validateForm(showAll = hasAttemptedSubmit)
    }

    fun onNewPasswordChanged(password: String) {
        newPassword = password.trim()
        newPasswordTouched = true
        validateForm(showAll = hasAttemptedSubmit)
    }

    fun onConfirmPasswordChanged(password: String) {
        confirmPassword = password.trim()
        confirmPasswordTouched = true
        validateForm(showAll = hasAttemptedSubmit)
    }

    private fun validateForm(showAll: Boolean = false): Boolean {
        val lengthValid = newPassword.length >= 6
        val uppercaseValid = newPassword.any { it in 'A'..'Z' }
        val lowercaseValid = newPassword.any { it in 'a'..'z' }
        val numberValid = newPassword.any { it in '0'..'9' }
        val specialChars = "!@#$%^&*()_+-=[]{}|;:'\",.<>?/"
        val specialValid = newPassword.any { it in specialChars }
        val confirmMatches = newPassword == confirmPassword

        val isCurrentPasswordEmpty = currentPassword.isEmpty()
        val isNewPasswordEmpty = newPassword.isEmpty()
        val isConfirmPasswordEmpty = confirmPassword.isEmpty()

        var currentPasswordError: Int? = null
        if (showAll || currentPasswordTouched) {
            if (isCurrentPasswordEmpty) {
                currentPasswordError = R.string.error_current_password_required
            }
        }

        var newPasswordError: Int? = null
        if (showAll || newPasswordTouched) {
            if (isNewPasswordEmpty) {
                newPasswordError = R.string.error_new_password_required
            } else if (showAll) {
                if (!lengthValid) {
                    newPasswordError = R.string.error_new_password_min_length
                } else if (!uppercaseValid) {
                    newPasswordError = R.string.error_new_password_uppercase
                } else if (!lowercaseValid) {
                    newPasswordError = R.string.error_new_password_lowercase
                } else if (!numberValid) {
                    newPasswordError = R.string.error_new_password_number
                } else if (!specialValid) {
                    newPasswordError = R.string.error_new_password_special_character
                }
            }
        }

        var confirmPasswordError: Int? = null
        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
            confirmPasswordError = R.string.error_passwords_do_not_match
        } else if (showAll || confirmPasswordTouched) {
            if (isConfirmPasswordEmpty) {
                confirmPasswordError = R.string.error_confirm_password_required
            } else if (confirmPassword != newPassword) {
                confirmPasswordError = R.string.error_passwords_do_not_match
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
                isConfirmPasswordEmpty ||
                !lengthValid ||
                !uppercaseValid ||
                !lowercaseValid ||
                !numberValid ||
                !specialValid ||
                confirmPassword != newPassword
    }

    fun isSubmitEnabled(): Boolean {
        val state = _uiState.value
        return currentPassword.isNotEmpty() &&
                newPassword.isNotEmpty() &&
                state.isNewPasswordValid &&
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
                    val errorRes = when (exception) {
                        is UnauthorizedException -> {
                            R.string.error_session_expired
                        }
                        is java.io.IOException -> {
                            R.string.error_network_error
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
                                parsedMessage.contains("incorrect password", ignoreCase = true) ||
                                parsedMessage.contains("The current password is incorrect", ignoreCase = true)) {
                                R.string.error_current_password_incorrect
                            } else if (parsedMessage.contains("Mật khẩu mới không được trùng với mật khẩu hiện tại", ignoreCase = true) ||
                                parsedMessage.contains("same password", ignoreCase = true) ||
                                parsedMessage.contains("cannot be the same", ignoreCase = true)) {
                                R.string.error_new_password_same_as_current
                            } else {
                                R.string.error_unable_to_update_password
                            }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isUnauthorized = exception is UnauthorizedException,
                        error = errorRes
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
