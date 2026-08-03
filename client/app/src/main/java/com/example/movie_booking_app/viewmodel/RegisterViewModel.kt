package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.request.RegisterRequest
import com.example.movie_booking_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class RegisterUiState {
    object Idle : RegisterUiState()
    object Loading : RegisterUiState()
    data class Success(val message: String) : RegisterUiState()
    data class Error(val message: String) : RegisterUiState()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val response = repository.register(request)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = RegisterUiState.Success("Registration successful!")
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                    var displayMsg = errorMsg
                    try {
                        val jsonObject = JSONObject(errorMsg)
                        if (jsonObject.has("errors")) {
                            val errorsArray = jsonObject.getJSONArray("errors")
                            if (errorsArray.length() > 0) {
                                displayMsg = errorsArray.getJSONObject(0).getString("message")
                            }
                        } else if (jsonObject.has("message")) {
                            displayMsg = jsonObject.getString("message")
                        }
                    } catch (e: Exception) {
                        // Ignore and use raw errorMsg
                    }
                    _uiState.value = RegisterUiState.Error(displayMsg)
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val response = repository.googleLogin(idToken)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val token = body?.accessToken
                    val dbUsername = body?.account?.username ?: "GoogleUser"
                    val roleNameStr = body?.account?.roleId?.roleName ?: body?.account?.role ?: "user"
                    
                    if (token != null) {
                        repository.saveLoginData(
                            token = token,
                            refreshToken = body?.refreshToken ?: "",
                            username = dbUsername,
                            role = roleNameStr
                        )
                    }
                    _uiState.value = RegisterUiState.Success("GOOGLE_SUCCESS")
                } else {
                    _uiState.value = RegisterUiState.Error(context.getString(R.string.auto_str_google_sign_in_failed))
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
