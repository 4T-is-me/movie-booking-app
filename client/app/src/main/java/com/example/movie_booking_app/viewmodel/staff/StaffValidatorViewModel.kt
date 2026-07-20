package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.CheckInTicketResponse
import com.example.movie_booking_app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffValidatorViewModel @Inject constructor(
    private val repository: StaffRepository
) : ViewModel() {

    private val _checkInResult = MutableStateFlow<CheckInTicketResponse?>(null)
    val checkInResult: StateFlow<CheckInTicketResponse?> = _checkInResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun checkInTicket(bookingCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.checkInTicket(bookingCode).collectLatest { response ->
                _checkInResult.value = response
                _isLoading.value = false
            }
        }
    }

    fun resetResult() {
        _checkInResult.value = null
    }
}
