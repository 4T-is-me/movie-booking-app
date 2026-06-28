package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookingRepositoryImpl : BookingRepository {

    override fun getSeats(showtimeId: String): Flow<List<Seat>> = flow {
        val seats = mutableListOf<Seat>()
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K")
        for (row in rows) {
            for (col in 1..10) {
                val type = if (row in listOf("E", "F", "G")) SeatType.VIP else SeatType.NORMAL
                val status = if (col % 7 == 0) SeatStatus.SOLD else SeatStatus.AVAILABLE
                seats.add(Seat(id = "$row$col", row = row, col = col, type = type, status = status))
            }
        }
        emit(seats)
    }

    override fun getShowDates(movieId: String): Flow<List<ShowDate>> = flow {
        emit(listOf(
            ShowDate("24", "Mon", "Jun", "2024", true),
            ShowDate("25", "Tue", "Jun", "2024"),
            ShowDate("26", "Wed", "Jun", "2024"),
            ShowDate("27", "Thu", "Jun", "2024"),
            ShowDate("28", "Fri", "Jun", "2024"),
            ShowDate("29", "Sat", "Jun", "2024")
        ))
    }

    override fun getShowTimes(movieId: String, date: String): Flow<List<ShowTime>> = flow {
        emit(listOf(
            ShowTime("09:00", true),
            ShowTime("12:00"),
            ShowTime("15:30"),
            ShowTime("18:00"),
            ShowTime("21:00")
        ))
    }
}
