package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.data.model.entity.showDate
import com.example.movie_booking_app.data.model.entity.showTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class selectSeatViewModel : ViewModel() {

    private val _seats = MutableStateFlow<List<Seat>>(emptyList())
    val seats: StateFlow<List<Seat>> = _seats.asStateFlow()

    private val _dates = MutableStateFlow<List<showDate>>(emptyList())
    val dates: StateFlow<List<showDate>> = _dates.asStateFlow()

    private val _times = MutableStateFlow<List<showTime>>(emptyList())
    val times: StateFlow<List<showTime>> = _times.asStateFlow()

    private val _totalPrice = MutableStateFlow(0)
    val totalPrice: StateFlow<Int> = _totalPrice.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Mock data for seats (A-J rows, 1-10 columns)
        val initialSeats = mutableListOf<Seat>()
        for (row in 'A'..'J') {
            for (col in 1..10) {
                val status = if (row == 'C' && col in 4..6) SeatStatus.RESERVED else SeatStatus.AVAILABLE
                initialSeats.add(Seat(id = "$row$col", row = row.toString(), number = col, status = status))
            }
        }
        _seats.value = initialSeats

        // Mock dates
        _dates.value = listOf(
            showDate("1", "Dec", "10", true),
            showDate("2", "Dec", "11"),
            showDate("3", "Dec", "12"),
            showDate("4", "Dec", "13"),
            showDate("5", "Dec", "14"),
            showDate("6", "Dec", "15"),
            showDate("7", "Dec", "16"),
            showDate("8", "Dec", "17"),
            showDate("9", "Dec", "18"),
            showDate("10", "Dec", "19")
        )

        // Mock times
        _times.value = listOf(
            showTime("1", "12:10"),
            showTime("2", "14:15", true),
            showTime("3", "16:20"),
            showTime("4", "18:25"),
            showTime("5", "20:30")
        )

        calculateTotalPrice()
    }

    fun toggleSeatSelection(seatId: String) {
        _seats.update { list ->
            list.map { seat ->
                if (seat.id == seatId) {
                    when (seat.status) {
                        SeatStatus.AVAILABLE -> seat.copy(status = SeatStatus.SELECTED)
                        SeatStatus.SELECTED -> seat.copy(status = SeatStatus.AVAILABLE)
                        else -> seat
                    }
                } else {
                    seat
                }
            }
        }
        calculateTotalPrice()
    }

    fun selectDate(dateId: String) {
        _dates.update { list ->
            list.map { it.copy(isSelected = it.id == dateId) }
        }
    }

    fun selectTime(timeId: String) {
        _times.update { list ->
            list.map { it.copy(isSelected = it.id == timeId) }
        }
    }

    private fun calculateTotalPrice() {
        val total = _seats.value.filter { it.status == SeatStatus.SELECTED }.sumOf { it.price }
        _totalPrice.value = total
    }
}
