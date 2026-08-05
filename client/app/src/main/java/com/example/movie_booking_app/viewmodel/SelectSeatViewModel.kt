package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.data.model.entity.SeatType
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.utils.BookingTimerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

enum class SeatingUiState {
    IDLE, HOLDING, HELD, EXPIRED, ERROR
}

@HiltViewModel
class SelectSeatViewModel @Inject constructor(
    private val repository: BookingRepository,
    private val savedStateHandle: SavedStateHandle,
    val timerManager: BookingTimerManager
) : ViewModel() {

    private val isoFormat get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val dayFormat get() = SimpleDateFormat("dd", Locale.getDefault())
    private val monthFormat get() = SimpleDateFormat("MMM", Locale.getDefault())
    private val dowFormat get() = SimpleDateFormat("EEE", Locale.getDefault())
    private val yearFormat get() = SimpleDateFormat("yyyy", Locale.getDefault())

    private val movieId: String = savedStateHandle["movieId"] ?: ""
    private val targetCinemaId: String? = savedStateHandle["cinemaId"]

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _uiState = MutableStateFlow(SeatingUiState.IDLE)
    val uiState: StateFlow<SeatingUiState> = _uiState.asStateFlow()

    private var countdownCollectJob: Job? = null

    private var lastHeldSeatIds: List<String>?
        get() = savedStateHandle["lastHeldSeatIds"]
        set(value) { savedStateHandle["lastHeldSeatIds"] = value }

    private val _allShowtimes = MutableStateFlow<List<Showtime>>(emptyList())

    private val _dates = MutableStateFlow<List<ShowDate>>(emptyList())
    val dates: StateFlow<List<ShowDate>> = _dates.asStateFlow()

    private val _showTimes = MutableStateFlow<List<Showtime>>(emptyList())
    val times: StateFlow<List<Showtime>> = _showTimes.asStateFlow()

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _cinemaRoomName = MutableStateFlow<String?>(null)
    val cinemaRoomName: StateFlow<String?> = _cinemaRoomName.asStateFlow()

    private val _cinemaName = MutableStateFlow<String?>(null)
    val cinemaName: StateFlow<String?> = _cinemaName.asStateFlow()

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
                val filteredList = if (!targetCinemaId.isNullOrEmpty()) {
                    val filtered = list.filter { showtime -> showtime.cinemaId?.id == targetCinemaId }
                    if (filtered.isNotEmpty()) filtered else list
                } else {
                    list
                }
                _allShowtimes.value = filteredList
                
                if (filteredList.isEmpty()) {
                    _isLoading.value = false
                    _errorMessage.emit(savedStateHandle.get<String>("error_no_showtimes") ?: "error_no_showtimes")
                    return@collect
                }
                
                val dateMap = mutableMapOf<String, ShowDate>()

                list.forEach { showtime ->
                    val dateStr = showtime.showDate?.take(10) ?: return@forEach
                    if (!dateMap.containsKey(dateStr)) {
                        val dateObj = try { isoFormat.parse(dateStr) } catch (e: Exception) { 
                            Timber.e(e, "Date parse error: $dateStr")
                            null 
                        }
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
        // If user is holding seats, release them before switching date
        if (_uiState.value == SeatingUiState.HELD) {
            releaseCurrentHold()
        }

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
        // If user is holding seats, release them before switching showtime
        // Check if the showtime actually changed to avoid redundant releases
        val currentShowtime = _showTimes.value.find { it.isSelected }
        if (currentShowtime != null && currentShowtime.id != showtimeId) {
            if (_uiState.value == SeatingUiState.HELD) {
                releaseCurrentHold()
            }
        }

        _showTimes.update { list -> list.map { it.copy(isSelected = it.id == showtimeId) } }
        
        // Restore locally held seats for this showtime if we don't have them in the current instance
        val persistentHeld = repository.getLocalHeldSeats(showtimeId)
        if (lastHeldSeatIds.isNullOrEmpty() && persistentHeld.isNotEmpty()) {
            lastHeldSeatIds = persistentHeld
            _uiState.value = SeatingUiState.HELD
            // Restart countdown if timer is active
            if (timerManager.isActive.value) {
                startCountdown(showtimeId, persistentHeld)
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            repository.getSeats(showtimeId).collect { seatMapData ->
                val seatList = seatMapData.seats ?: emptyList()
                
                // Map HOLDING status to SELECTED if they are the user's own previously held seats
                val mappedSeats = if (!lastHeldSeatIds.isNullOrEmpty()) {
                    seatList.map { seat ->
                        // Check for both displayStatus (mapped) and rawStatus (from server) to be safe
                        val isHeldByMe = seat.id in lastHeldSeatIds!! && (seat.displayStatus == SeatStatus.HOLDING || seat.rawStatus == "holding")
                        if (isHeldByMe) {
                            seat.copy(displayStatus = SeatStatus.SELECTED)
                        } else {
                            seat
                        }
                    }
                } else {
                    seatList
                }

                _seats.value = mappedSeats
                _cinemaRoomName.value = seatMapData.cinemaRoomName
                _cinemaName.value = seatMapData.cinemaName
                
                // If we restored SELECTED seats from HOLDING, update _selectedSeats
                _selectedSeats.value = mappedSeats.filter { it.displayStatus == SeatStatus.SELECTED }

                _isLoading.value = false
                if (seatList.isEmpty()) {
                    _errorMessage.emit(savedStateHandle.get<String>("error_no_seats") ?: "error_no_seats")
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
                        _errorMessage.emit(savedStateHandle.get<String>("error_same_type") ?: "error_same_type")
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
                        savedStateHandle.get<String>("error_hold_conflict") ?: "error_hold_conflict"
                    } else {
                        exception.message ?: (savedStateHandle.get<String>("error_hold_failed") ?: "error_hold_failed")
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
        timerManager.startTimer(com.example.movie_booking_app.utils.Constants.BookingConfig.HOLD_TIME_SECONDS.toInt())
        countdownCollectJob?.cancel()
        countdownCollectJob = viewModelScope.launch {
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
        countdownCollectJob?.cancel()
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
