package com.example.movie_booking_app.data.repository

import com.example.movie_booking_app.data.model.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import javax.inject.Inject

class BookingRepositoryImpl @Inject constructor() : BookingRepository {

    override fun getSeats(date: String, time: String): Flow<List<Seat>> = flow {
        val seats = mutableListOf<Seat>()
        val rows = listOf("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K")
        
        // MIX DATE AND TIME: Create a unique seed for each combination
        val uniqueKey = "$date-$time"
        val seed = uniqueKey.hashCode().toLong()
        val random = Random(seed)

        for (row in rows) {
            for (col in 1..10) {
                val type = if (row in listOf("E", "F", "G")) SeatType.VIP else SeatType.NORMAL
                // The random distribution will now be different for June 24th vs June 25th
                val status = if (random.nextInt(100) < 20) SeatStatus.RESERVED else SeatStatus.AVAILABLE
                seats.add(Seat(id = "$uniqueKey-$row$col", row = row, number = col, type = type, status = status))
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

    override fun getShowTimes(movieId: String, date: String): Flow<List<TimeSlot>> = flow {
        // We could even return different times for different dates if we wanted
        emit(listOf(
            TimeSlot("09:00", true),
            TimeSlot("12:00"),
            TimeSlot("15:30"),
            TimeSlot("18:00"),
            TimeSlot("21:00")
        ))
    }
}
