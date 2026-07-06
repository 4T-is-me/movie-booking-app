package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.VerifyOtpRequest
import com.example.movie_booking_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

sealed class OtpUiState {
    object Idle : OtpUiState()
    object Loading : OtpUiState()
    object Success : OtpUiState()
    data class Error(val message: String) : OtpUiState()
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<OtpUiState>(OtpUiState.Idle)
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    fun verifyOtp(email: String, otpCode: String) {
        viewModelScope.launch {
            _uiState.value = OtpUiState.Loading
            try {
                val request = VerifyOtpRequest(email, otpCode)
                val response = repository.verifyOtp(request)
                if (response.isSuccessful) {
                    _uiState.value = OtpUiState.Success
                } else {
                    val errorStr = response.errorBody()?.string()
                    val message = try {
                        if (errorStr != null && errorStr.trim().startsWith("{")) {
                            JSONObject(errorStr).optString("message", "Mã OTP sai hoặc đã hết hạn")
                        } else {
                            "Mã OTP sai hoặc đã hết hạn"
                        }
                    } catch (e: Exception) {
                        "Mã OTP sai hoặc đã hết hạn"
                    }
                    _uiState.value = OtpUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = OtpUiState.Error("Lỗi kết nối: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = OtpUiState.Idle
    }
}
