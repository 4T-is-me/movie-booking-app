package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.api.AdminApiService
import com.example.movie_booking_app.data.api.AuthApi
import com.example.movie_booking_app.data.api.BookingApi
import com.example.movie_booking_app.data.api.CinemaRoomApi
import com.example.movie_booking_app.data.api.MovieApi
import com.example.movie_booking_app.data.api.PaymentApi
import com.example.movie_booking_app.data.api.ProfileApi
import com.example.movie_booking_app.data.api.RetrofitClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMovieApi(): MovieApi {
        return RetrofitClient.movieApi
    }

    @Provides
    @Singleton
    fun provideCinemaRoomApi(): CinemaRoomApi {
        return RetrofitClient.cinemaRoomApi
    }

    @Provides
    @Singleton
    fun provideAuthApi(): AuthApi {
        return RetrofitClient.authApi
    }

    @Provides
    @Singleton
    fun provideProfileApi(): ProfileApi {
        return RetrofitClient.profileApi
    }

    @Provides
    @Singleton
    fun providePaymentApi(): PaymentApi {
        return RetrofitClient.paymentApi
    }

    @Provides
    @Singleton
    fun provideAdminApi(): AdminApiService {
        return RetrofitClient.adminApi
    }

    @Provides
    @Singleton
    fun provideBookingApi(): BookingApi {
        return RetrofitClient.bookingApi
    }
}
