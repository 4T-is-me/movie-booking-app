package com.example.movie_booking_app.utils

object Constants {
    /**
     * GLOBAL SWITCH:
     * true -> Use Mock Data (Stable for UI development according to Figma)
     * false -> Use Real Server/DB (For testing connectivity and merging code)
     */
    const val useMockData = false

    /**
     * Updated to your machine's actual IPv4: 192.168.88.100
     * Synced with group's server requirements.
     */
    const val BASE_URL = "http://10.41.79.56:5000/"
    const val RANK_GOLD = 10000000L
    const val RANK_SILVER = 5000000L

    const val MORNING = "08:00AM - 12:00PM"
    const val AFTERNOON = "12:00PM - 04:00PM"
    const val EVENING = "04:00PM - 08:00PM"
    const val NIGHT = "08:00PM - 12:00AM"
    const val FULLTIME = "08h–22h"
    const val MAX_SHIFT_OF_DATE = 4
    val SHIFTS = listOf("Morning", "Afternoon", "Evening", "Night")
    val STATUS_SHIFT = listOf("Active","Assigned", "Completed", "Absent", "NeedReplacement")

    val STATUS_REQUEST = listOf("pending", "approved", "rejected")

    val  ERROR_DUPLICATE_SHIFT =  "ERROR_DUPLICATE_SHIFT"
    val ERROR_UNKNOWN = "ERROR_UNKNOWN"
    object ComboPrice {
        const val SINGLE_COMBO = 80_000
        const val DOUBLE_COMBO = 120_000
    }
}



