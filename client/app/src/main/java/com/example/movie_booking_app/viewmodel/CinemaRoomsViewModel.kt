package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.repository.CinemaRoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@HiltViewModel
class CinemaRoomsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
    private val repository: CinemaRoomsRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<CinemaRoom>>(emptyList())
    val rooms: StateFlow<List<CinemaRoom>> = _rooms.asStateFlow()

    private val _filteredRooms = MutableStateFlow<List<CinemaRoom>>(emptyList())
    val filteredRooms: StateFlow<List<CinemaRoom>> = _filteredRooms.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe search query and filter rooms
        viewModelScope.launch {
            _searchQuery.collectLatest { query ->
                filterRooms(query)
            }
        }
    }

    fun loadRooms(cinemaId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getRooms(cinemaId).collect { list ->
                    _rooms.value = list
                    filterRooms(_searchQuery.value)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.error_load_cinema_rooms)
                _isLoading.value = false
            }
        }
    }

    fun toggleRoomStatus(roomId: String, currentStatus: String, cinemaId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val newStatus = if (currentStatus.lowercase() == "active") "inactive" else "active"
                val success = repository.updateRoomStatus(roomId, newStatus)
                if (success) {
                    loadRooms(cinemaId)
                } else {
                    _error.value = context.getString(R.string.auto_str_failed_to_update_room_status)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.error_update_room_status)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun filterRooms(query: String) {
        val allRooms = _rooms.value
        if (query.isBlank()) {
            _filteredRooms.value = allRooms
        } else {
            val lowerQuery = query.trim().lowercase()
            _filteredRooms.value = allRooms.filter { room ->
                room.roomName.lowercase().contains(lowerQuery) ||
                (room.roomType?.lowercase()?.contains(lowerQuery) == true)
            }
        }
    }
}
