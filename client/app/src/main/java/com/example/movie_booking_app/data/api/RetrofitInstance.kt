package com.example.movie_booking_app.data.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * RetrofitInstance: Configured for local Node.js server communication.
 */
object RetrofitInstance {
    
    /**
     * 10.0.2.2 is the special IP for Android Emulator to reach PC's localhost.
     * Ensure the BASE_URL ends with a trailing slash.
     */
    private const val BASE_URL = "http://10.0.2.2:5000/"

    val movieApi: MovieApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
}
