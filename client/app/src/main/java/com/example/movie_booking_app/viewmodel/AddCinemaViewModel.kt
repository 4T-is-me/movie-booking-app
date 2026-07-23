package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.AddCinemaRequest
import com.example.movie_booking_app.data.repository.CinemasRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

@HiltViewModel
class AddCinemaViewModel @Inject constructor(
    private val repository: CinemasRepository
) : ViewModel() {

    val cinemaId = MutableStateFlow<String?>(null)
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

    fun loadCinema(id: String) {
        cinemaId.value = id
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val cinema = repository.getCinemaDetails(id)
                if (cinema != null) {
                    cinemaName.value = cinema.cinemaName.orEmpty()
                    address.value = cinema.address.orEmpty()
                    city.value = cinema.city.orEmpty()
                    hotline.value = cinema.hotline.orEmpty()
                    status.value = cinema.status.orEmpty()
                    image.value = cinema.image.orEmpty()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load cinema details"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCinema(posterPart: MultipartBody.Part?) {
        val name = cinemaName.value.trim()
        val addr = address.value.trim()
        val cit = city.value.trim()
        val hot = hotline.value.trim()
        val stat = status.value
        val img = image.value.trim()

        if (name.isEmpty()) {
            _errorMessage.value = "Cinema name is required"
            return
        }
        if (addr.isEmpty()) {
            _errorMessage.value = "Address is required"
            return
        }
        if (cit.isEmpty()) {
            _errorMessage.value = "City/Province is required"
            return
        }
        if (hot.isEmpty()) {
            _errorMessage.value = "Hotline/Phone number is required"
            return
        }
        val phoneRegex = Regex("^[0-9\\-\\+\\s]{8,15}$")
        if (!phoneRegex.matches(hot)) {
            _errorMessage.value = "Invalid phone number format"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val currentId = cinemaId.value
                val result = if (currentId != null) {
                    if (posterPart != null) {
                        repository.updateCinemaWithImage(
                            currentId,
                            name.toRequestBody("text/plain".toMediaTypeOrNull()),
                            addr.toRequestBody("text/plain".toMediaTypeOrNull()),
                            cit.toRequestBody("text/plain".toMediaTypeOrNull()),
                            hot.toRequestBody("text/plain".toMediaTypeOrNull()),
                            stat.toRequestBody("text/plain".toMediaTypeOrNull()),
                            posterPart
                        )
                    } else {
                        val request = AddCinemaRequest(
                            cinemaName = name,
                            address = addr,
                            city = cit,
                            hotline = hot,
                            status = stat,
                            image = if (img.isEmpty()) null else img
                        )
                        repository.updateCinema(currentId, request)
                    }
                } else {
                    if (posterPart != null) {
                        repository.createCinemaWithImage(
                            name.toRequestBody("text/plain".toMediaTypeOrNull()),
                            addr.toRequestBody("text/plain".toMediaTypeOrNull()),
                            cit.toRequestBody("text/plain".toMediaTypeOrNull()),
                            hot.toRequestBody("text/plain".toMediaTypeOrNull()),
                            stat.toRequestBody("text/plain".toMediaTypeOrNull()),
                            posterPart
                        )
                    } else {
                        val request = AddCinemaRequest(
                            cinemaName = name,
                            address = addr,
                            city = cit,
                            hotline = hot,
                            status = stat,
                            image = if (img.isEmpty()) null else img
                        )
                        repository.createCinema(request)
                    }
                }

                if (result.isSuccess) {
                    _isSuccess.value = true
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: (if (currentId != null) "Failed to update cinema" else "Failed to create cinema")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
