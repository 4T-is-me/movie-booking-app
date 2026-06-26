package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.RepositoryProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * MovieViewModel: Back to Singleton approach as per BE team.
 */
class MovieViewModel : ViewModel() {

    private val repository: MovieRepository = RepositoryProvider.provideMovieRepository()

    private val _searchQuery = MutableStateFlow("")
    
    private val _homeItems = MutableStateFlow<List<HomeItem>>(emptyList())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private val _isServerConnected = MutableStateFlow<Boolean?>(null)
    val isServerConnected = _isServerConnected.asStateFlow()

    init {
        loadHomeData()
        checkServerConnection()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            combine(
                repository.getMovies(),
                repository.getPromos(),
                repository.getServices(),
                repository.getNews(),
                _searchQuery
            ) { movies, promos, services, news, query ->
                val filteredMovies = if (query.isEmpty()) {
                    movies
                } else {
                    movies.filter { 
                        it.nameEn.contains(query, ignoreCase = true) || 
                        it.nameVn.contains(query, ignoreCase = true) 
                    }
                }

                if (filteredMovies.isEmpty() && query.isNotEmpty()) {
                    listOf(HomeItem.HeaderSection("User"), HomeItem.EmptyState)
                } else {
                    val list = mutableListOf<HomeItem>()
                    list.add(HomeItem.HeaderSection("User"))
                    
                    val nowPlaying = filteredMovies.filter { it.isNowPlaying }
                    if (nowPlaying.isNotEmpty()) list.add(HomeItem.NowPlayingSection(nowPlaying))
                    
                    val comingSoon = filteredMovies.filter { !it.isNowPlaying }
                    if (comingSoon.isNotEmpty()) list.add(HomeItem.ComingSoonSection(comingSoon))
                    
                    if (promos.isNotEmpty() && query.isEmpty()) list.add(HomeItem.PromoSection(promos))
                    if (services.isNotEmpty() && query.isEmpty()) list.add(HomeItem.ServiceSection(services))
                    if (news.isNotEmpty() && query.isEmpty()) list.add(HomeItem.MovieNewsSection(news))
                    
                    list
                }
            }.collect {
                _homeItems.value = it
            }
        }
    }

    fun searchMovies(query: String) {
        _searchQuery.value = query
    }

    fun checkServerConnection() {
        viewModelScope.launch {
            try {
                repository.getMovies()
                    .catch { _isServerConnected.value = false }
                    .collect { 
                        _isServerConnected.value = true 
                    }
            } catch (e: Exception) {
                _isServerConnected.value = false
            }
        }
    }
}
