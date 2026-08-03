package com.example.movie_booking_app.viewmodel.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.api.UpdateRoomRequest
import com.example.movie_booking_app.data.model.response.ManagerRoom
import com.example.movie_booking_app.data.model.response.ManagerSeatRow
import com.example.movie_booking_app.data.repository.ManagerRoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ManagerRoomViewModel @Inject constructor(
    private val repository: ManagerRoomRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<ManagerRoom>>(emptyList())
    val rooms: StateFlow<List<ManagerRoom>> = _rooms.asStateFlow()

    private val _seats = MutableStateFlow<List<ManagerSeatRow>>(emptyList())
    val seats: StateFlow<List<ManagerSeatRow>> = _seats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRooms().collect { response ->
                _rooms.value = response?.data ?: emptyList()
                _isLoading.value = false
            }
        }
    }

    fun loadRoomSeats(roomId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getRoomSeats(roomId).collect { response ->
                _seats.value = response?.data ?: emptyList()
                _isLoading.value = false
            }
        }
    }

    fun updateRoom(roomId: String, request: UpdateRoomRequest, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateRoom(roomId, request).collect { success ->
                _isLoading.value = false
                onResult(success)
            }
        }
    }

    fun updateSeatStatus(roomId: String, seatId: String, status: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateSeatStatus(roomId, seatId, status).collect { success ->
                _isLoading.value = false
                if (success) {
                    loadRoomSeats(roomId)
                }
                onResult(success)
            }
        }
    }

    fun bulkUpdateSeatStatus(roomId: String, seatIds: List<String>, status: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.bulkUpdateSeatStatus(roomId, seatIds, status).collect { success ->
                _isLoading.value = false
                if (success) {
                    loadRoomSeats(roomId)
                }
                onResult(success)
            }
        }
    }
}
