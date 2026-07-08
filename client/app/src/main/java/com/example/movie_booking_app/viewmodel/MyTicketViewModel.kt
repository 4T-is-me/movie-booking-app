package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.data.repository.MyTicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyTicketViewModel @Inject constructor(
    private val repository: MyTicketRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tickets = MutableStateFlow<List<BookingHistoryItem>>(emptyList())
    val tickets: StateFlow<List<BookingHistoryItem>> = _tickets.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadActiveTickets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getMyTickets().collect { response ->
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            _tickets.value = body.data ?: emptyList()
                        } else {
                            _error.value = "Failed to load active tickets"
                        }
                    } else {
                        _error.value = "Server returned an error: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "Network error: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    fun resetError() {
        _error.value = null
    }
}
