package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.bookingApi
import com.example.movie_booking_app.data.repository.*
import com.example.movie_booking_app.utils.constants
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
        return if (constants.USE_MOCK_DATA) {
            BookingRepositoryImpl()
        } else {
            BookingRepositoryRemoteImpl(api)
        }
    }
}
