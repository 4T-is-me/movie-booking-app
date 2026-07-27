package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.ReviewItem
import com.example.movie_booking_app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffRatingViewModel @Inject constructor(
    private val repository: StaffRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _ratings = MutableStateFlow<List<ReviewItem>>(emptyList())
    val ratings: StateFlow<List<ReviewItem>> = _ratings.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _totalRating = MutableStateFlow(0.0)
    val totalRating: StateFlow<Double> = _totalRating.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    fun loadRatings() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getMyRatings().collect { response ->
                    if (response != null && response.success == true) {
                        val items = response.data ?: emptyList()
                        android.util.Log.d("StaffRating", "Data loaded: ${items.size} items")
                        _ratings.value = items
                        
                        if (items.isNotEmpty()) {
                            val validRatings = items.mapNotNull { it.rating }
                            if (validRatings.isNotEmpty()) {
                                _totalRating.value = validRatings.average()
                                _totalCount.value = items.size
                            } else {
                                _totalRating.value = 0.0
                                _totalCount.value = items.size
                            }
                        } else {
                            _totalRating.value = 0.0
                            _totalCount.value = 0
                        }
                    } else {
                        android.util.Log.e("StaffRating", "Response error or null: $response")
                        _error.value = "Failed to load ratings or no ratings yet"
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("StaffRating", "Exception: ${e.message}")
                _isLoading.value = false
                _error.value = e.localizedMessage ?: "Network error"
            }
        }
    }
}
