package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.UserProfile
import com.example.movie_booking_app.data.model.request.UpdateProfileRequest
import com.example.movie_booking_app.data.repository.ProfileRepository
import com.example.movie_booking_app.data.repository.UnauthorizedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Profile Screen, managing state and data loading.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    private val _isUnauthorized = MutableStateFlow(false)
    val isUnauthorized: StateFlow<Boolean> = _isUnauthorized.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Loads the profile details from repository.
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            repository.getUserProfile()
                .onStart {
                    _isLoading.value = true
                    _error.value = null
                    _isUnauthorized.value = false
                }
                .catch { exception ->
                    _isLoading.value = false
                    if (exception is UnauthorizedException) {
                        _isUnauthorized.value = true
                    } else {
                        _error.value = exception.message ?: "Failed to load profile"
                    }
                }
                .collect { profile ->
                    _isLoading.value = false
                    _userProfile.value = profile
                }
        }
    }

    /**
     * Updates the user profile.
     */
    fun updateProfile(email: String?, phone: String?, address: String?) {
        viewModelScope.launch {
            val request = UpdateProfileRequest(email, phone, address)
            repository.updateProfile(request)
                .onStart {
                    _isLoading.value = true
                    _error.value = null
                    _updateSuccess.value = false
                    _isUnauthorized.value = false
                }
                .catch { exception ->
                    _isLoading.value = false
                    if (exception is UnauthorizedException) {
                        _isUnauthorized.value = true
                    } else {
                        val rawMsg = exception.message ?: "Failed to update profile"
                        _error.value = parseErrorMessage(rawMsg)
                    }
                }
                .collect { updatedProfile ->
                    _isLoading.value = false
                    _userProfile.value = updatedProfile
                    _updateSuccess.value = true
                }
        }
    }

    fun resetUpdateSuccess() {
        _updateSuccess.value = false
    }

    fun resetError() {
        _error.value = null
    }

    private fun parseErrorMessage(raw: String): String {
        return try {
            if (raw.startsWith("{") && raw.contains("message")) {
                val json = com.google.gson.JsonParser.parseString(raw).asJsonObject
                json.get("message")?.asString ?: raw
            } else {
                raw
            }
        } catch (e: Exception) {
            raw
        }
    }
}
