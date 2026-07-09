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
}
