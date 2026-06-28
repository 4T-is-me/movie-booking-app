package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.BookingRepositoryImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SelectSeatViewModel(
    private val repository: BookingRepository = BookingRepositoryImpl()
) : ViewModel() {

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _showDates = MutableStateFlow<List<ShowDate>>(emptyList())
    val showDates: StateFlow<List<ShowDate>> = _showDates.asStateFlow()

    private val _showTimes = MutableStateFlow<List<ShowTime>>(emptyList())
    val showTimes: StateFlow<List<ShowTime>> = _showTimes.asStateFlow()

    private val _targetCount = MutableStateFlow(1)
    val targetCount: StateFlow<Int> = _targetCount.asStateFlow()

    private val _selectedSeats = MutableStateFlow<List<Seat>>(emptyList())
    val selectedSeats: StateFlow<List<Seat>> = _selectedSeats.asStateFlow()

    private val _validationError = MutableSharedFlow<ValidationError>()
    val validationError = _validationError.asSharedFlow()

    sealed class ValidationError {
        data class SelectMore(val count: Int) : ValidationError()
        data class SelectOnly(val count: Int) : ValidationError()
        object Success : ValidationError()
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.getSeats("1").collect { _seats.value = it }
        }
        viewModelScope.launch {
            repository.getShowDates("1").collect { _showDates.value = it }
        }
        viewModelScope.launch {
            repository.getShowTimes("1", "2024-06-24").collect { _showTimes.value = it }
        }
    }

    fun setTargetCount(count: Int) {
        _targetCount.value = count
    }

    fun onSeatClicked(seat: Seat) {
        if (seat.status == SeatStatus.SOLD) return

        // Immutable update to trigger UI refresh
        val updatedSeats = _seats.value.map { currentSeat ->
            if (currentSeat.id == seat.id) {
                val newStatus = if (currentSeat.status == SeatStatus.SELECTED) 
                    SeatStatus.AVAILABLE else SeatStatus.SELECTED
                currentSeat.copy(status = newStatus)
            } else {
                currentSeat
            }
        }
        _seats.value = updatedSeats
        _selectedSeats.value = updatedSeats.filter { it.status == SeatStatus.SELECTED }
    }

    fun validateAndProceed() {
        val selectedCount = _selectedSeats.value.size
        val target = _targetCount.value

        viewModelScope.launch {
            when {
                selectedCount < target -> {
                    _validationError.emit(ValidationError.SelectMore(target - selectedCount))
                }
                selectedCount > target -> {
                    _validationError.emit(ValidationError.SelectOnly(target))
                }
                else -> {
                    _validationError.emit(ValidationError.Success)
                }
            }
        }
    }

    val totalAmount: StateFlow<Int> = _selectedSeats.map { list: List<Seat> ->
        var total = 0
        list.forEach { s ->
            total += if (s.type == SeatType.VIP) 120000 else 80000
        }
        total
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)
}
