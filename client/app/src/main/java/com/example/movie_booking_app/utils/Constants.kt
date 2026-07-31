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
    const val BASE_URL = "http://192.168.32.129:5000/"
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

    object Movie {
        const val EXTRA_MOVIE_DATA = "movie"
        const val MIME_TYPE_IMAGE = "image/*"
        const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
        const val DATE_FORMAT_SERVER = "yyyy-MM-dd"
        const val PREFIX_POSTER_FILE = "movie_poster_"
        const val SUFFIX_JPG = ".jpg"
        const val STATUS_ACTIVE = "active"
        const val STATUS_DELETED = "deleted"

        const val EXTRA_MOVIE_ID = "movieId"
        const val EXTRA_MOVIE_TITLE = "movieTitle"
        const val EXTRA_MOVIE_POSTER = "moviePoster"
        const val EXTRA_MOVIE_GENRE = "movieGenre"

        const val MIN_COMMENT_LENGTH = 5
        const val MAX_COMMENT_LENGTH = 1000

        const val SEPARATOR_DASH = "-"
        const val SEPARATOR_SLASH = "/"
    }
}



