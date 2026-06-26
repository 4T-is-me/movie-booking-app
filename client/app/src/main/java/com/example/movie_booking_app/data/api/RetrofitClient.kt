package com.example.movie_booking_app.data.api

import com.example.movie_booking_app.utils.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton Retrofit Client as requested by BE team.
 */
object RetrofitClient {
    
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val movieApi: MovieApi by lazy {
        retrofit.create(MovieApi::class.java)
    }
    
    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}
