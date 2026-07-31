package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.repository.BookingRepository
import com.example.movie_booking_app.data.repository.BookingRepositoryRemoteImpl
import com.example.movie_booking_app.data.repository.CinemaRoomRepository
import com.example.movie_booking_app.data.repository.CinemaRoomRepositoryImpl
import com.example.movie_booking_app.data.repository.CinemaRoomsRepository
import com.example.movie_booking_app.data.repository.CinemaRoomsRepositoryImpl
import com.example.movie_booking_app.data.repository.CinemasRepository
import com.example.movie_booking_app.data.repository.CinemasRepositoryImpl
import com.example.movie_booking_app.data.repository.FoodRepository
import com.example.movie_booking_app.data.repository.FoodRepositoryImpl
import com.example.movie_booking_app.data.repository.InventoryRepository
import com.example.movie_booking_app.data.repository.InventoryRepositoryImpl
import com.example.movie_booking_app.data.repository.MemberRepository
import com.example.movie_booking_app.data.repository.MemberRepositoryImpl
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.MovieRepositoryRemoteImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(
        movieRepositoryRemoteImpl: MovieRepositoryRemoteImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindCinemaRoomRepository(
        impl: CinemaRoomRepositoryImpl
    ): CinemaRoomRepository

    @Binds
    @Singleton
    abstract fun bindCinemaRoomsRepository(
        impl: CinemaRoomsRepositoryImpl
    ): CinemaRoomsRepository

    @Binds
    @Singleton
    abstract fun bindCinemasRepository(
        impl: CinemasRepositoryImpl
    ): CinemasRepository

    @Binds
    @Singleton
    abstract fun bindBookingRepository(
        impl: BookingRepositoryRemoteImpl // Must be RemoteImpl to call actual API
    ): BookingRepository

    @Binds
    @Singleton
    abstract fun bindMemberRepository(
        impl: MemberRepositoryImpl
    ): MemberRepository

    @Binds
    @Singleton
    abstract fun bindFoodRepository(
        impl: FoodRepositoryImpl
    ): FoodRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        impl: InventoryRepositoryImpl
    ): InventoryRepository
}
