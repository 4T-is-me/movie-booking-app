package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatType
import com.example.movie_booking_app.data.repository.CinemaRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AdminRoomViewModel @Inject constructor(
    private val repository: CinemaRoomRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<CinemaRoom>>(emptyList())
    val rooms: StateFlow<List<CinemaRoom>> = _rooms.asStateFlow()

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRooms().collect {
                _rooms.value = it
                _isLoading.value = false
            }
        }
    }

    fun loadRoomSeats(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRoomSeats(roomId).collect {
                _seats.value = it
                _isLoading.value = false
            }
        }
    }

    fun toggleSeatType(seat: Seat) {
        val newType = if (seat.type == SeatType.NORMAL) SeatType.VIP else SeatType.NORMAL
        viewModelScope.launch {
            val success = repository.updateSeatType(seat.id, newType)
            if (success) {
                // Update local state
                val updatedSeats = _seats.value.map {
                    if (it.id == seat.id) it.copy(type = newType) else it
                }
                _seats.value = updatedSeats
            }
        }
    }
}
