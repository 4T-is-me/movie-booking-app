package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.VerifyOtpRequest
import com.example.movie_booking_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import android.content.Context
import com.example.movie_booking_app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    object Success : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun verifyOtp(email: String, otpCode: String, type: String = "forgot_password") {
        viewModelScope.launch {
            _uiState.value = OtpUiState.Loading
            try {
                val request = VerifyOtpRequest(email, otpCode)
                val response = if (type == "register") repository.verifyRegisterOtp(request) else repository.verifyOtp(request)
                if (response.isSuccessful) {
                    _uiState.value = OtpUiState.Success
                } else {
                    val errorStr = response.errorBody()?.string()
                    val message = try {
                        if (errorStr != null && errorStr.trim().startsWith("{")) {
                            JSONObject(errorStr).optString("message", context.getString(R.string.error_invalid_otp))
                        } else {
                            context.getString(R.string.error_invalid_otp)
                        }
                    } catch (e: Exception) {
                        context.getString(R.string.error_invalid_otp)
                    }
                    _uiState.value = OtpUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = OtpUiState.Error("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = OtpUiState.Idle
    }
}
