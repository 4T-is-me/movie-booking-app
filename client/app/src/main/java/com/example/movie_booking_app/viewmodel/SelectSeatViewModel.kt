package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.*
import com.example.movie_booking_app.data.repository.BookingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class SeatingUiState {
    IDLE, HOLDING, HELD, EXPIRED, ERROR
}

@HiltViewModel
class SelectSeatViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val savedStateHandle: SavedStateHandle,
    val timerManager: com.example.movie_booking_app.utils.BookingTimerManager
) : ViewModel() {

    private val movieId: String = savedStateHandle["movieId"] ?: ""

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow(SeatingUiState.IDLE)
    val uiState: StateFlow<SeatingUiState> = _uiState.asStateFlow()

    private var lastHeldSeatIds: List<String>? = null

    private val _allShowtimes = MutableStateFlow<List<Showtime>>(emptyList())

    private val _dates = MutableStateFlow<List<ShowDate>>(emptyList())
    val dates: StateFlow<List<ShowDate>> = _dates.asStateFlow()

    private val _showTimes = MutableStateFlow<List<Showtime>>(emptyList())
    val times: StateFlow<List<Showtime>> = _showTimes.asStateFlow()

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _selectedSeats = MutableStateFlow<List<Seat>>(emptyList())
    val selectedSeats: StateFlow<List<Seat>> = _selectedSeats.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()

    private val _holdSuccess = MutableStateFlow(false)
    val holdSuccess: StateFlow<Boolean> = _holdSuccess.asStateFlow()

    private val _error = MutableSharedFlow<String?>()
    val error = _error.asSharedFlow()

    private val _targetCount = MutableStateFlow(1)
    val targetCount: StateFlow<Int> = _targetCount.asStateFlow()

    init {
        loadInitialData(movieId)
    }

    private fun loadInitialData(targetMovieId: String) {
        if (targetMovieId.isEmpty()) {
            _isLoading.value = false
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            repository.getMovieShowtimes(targetMovieId).collect { list ->
                _allShowtimes.value = list
                
                if (list.isEmpty()) {
                    _isLoading.value = false
                    _errorMessage.emit(savedStateHandle.get<String>("error_no_showtimes") ?: "This movie has no available showtimes.")
                    return@collect
                }
                
                val dateMap = mutableMapOf<String, ShowDate>()
                val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
                val dowFormat = SimpleDateFormat("EEE", Locale.getDefault())
                val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

                list.forEach { showtime ->
                    val dateStr = showtime.showDate?.take(10) ?: return@forEach
                    if (!dateMap.containsKey(dateStr)) {
                        val dateObj = try { isoFormat.parse(dateStr) } catch (e: Exception) { null }
                        if (dateObj != null) {
                            dateMap[dateStr] = ShowDate(
                                id = dateStr,
                                day = dayFormat.format(dateObj),
                                month = monthFormat.format(dateObj),
                                dayOfWeek = dowFormat.format(dateObj),
                                year = yearFormat.format(dateObj),
                                isSelected = false
                            )
                        }
                    }
                }

                val sortedDates = dateMap.values.toList().sortedBy { it.id }
                _dates.value = sortedDates
                
                if (sortedDates.isNotEmpty()) {
                    selectDate(sortedDates[0].id)
                } else {
                    _isLoading.value = false
                }
            }
        }
    }

    fun selectDate(dateId: String) {
        _dates.update { list -> list.map { it.copy(isSelected = it.id == dateId) } }
        
        val filteredTimes = _allShowtimes.value.filter { it.showDate?.startsWith(dateId) == true }
        _showTimes.value = filteredTimes.mapIndexed { index, showtime ->
            showtime.copy(isSelected = index == 0)
        }
        
        if (filteredTimes.isNotEmpty()) {
            selectTime(filteredTimes[0].id)
        } else {
            _seats.value = emptyList()
            _isLoading.value = false
        }
    }

    fun selectTime(showtimeId: String) {
        _showTimes.update { list -> list.map { it.copy(isSelected = it.id == showtimeId) } }
        
        viewModelScope.launch {
            _isLoading.value = true
            repository.getSeats(showtimeId).collect { seatList ->
                _seats.value = seatList
                _selectedSeats.value = emptyList()
                _isLoading.value = false
                if (seatList.isEmpty()) {
                    _errorMessage.emit(savedStateHandle.get<String>("error_no_seats") ?: "No seats available for this showtime.")
                }
            }
        }
    }

    fun setTargetCount(count: Int) {
        _targetCount.value = count
    }

    fun toggleSeatSelection(seatId: String) {
        val seatToToggle = _seats.value.find { it.id == seatId } ?: return
        
        if (seatToToggle.displayStatus == SeatStatus.RESERVED || seatToToggle.displayStatus == SeatStatus.HOLDING) return

        if (seatToToggle.displayStatus != SeatStatus.SELECTED) {
            val currentlySelected = _selectedSeats.value
            if (currentlySelected.isNotEmpty()) {
                val baseType = currentlySelected.first().type
                if (seatToToggle.type != baseType) {
                    viewModelScope.launch {
                        _errorMessage.emit(savedStateHandle.get<String>("error_same_type") ?: "Please select seats of the same type")
                    }
                    return
                }
            }
        }

        _seats.update { list ->
            list.map { currentSeat ->
                if (currentSeat.id == seatId) {
                    val newStatus = if (currentSeat.displayStatus == SeatStatus.SELECTED) 
                        SeatStatus.AVAILABLE else SeatStatus.SELECTED
                    currentSeat.copy(displayStatus = newStatus)
                } else {
                    currentSeat
                }
            }
        }
        _selectedSeats.value = _seats.value.filter { it.displayStatus == SeatStatus.SELECTED }
    }

    val selectedSeatsTotalCount: StateFlow<Int> = _selectedSeats.map { list ->
        list.fold(0) { acc, seat -> acc + if (seat.type == SeatType.COUPLE) 2 else 1 }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val totalPrice: StateFlow<Int> = _selectedSeats.map { list ->
        list.fold(0) { acc, seat -> acc + seat.price }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun holdSeats(showtimeId: String, seatIds: List<String>) {
        // If selection hasn't changed and timer is still active, just proceed without resetting
        if (lastHeldSeatIds?.sorted() == seatIds.sorted() && timerManager.isActive.value) {
            _holdSuccess.value = true
            return
        }

        viewModelScope.launch {
            repository.holdSeats(showtimeId, seatIds)
                .onStart {
                    _isLoading.value = true
                    _uiState.value = SeatingUiState.HOLDING
                }
                .catch { exception ->
                    _isLoading.value = false
                    _uiState.value = SeatingUiState.ERROR
                    val msg = if (exception.message == "CONFLICT") {
                        savedStateHandle.get<String>("error_hold_conflict") ?: "Ghế đã bị người khác chọn, vui lòng chọn lại"
                    } else {
                        exception.message ?: (savedStateHandle.get<String>("error_hold_failed") ?: "Failed to hold seats")
                    }
                    _errorMessage.emit(msg)
                }
                .collect {
                    _isLoading.value = false
                    _uiState.value = SeatingUiState.HELD
                    lastHeldSeatIds = seatIds
                    _holdSuccess.value = true
                    startCountdown(showtimeId, seatIds)
                }
        }
    }

    private fun startCountdown(showtimeId: String, seatIds: List<String>) {
        timerManager.startTimer(900)
        viewModelScope.launch {
            timerManager.isExpired.collect { expired ->
                if (expired) {
                    _uiState.value = SeatingUiState.EXPIRED
                    releaseSeats(showtimeId, seatIds)
                }
            }
        }
    }

    fun releaseCurrentHold() {
        val selectedTime = _showTimes.value.find { it.isSelected }
        val seatIds = _selectedSeats.value.map { it.id }
        if (selectedTime != null && seatIds.isNotEmpty()) {
            releaseSeats(selectedTime.id, seatIds)
        }
    }

    private fun releaseSeats(showtimeId: String, seatIds: List<String>) {
        timerManager.stopTimer()
        viewModelScope.launch {
            repository.releaseSeats(showtimeId, seatIds)
                .catch { /* Silently fail on release */ }
                .collect {
                    _uiState.value = SeatingUiState.IDLE
                    lastHeldSeatIds = null
                    _selectedSeats.value = emptyList()
                    // Re-fetch seats to show they are available again
                    selectTime(showtimeId)
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Note: Release seats here if needed using a separate scope since viewModelScope is canceled
    }

    fun resetHoldSuccess() {
        _holdSuccess.value = false
    }
}
