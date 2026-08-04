package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.LoginRequest
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

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val role: String, val username: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = repository.login(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()
                    val token = body?.accessToken
                    
                    val dbUsername = body?.account?.username ?: request.email
                    val roleNameStr = body?.account?.roleId?.roleName ?: body?.account?.role ?: "user"
                    
                    if (token != null) {
                        repository.saveLoginData(
                            token = token,
                            refreshToken = body?.refreshToken ?: "",
                            username = dbUsername,
                            role = roleNameStr
                        )
                    }
                    _uiState.value = LoginUiState.Success(roleNameStr, dbUsername)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val message = try {
                        if (errorStr != null && errorStr.trim().startsWith("{")) {
                            JSONObject(errorStr).optString("message", context.getString(R.string.error_invalid_credentials))
                        } else {
                            context.getString(R.string.error_invalid_username_password)
                        }
                    } catch (e: Exception) {
                        context.getString(R.string.error_invalid_username_password)
                    }
                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun googleLogin(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
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
                    _uiState.value = LoginUiState.Success(roleNameStr, dbUsername)
                } else {
                    val errorStr = response.errorBody()?.string()
                    val message = try {
                        if (errorStr != null && errorStr.trim().startsWith("{")) {
                            JSONObject(errorStr).optString("message", context.getString(R.string.error_google_login_failed))
                        } else {
                            context.getString(R.string.error_google_login_failed)
                        }
                    } catch (e: Exception) {
                        context.getString(R.string.error_google_login_failed)
                    }
                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Connection error: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
