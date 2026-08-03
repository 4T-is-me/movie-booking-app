package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.api.AuthEvent
import com.example.movie_booking_app.data.api.AuthEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for handling authentication state and global auth events.
 */
@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _authEvents = MutableSharedFlow<AuthEvent>()
    val authEvents = _authEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            AuthEventBus.events.collectLatest { event ->
                _authEvents.emit(event)
            }
        }
    }
}
