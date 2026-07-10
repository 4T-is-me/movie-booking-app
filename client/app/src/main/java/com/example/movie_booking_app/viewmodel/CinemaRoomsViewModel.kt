package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.CinemaRoom
import com.example.movie_booking_app.data.repository.CinemaRoomsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CinemaRoomsViewModel @Inject constructor(
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

    fun loadRooms() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getRooms().collect { list ->
                    _rooms.value = list
                    filterRooms(_searchQuery.value)
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cinema rooms"
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
