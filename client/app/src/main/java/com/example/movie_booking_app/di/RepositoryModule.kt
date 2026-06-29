package com.example.movie_booking_app.di

import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.MovieRepositoryRemoteImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn

@Module
abstract class RepositoryModule {

    @Binds
    abstract fun bindMovieRepository(
        movieRepositoryRemoteImpl: MovieRepositoryRemoteImpl
    ): MovieRepository
}
