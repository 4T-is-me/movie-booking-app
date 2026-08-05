package com.example.movie_booking_app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.data.repository.MyTicketRepository
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyTicketViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    
    private val repository: MyTicketRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _tickets = MutableStateFlow<List<BookingHistoryItem>>(emptyList())
    val tickets: StateFlow<List<BookingHistoryItem>> = _tickets.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _rateSuccess = MutableSharedFlow<Boolean>()
    val rateSuccess: SharedFlow<Boolean> = _rateSuccess.asSharedFlow()

    private val _rateErrorMessage = MutableSharedFlow<String?>()
    val rateErrorMessage: SharedFlow<String?> = _rateErrorMessage.asSharedFlow()

    fun loadActiveTickets() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.getMyTickets().collect { response ->
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.success) {
                            _tickets.value = body.data ?: emptyList()
                        } else {
                            _error.value = context.getString(R.string.auto_str_failed_to_load_active_tickets)
                        }
                    } else {
                        _error.value = context.getString(R.string.error_server_code, response.code().toString())
                    }
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = context.getString(R.string.error_network_localized, e.localizedMessage ?: context.getString(R.string.error_unknown))
            }
        }
    }

    fun rateStaff(bookingId: String, rating: Int, comment: String? = null) {
        viewModelScope.launch {
            try {
                val response = repository.rateStaff(bookingId, rating, comment)
                if (response.isSuccessful) {
                    _rateSuccess.emit(true)
                    loadActiveTickets()
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val message = try {
                        val json = JsonParser.parseString(errorBody).asJsonObject
                        json.get("message")?.asString ?: context.getString(R.string.error_server_code, response.code().toString())
                    } catch (_: Exception) {
                        context.getString(R.string.error_server_code, response.code().toString())
                    }
                    _rateErrorMessage.emit(message)
                }
            } catch (e: Exception) {
                _rateErrorMessage.emit(context.getString(R.string.error_network_localized, e.localizedMessage))
            }
        }
    }

    fun resetError() {
        _error.value = null
    }
}
