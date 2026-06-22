package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.userProfile
import com.example.movie_booking_app.data.repository.profileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for Profile Screen, managing state and data loading.
 */
class profileViewModel(
    private val repository: profileRepository = profileRepository()
) : ViewModel() {

    private val _userProfile = MutableStateFlow<userProfile?>(null)
    val userProfile: StateFlow<userProfile?> = _userProfile.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Loads the profile details from repository.
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            repository.getUserProfile()
                .catch { exception ->
                    _error.value = exception.message ?: "Failed to load profile"
                }
                .collect { profile ->
                    _userProfile.value = profile
                }
        }
    }
}
