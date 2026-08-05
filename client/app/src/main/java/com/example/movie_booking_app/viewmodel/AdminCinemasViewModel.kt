package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.repository.CinemasRepository
import com.example.movie_booking_app.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AdminCinemasViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
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
                _error.value = e.message ?: context.getString(R.string.error_load_cinemas)
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
                val newStatus = if (currentStatus.lowercase() == Constants.Status.ACTIVE.lowercase()) Constants.Status.INACTIVE.lowercase() else Constants.Status.ACTIVE.lowercase()
                val success = repository.updateCinemaStatus(cinemaId, newStatus)
                if (success) {
                    loadCinemas()
                } else {
                    _error.value = context.getString(R.string.auto_str_failed_to_update_status)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: context.getString(R.string.error_update_status)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
