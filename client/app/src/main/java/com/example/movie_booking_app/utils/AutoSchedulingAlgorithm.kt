package com.example.movie_booking_app.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter

data class MovieDraft(
    val _id: String,
    val movieName: String,
    val duration: Int,
    val status: String,
    val tier: Int
)

data class RoomDraft(
    val _id: String,
    val roomName: String,
    val cinema_id: String,
    val status: String
)

data class CinemaConfigDraft(
    val cleaning_time_minutes: Int
)

data class ShowtimeDraft(
    val cinema_id: String,
    val room_id: String,
    val room_name: String,
    val movie_id: String,
    val movie_name: String,
    val show_date: String,
    val start_time: String,
    val end_time: String,
    val status: String = "draft",
    val is_auto_generated: Boolean = true
)

object AutoSchedulingAlgorithm {

    fun generateWeeklySchedule(
        startDate: LocalDate,
        endDate: LocalDate,
        movies: List<MovieDraft>,
        rooms: List<RoomDraft>,
        config: CinemaConfigDraft,
        holidayDates: List<String> = emptyList(),
        existingShowtimes: List<ShowtimeDraft> = emptyList()
    ): List<ShowtimeDraft> {
        val activeMovies = movies.filter { it.status == "showing" }
        val activeRooms = rooms.filter { it.status == "active" }
        val result = mutableListOf<ShowtimeDraft>()

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val isHoliday = holidayDates.contains(currentDate.toString())
            val existingForDay = existingShowtimes.filter { it.show_date == currentDate.toString() }
            val dailySchedule = generateDailySchedule(currentDate, activeMovies, activeRooms, config, isHoliday, existingForDay)
            result.addAll(dailySchedule)
            currentDate = currentDate.plusDays(1)
        }
        return result
    }

    private fun generateDailySchedule(
        date: LocalDate,
        movies: List<MovieDraft>,
        rooms: List<RoomDraft>,
        config: CinemaConfigDraft,
        isHoliday: Boolean,
        existingShowtimesForDay: List<ShowtimeDraft>
    ): List<ShowtimeDraft> {
        // Block completely if the scheduled date is in the past
        if (date.isBefore(LocalDate.now())) {
            return emptyList()
        }

        val isWeekend = date.dayOfWeek.value == 6 || date.dayOfWeek.value == 7 || isHoliday
        val isHighSeason = date.month == Month.JUNE || date.month == Month.JULY || date.month == Month.AUGUST
        
        val result = mutableListOf<ShowtimeDraft>()
        val nowTime = LocalTime.now()
        val isToday = date == LocalDate.now()

        // Helper function to adjust slot start time if it is today
        fun getAdjustedStart(defaultStart: LocalTime): LocalTime {
            return if (isToday && defaultStart.isBefore(nowTime)) {
                nowTime
            } else {
                defaultStart
            }
        }
        
        if (isWeekend) {
            // Rule B: Weekend allocation (Sat - Sun)
            // 08:00 - 00:00 Continuous Bin Packing, prioritize Tier 1
            val weekendStart = getAdjustedStart(LocalTime.of(8, 0))
            val weekendEnd = LocalTime.of(23, 59)
            if (weekendStart.isBefore(weekendEnd)) {
                for (room in rooms) {
                    fillSlot(room, date, weekendStart, weekendEnd, Int.MAX_VALUE, movies, config, result, existingShowtimesForDay, preferLowTier = false)
                }
            }
        } else {
            // Rule A: Weekday & Seasonal allocation (Mon - Fri)
            if (isHighSeason) {
                // High Season: 08:00 - 00:00
                // Morning (08:30 - 11:45): 2 slots (prefer low tier)
                // Afternoon (12:00 - 16:30): 2 slots (prefer low tier)
                // Evening (17:00 - 00:00): Full, prioritize Tier 1
                for (room in rooms) {
                    val morningStart = getAdjustedStart(LocalTime.of(8, 30))
                    val morningEnd = LocalTime.of(11, 45)
                    if (morningStart.isBefore(morningEnd)) {
                        fillSlot(room, date, morningStart, morningEnd, 2, movies, config, result, existingShowtimesForDay, preferLowTier = true)
                    }

                    val afternoonStart = getAdjustedStart(LocalTime.of(12, 0))
                    val afternoonEnd = LocalTime.of(16, 30)
                    if (afternoonStart.isBefore(afternoonEnd)) {
                        fillSlot(room, date, afternoonStart, afternoonEnd, 2, movies, config, result, existingShowtimesForDay, preferLowTier = true)
                    }

                    val eveningStart = getAdjustedStart(LocalTime.of(17, 0))
                    val eveningEnd = LocalTime.of(23, 59)
                    if (eveningStart.isBefore(eveningEnd)) {
                        fillSlot(room, date, eveningStart, eveningEnd, Int.MAX_VALUE, movies, config, result, existingShowtimesForDay, preferLowTier = false)
                    }
                }
            } else {
                // Low Season: 09:00 - 00:00
                // Morning (09:30 - 11:45): 1 slot (prefer low tier)
                // Afternoon (12:00 - 16:30): 2 slots (prefer low tier)
                // Evening (17:00 - 00:00): Full, prioritize Tier 1
                for (room in rooms) {
                    val morningStart = getAdjustedStart(LocalTime.of(9, 30))
                    val morningEnd = LocalTime.of(11, 45)
                    if (morningStart.isBefore(morningEnd)) {
                        fillSlot(room, date, morningStart, morningEnd, 1, movies, config, result, existingShowtimesForDay, preferLowTier = true)
                    }

                    val afternoonStart = getAdjustedStart(LocalTime.of(12, 0))
                    val afternoonEnd = LocalTime.of(16, 30)
                    if (afternoonStart.isBefore(afternoonEnd)) {
                        fillSlot(room, date, afternoonStart, afternoonEnd, 2, movies, config, result, existingShowtimesForDay, preferLowTier = true)
                    }

                    val eveningStart = getAdjustedStart(LocalTime.of(17, 0))
                    val eveningEnd = LocalTime.of(23, 59)
                    if (eveningStart.isBefore(eveningEnd)) {
                        fillSlot(room, date, eveningStart, eveningEnd, Int.MAX_VALUE, movies, config, result, existingShowtimesForDay, preferLowTier = false)
                    }
                }
            }
        }
        return result
    }

    private fun fillSlot(
        room: RoomDraft, date: LocalDate, start: LocalTime, end: LocalTime, maxSlots: Int,
        movies: List<MovieDraft>, config: CinemaConfigDraft, result: MutableList<ShowtimeDraft>,
        existingShowtimesForDay: List<ShowtimeDraft>,
        preferLowTier: Boolean
    ) {
        var currentDateTime = LocalDateTime.of(date, start)
        val endDateTimeLimit = LocalDateTime.of(date, end)
        
        var slots = 0
        while (currentDateTime.isBefore(endDateTimeLimit) && slots < maxSlots) {
            var movie: MovieDraft? = null
            var actualStartDT = currentDateTime
            
            // Try to find a non-conflicting movie by shifting start time in 15 min increments
            while (actualStartDT.isBefore(endDateTimeLimit)) {
                val combinedExisting = result + existingShowtimesForDay
                movie = selectBestMovie(actualStartDT.toLocalTime(), movies, combinedExisting, date, preferLowTier)
                if (movie != null) {
                    val endDT = actualStartDT.plusMinutes(movie.duration.toLong())
                    // Check if it fits the time block before ending
                    if (endDT.isBefore(endDateTimeLimit) || endDT == endDateTimeLimit) {
                        break
                    }
                    movie = null
                }
                
                if (movie == null) {
                    // Try to pick a shorter movie instead of advancing time
                    val allMovies = if (preferLowTier) movies.sortedByDescending { it.tier } else movies.sortedBy { it.tier }
                    movie = allMovies.firstOrNull { m ->
                        val hasConflict = combinedExisting.any { r ->
                            r.movie_id == m._id && r.show_date == date.toString() && isConflict(r.start_time, actualStartDT)
                        }
                        if (!hasConflict) {
                            val endDT = actualStartDT.plusMinutes(m.duration.toLong())
                            endDT.isBefore(endDateTimeLimit) || endDT == endDateTimeLimit
                        } else false
                    }
                    if (movie != null) break
                }
                
                actualStartDT = actualStartDT.plusMinutes(15)
            }
            
            if (movie == null) break // Cannot fit any movie in the remaining block
            
            val startDateTime = actualStartDT
            val endDateTime = startDateTime.plusMinutes(movie.duration.toLong())

            result.add(createDraft(room, movie, date, startDateTime, endDateTime))
            
            // Calculate next available time and round up to the nearest 5 minutes ("even number")
            val rawNextTime = endDateTime.plusMinutes(config.cleaning_time_minutes.toLong())
            val remainder = rawNextTime.minute % 5
            currentDateTime = if (remainder == 0) {
                rawNextTime
            } else {
                rawNextTime.plusMinutes((5 - remainder).toLong())
            }.withSecond(0).withNano(0)
            
            slots++
        }
    }

    private fun selectBestMovie(
        time: LocalTime, movies: List<MovieDraft>, existing: List<ShowtimeDraft>, 
        date: LocalDate, preferLowTier: Boolean
    ): MovieDraft? {
        val sortedMovies = if (preferLowTier) {
            movies.sortedByDescending { it.tier }
        } else {
            movies.sortedBy { it.tier }
        }
        
        for (movie in sortedMovies) {
            val hasConflict = existing.any { 
                it.movie_id == movie._id && 
                it.show_date == date.toString() &&
                isConflict(it.start_time, LocalDateTime.of(date, time))
            }
            if (!hasConflict) {
                return movie
            }
        }
        return null // Return null so the caller MUST delay the time instead of breaking the rule
    }

    private fun isConflict(existingStartTime: String, newStartTime: LocalDateTime): Boolean {
        try {
            val existingTime = LocalDateTime.parse(existingStartTime, DateTimeFormatter.ISO_DATE_TIME)
            val diff = Math.abs(java.time.Duration.between(existingTime, newStartTime).toMinutes())
            return diff < 15 // Staggering rule: min 15 mins diff across rooms
        } catch (e: Exception) {
            return false
        }
    }

    private fun createDraft(
        room: RoomDraft, movie: MovieDraft, date: LocalDate, start: LocalDateTime, end: LocalDateTime
    ): ShowtimeDraft {
        return ShowtimeDraft(
            cinema_id = room.cinema_id,
            room_id = room._id,
            room_name = room.roomName,
            movie_id = movie._id,
            movie_name = movie.movieName,
            show_date = date.toString(),
            start_time = start.format(DateTimeFormatter.ISO_DATE_TIME),
            end_time = end.format(DateTimeFormatter.ISO_DATE_TIME)
        )
    }
}
