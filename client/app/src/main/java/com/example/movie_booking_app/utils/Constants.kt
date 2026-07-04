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
    const val BASE_URL = "http://192.168.9.49:5000/"
}
