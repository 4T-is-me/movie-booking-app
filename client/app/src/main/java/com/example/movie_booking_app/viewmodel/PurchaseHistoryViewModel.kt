package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.data.repository.PurchaseHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    private val repository: PurchaseHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingHistoryItem>>(emptyList())
    val bookings: StateFlow<List<BookingHistoryItem>> = _bookings.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _rateSuccess = MutableSharedFlow<Boolean>()
    val rateSuccess: SharedFlow<Boolean> = _rateSuccess.asSharedFlow()

    fun loadHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getPurchaseHistory().collect { response ->
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            _bookings.value = body.data ?: emptyList()
                        } else {
                            _error.value = "Failed to load booking history"
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

    fun rateStaff(bookingId: String, rating: Int, comment: String? = null) {
        viewModelScope.launch {
            try {
                val response = repository.rateStaff(bookingId, rating, comment)
                if (response.isSuccessful) {
                    _rateSuccess.emit(true)
                    loadHistory()
                } else {
                    _error.value = "Failed to submit rating: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Rating error: ${e.localizedMessage}"
            }
        }
    }

    fun resetError() {
        _error.value = null
    }
}
