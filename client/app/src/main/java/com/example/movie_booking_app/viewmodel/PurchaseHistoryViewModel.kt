package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.data.repository.PurchaseHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PurchaseHistoryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
    private val repository: PurchaseHistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingHistoryItem>>(emptyList())
    val bookings: StateFlow<List<BookingHistoryItem>> = _bookings.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

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
                            _error.value = context.getString(R.string.auto_str_failed_to_load_booking_history)
                        }
                    } else {
                        _error.value = context.getString(R.string.error_server_code, response.code().toString())
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
