package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.repository.CinemasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminCinemasViewModel @Inject constructor(
    private val repository: CinemasRepository
) : ViewModel() {

    private val _cinemas = MutableStateFlow<List<Cinema>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    
    val filteredCinemas = combine(_cinemas, _searchQuery) { cinemas, query ->
        if (query.isBlank()) {
            cinemas
        } else {
            cinemas.filter { it.cinemaName?.contains(query, ignoreCase = true) == true }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun loadCinemas() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val list = repository.getCinemas()
                _cinemas.value = list
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load cinemas"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCinemaStatus(cinemaId: String, currentStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val newStatus = if (currentStatus.lowercase() == "active") "inactive" else "active"
                val success = repository.updateCinemaStatus(cinemaId, newStatus)
                if (success) {
                    loadCinemas()
                } else {
                    _error.value = "Failed to update status"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update status"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
