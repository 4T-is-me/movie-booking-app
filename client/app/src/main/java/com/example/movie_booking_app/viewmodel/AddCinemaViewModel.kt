package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import com.example.movie_booking_app.data.repository.CinemasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCinemaViewModel @Inject constructor(
    private val repository: CinemasRepository
) : ViewModel() {

    val cinemaName = MutableStateFlow("")
    val address = MutableStateFlow("")
    val city = MutableStateFlow("")
    val hotline = MutableStateFlow("")
    val status = MutableStateFlow("active")
    val image = MutableStateFlow("")

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess = _isSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun saveCinema() {
        val name = cinemaName.value.trim()
        val addr = address.value.trim()
        val cit = city.value.trim()
        val hot = hotline.value.trim()
        val stat = status.value
        val img = image.value.trim()

        if (name.isEmpty() || addr.isEmpty() || cit.isEmpty() || hot.isEmpty()) {
            _errorMessage.value = "Please fill in all required fields"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val request = AddCinemaRequest(
                    cinemaName = name,
                    address = addr,
                    city = cit,
                    hotline = hot,
                    status = stat,
                    image = if (img.isEmpty()) null else img
                )
                val success = repository.createCinema(request)
                if (success) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value = "Failed to create cinema"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
