package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.BookingDetailData
import com.example.movie_booking_app.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookingDetailViewModel @Inject constructor(
    private val repository: HistoryRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bookingDetail = MutableStateFlow<BookingDetailData?>(null)
    val bookingDetail: StateFlow<BookingDetailData?> = _bookingDetail.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadBookingDetail(bookingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            repository.getBookingDetail(bookingId).collect { response ->
                _isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        _bookingDetail.value = body.data
                    } else {
                        _error.value = "Failed to load booking details"
                    }
                } else {
                    _error.value = "Server returned an error: ${response.code()}"
                }
            }
        }
    }

    fun resetError() {
        _error.value = null
    }
}
