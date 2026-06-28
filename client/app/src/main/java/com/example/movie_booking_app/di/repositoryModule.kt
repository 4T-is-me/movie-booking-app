package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.bookingApi
import com.example.movie_booking_app.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object repositoryModule {

    @Provides
    @Singleton
    fun provideBookingRepository(api: bookingApi): BookingRepository {
        // --- MANUAL SWAP AREA ---
        
        // 1. Production Mode (Use for submission)
        return BookingRepositoryRemoteImpl(api)
        
        // 2. Mock Mode (Use for local UI testing)
        // return BookingRepositoryImpl()
        
        // -------------------------
    }
}
