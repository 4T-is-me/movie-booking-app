package com.example.movie_booking_app.utils

object Constants {
    /**
     * GLOBAL SWITCH:
     * true -> Use Mock Data (Stable for UI development according to Figma)
     * false -> Use Real Server/DB (For testing connectivity and merging code)
     */
    const val useMockData = false

    /**
     * Updated to your machine's actual IPv4: 192.168.1.184
     * Synced with group's server requirements.
     */
    const val BASE_URL = "http://192.168.1.184:5000/"
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

    val STATUS_PAYROLL = listOf("All Status", "Draft", "Paid")

    object MovieStatus {
        const val ACTIVE = "active"
        const val INACTIVE = "inactive"
        const val DELETED = "deleted"
        const val SHOWING = "showing"
        const val COMING_SOON = "coming"
        const val SOON = "soon"
        const val UPCOMING = "upcoming"
    }

    object SeatConstants {
        const val TYPE_REGULAR = "Regular"
        const val TYPE_VIP = "VIP"
        const val TYPE_COUPLE = "Couple"
    }

    object BookingConfig {
        const val HOLD_TIME_SECONDS = 600L
        const val DEBOUNCE_SEARCH_MS = 500L
        const val DEFAULT_MOVIE_DURATION = 120
    }

    object FoodCategory {
        const val ALL = "All"
        const val COMBO = "combo"
        const val DRINK = "drink"
        const val FOOD = "food"
    }
}



