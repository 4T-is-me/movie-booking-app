package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.SeatPreview
import com.example.movie_booking_app.data.model.request.AddRoomRequest
import com.example.movie_booking_app.data.model.request.SeatInput
import com.example.movie_booking_app.data.repository.CinemaRoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCinemaRoomViewModel @Inject constructor(
    private val repository: CinemaRoomsRepository
) : ViewModel() {

    val roomId = MutableStateFlow<String?>(null)
    val roomName = MutableStateFlow("")
    val roomType = MutableStateFlow("Standard")
    val screenType = MutableStateFlow("2D")
    val rowsStr = MutableStateFlow("8")
    val columnsStr = MutableStateFlow("10")
    val status = MutableStateFlow("active")

    private var isPrefilling = false

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _previewSeats = MutableStateFlow<List<SeatPreview>>(emptyList())
    val previewSeats: StateFlow<List<SeatPreview>> = _previewSeats.asStateFlow()

    // Dynamically calculate capacity based on rows and columns
    val capacity: StateFlow<Int> = combine(rowsStr, columnsStr) { rStr, cStr ->
        val r = rStr.toIntOrNull() ?: 0
        val c = cStr.toIntOrNull() ?: 0
        r * c
    }.stateIn(viewModelScope, SharingStarted.Lazily, 80)

    init {
        // Automatically generate layout preview when rowsStr or columnsStr changes
        viewModelScope.launch {
            combine(rowsStr, columnsStr) { rStr, cStr ->
                Pair(rStr.toIntOrNull(), cStr.toIntOrNull())
            }.collectLatest { (r, c) ->
                if (isPrefilling) {
                    return@collectLatest
                }
                if (r != null && r in 1..8 && c != null && c in 1..10) {
                    generatePreview(r, c)
                } else {
                    _previewSeats.value = emptyList()
                }
            }
        }
    }

    private fun generatePreview(rows: Int, cols: Int) {
        _errorMessage.value = null
        val seats = mutableListOf<SeatPreview>()
        for (row in 0 until rows) {
            val rowLetter = (row + 65).toChar().toString()
            for (col in 1..cols) {
                // Guess standard layout automatically: Standard, last 2 rows VIP, last row Couple
                val defaultType = when {
                    row == rows - 1 -> "couple"
                    row == rows - 2 || row == rows - 3 -> "vip"
                    else -> "standard"
                }
                seats.add(SeatPreview(seatName = "$rowLetter$col", seatType = defaultType))
            }
        }
        _previewSeats.value = seats
    }

    fun cycleSeatType(position: Int) {
        val currentList = _previewSeats.value.toMutableList()
        if (position in currentList.indices) {
            val seat = currentList[position]
            val nextType = when (seat.seatType) {
                "standard" -> "vip"
                "vip" -> "couple"
                else -> "standard"
            }
            currentList[position] = seat.copy(seatType = nextType)
            _previewSeats.value = currentList
        }
    }

    fun loadRoom(id: String) {
        roomId.value = id
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val room = repository.getRoomDetails(id)
                if (room != null) {
                    isPrefilling = true
                    roomName.value = room.roomName
                    roomType.value = room.roomType ?: "Standard"
                    screenType.value = room.screenType ?: "2D"
                    rowsStr.value = room.rows.toString()
                    columnsStr.value = room.columns.toString()
                    status.value = room.status ?: "active"

                    val seats = repository.getRoomSeats(id)
                    if (seats.isNotEmpty()) {
                        _previewSeats.value = seats
                    }
                    kotlinx.coroutines.delay(100)
                    isPrefilling = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load room details"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveRoom(cinemaId: String) {
        val name = roomName.value.trim()
        val r = rowsStr.value.toIntOrNull()
        val c = columnsStr.value.toIntOrNull()
        val type = roomType.value
        val screen = screenType.value
        val currentStatus = status.value
        val configuredSeats = _previewSeats.value

        if (name.isEmpty()) {
            _errorMessage.value = "Room name is required"
            return
        }
        if (r == null || r < 1 || r > 8) {
            _errorMessage.value = "Rows must be between 1 and 8."
            return
        }
        if (c == null || c < 1 || c > 10) {
            _errorMessage.value = "Columns must be between 1 and 10."
            return
        }
        if (configuredSeats.isEmpty()) {
            _errorMessage.value = "Seat layout is empty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val seatInputs = configuredSeats.map {
                    SeatInput(seatName = it.seatName, seatType = it.seatType)
                }
                val request = AddRoomRequest(
                    cinemaId = cinemaId,
                    roomName = name,
                    roomType = type,
                    screenType = screen,
                    rows = r,
                    columns = c,
                    capacity = r * c,
                    status = currentStatus,
                    seats = seatInputs
                )
                val currentRoomId = roomId.value
                val success = if (currentRoomId != null) {
                    repository.updateRoom(currentRoomId, request)
                } else {
                    repository.createRoom(request)
                }
                if (success) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value = if (currentRoomId != null) "Unable to update cinema room. Make sure the name is unique." else "Unable to create cinema room. Make sure the name is unique."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to save cinema room"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
