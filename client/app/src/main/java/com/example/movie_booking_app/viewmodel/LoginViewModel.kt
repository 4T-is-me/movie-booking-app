package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.repository.AuthRepository
import com.example.movie_booking_app.utils.Constants
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

    private fun getStringEn(resId: Int, vararg formatArgs: Any): String {
        val config = android.content.res.Configuration(context.resources.configuration)
        config.setLocale(java.util.Locale.ENGLISH)
        val localizedContext = context.createConfigurationContext(config)
        return localizedContext.getString(resId, *formatArgs)
    }

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
                    var message = try {
                        if (errorStr != null && errorStr.trim().startsWith("{")) {
                            JSONObject(errorStr).optString("message", getStringEn(R.string.error_invalid_credentials))
                        } else {
                            getStringEn(R.string.error_invalid_username_password)
                        }
                    } catch (e: Exception) {
                        getStringEn(R.string.error_invalid_username_password)
                    }

                    // Map based on HTTP Status Code for precision
                    if (response.code() == 401) {
                        message = getStringEn(R.string.error_invalid_username_password)
                    } else if (response.code() == 403) {
                        message = getStringEn(R.string.error_account_locked)
                    } else {
                        // Fallback to string matching if status code is generic
                        if (message.contains("Username, email, phone or password is incorrect", ignoreCase = true) ||
                            message.contains("Sai tài khoản hoặc mật khẩu", ignoreCase = true)
                        ) {
                            message = getStringEn(R.string.error_invalid_username_password)
                        } else if (message.contains("Account has been locked", ignoreCase = true) || 
                                   message.contains("Tài khoản của bạn đã bị khóa", ignoreCase = true)) {
                            message = getStringEn(R.string.error_account_locked)
                        }
                    }

                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(getStringEn(R.string.connection_error_format, e.localizedMessage ?: "Unknown error"))
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
                    val roleNameStr = body?.account?.roleId?.roleName ?: body?.account?.role ?: Constants.ROLE.USER.lowercase()
                    
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
                            JSONObject(errorStr).optString("message", getStringEn(R.string.error_google_login_failed))
                        } else {
                            getStringEn(R.string.error_google_login_failed)
                        }
                    } catch (e: Exception) {
                        getStringEn(R.string.error_google_login_failed)
                    }
                    _uiState.value = LoginUiState.Error(message)
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(getStringEn(R.string.connection_error_format, e.localizedMessage ?: "Unknown error"))
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState.Idle
    }
}
