package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.repository.BookingRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class SelectSeatViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _showDates = MutableStateFlow<List<ShowDate>>(emptyList())
    val showDates: StateFlow<List<ShowDate>> = _showDates.asStateFlow()

    private val _showTimes = MutableStateFlow<List<TimeSlot>>(emptyList())
    val showTimes: StateFlow<List<TimeSlot>> = _showTimes.asStateFlow()

    private val _targetCount = MutableStateFlow(1)
    val targetCount: StateFlow<Int> = _targetCount.asStateFlow()

    private val _selectedSeats = MutableStateFlow<List<Seat>>(emptyList())
    val selectedSeats: StateFlow<List<Seat>> = _selectedSeats.asStateFlow()

    private val _validationError = MutableSharedFlow<ValidationError>()
    val validationError = _validationError.asSharedFlow()

    private var currentSelectedDate: String = ""
    private var currentSelectedTime: String = ""

    sealed class ValidationError {
        data class SelectMore(val count: Int) : ValidationError()
        data class SelectOnly(val count: Int) : ValidationError()
        object Success : ValidationError()
    }

    init {
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            _isLoading.value = true
            val loadingJob = launch { delay(500) }
            
            try {
                val dates = repository.getShowDates("1").take(1).firstOrNull() ?: emptyList()
                _showDates.value = dates
                
                val initialDate = dates.find { it.isSelected } ?: dates.firstOrNull()
                if (initialDate != null) {
                    currentSelectedDate = initialDate.toApiId()
                    val times = repository.getShowTimes("1", currentSelectedDate).take(1).firstOrNull() ?: emptyList()
                    _showTimes.value = times
                    
                    val initialTime = times.find { it.isSelected } ?: times.firstOrNull()
                    if (initialTime != null) {
                        currentSelectedTime = initialTime.time
                        val seats = repository.getSeats(currentSelectedDate, currentSelectedTime).take(1).firstOrNull() ?: emptyList()
                        _seats.value = seats
                    }
                }
            } catch (e: Exception) {
                _seats.value = emptyList()
            }
            
            loadingJob.join()
            if (_seats.value.isNotEmpty()) { _isLoading.value = false }
        }
    }

    fun onDateSelected(selectedDate: ShowDate) {
        viewModelScope.launch {
            _isLoading.value = true
            val loadingJob = launch { delay(500) }
            
            val dateStr = selectedDate.toApiId()
            currentSelectedDate = dateStr
            
            _showDates.value = _showDates.value.map {
                it.copy(isSelected = it.day == selectedDate.day)
            }
            
            try {
                val times = repository.getShowTimes("1", dateStr).take(1).firstOrNull() ?: emptyList()
                _showTimes.value = times
                
                val initialTime = times.find { it.isSelected } ?: times.firstOrNull()
                if (initialTime != null) {
                    currentSelectedTime = initialTime.time
                    val seats = repository.getSeats(dateStr, currentSelectedTime).take(1).firstOrNull() ?: emptyList()
                    _seats.value = seats
                    _selectedSeats.value = emptyList()
                }
            } catch (e: Exception) {
                _seats.value = emptyList()
            }
            
            loadingJob.join()
            if (_seats.value.isNotEmpty()) { _isLoading.value = false }
        }
    }

    fun onTimeSelected(selectedTime: TimeSlot) {
        viewModelScope.launch {
            _isLoading.value = true
            val loadingJob = launch { delay(500) }
            
            currentSelectedTime = selectedTime.time
            _showTimes.value = _showTimes.value.map {
                it.copy(isSelected = it.time == selectedTime.time)
            }
            
            try {
                val seats = repository.getSeats(currentSelectedDate, selectedTime.time).take(1).firstOrNull() ?: emptyList()
                _seats.value = seats
                _selectedSeats.value = emptyList()
            } catch (e: Exception) {
                _seats.value = emptyList()
            }
            
            loadingJob.join()
            if (_seats.value.isNotEmpty()) { _isLoading.value = false }
        }
    }

    fun setTargetCount(count: Int) {
        _targetCount.value = count
    }

    fun onSeatClicked(seat: Seat) {
        if (seat.status == SeatStatus.RESERVED) return

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
