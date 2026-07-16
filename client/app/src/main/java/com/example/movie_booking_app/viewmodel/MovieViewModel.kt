package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.response.MovieResponse
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.collections.emptyList

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItem>>(initialHomeState())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private val _adminMovies = MutableStateFlow<List<Movie>>(emptyList())
    val adminMovies: StateFlow<List<Movie>> = _adminMovies.asStateFlow()

    private val _movieDetail = MutableStateFlow<MovieResponse?>(null)
    val movieDetail: StateFlow<MovieResponse?> = _movieDetail.asStateFlow()
    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private val _movieShowtimes = MutableStateFlow<List<Showtime>>(emptyList())
    val movieShowtimes: StateFlow<List<Showtime>> = _movieShowtimes

    private val _showtimeError = MutableStateFlow<String?>(null)
    val showtimeError: StateFlow<String?> = _showtimeError

    // Combined admin action flows
    private val _movieActionSuccess = MutableSharedFlow<Boolean>()
    val movieActionSuccess = _movieActionSuccess.asSharedFlow()

    private val _movieActionMessage = MutableSharedFlow<String>()
    val movieActionMessage = _movieActionMessage.asSharedFlow()

    init {
        setupSearch()
        loadHomeData()
        loadAdminMovies()
    }

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    private fun setupSearch() {
        _searchQuery
            .debounce(500)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    loadHomeData()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.searchMovies(query).collect { movies ->
                if (movies.isEmpty()) {
                    _homeItems.value = listOf(HomeItem.EmptyState)
                } else {
                    _homeItems.value = listOf(HomeItem.NowPlayingSection(movies, isLoading = false))
                }
                _isLoading.value = false
            }
        }
    }

    fun loadAdminMovies(status: String? = null) {
        viewModelScope.launch {
            repository.getAdminMovies(status)
                .collect { movies ->
                    _adminMovies.value = movies
                }
        }
    }

    private fun initialHomeState(): List<HomeItem> = listOf(
        HomeItem.NowPlayingSection(null, isLoading = true),
        HomeItem.ComingSoonSection(null, isLoading = true),
        HomeItem.PromoSection(null, isLoading = true),
        HomeItem.ServiceSection(null, isLoading = true),
        HomeItem.MovieNewsSection(null, isLoading = true),
        HomeItem.VideosSection(null, isLoading = true)
    )

    fun loadHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            combine(
                repository.getMovies().onStart { emit(emptyList()) },
                repository.getPromos().onStart { emit(emptyList()) },
                repository.getServices().onStart { emit(emptyList()) },
                repository.getNews().onStart { emit(emptyList()) }
            ) { movies, promos, services, news ->
                val list = mutableListOf<HomeItem>()
                
                // Now Playing Section: Lọc linh hoạt tại Client
                val nowPlaying = movies.filter {
                    val s = it.status?.lowercase() ?: ""
                    (s.contains("showing") || s == "active" || s == "now_showing")
                }
                list.add(HomeItem.NowPlayingSection(
                    nowPlaying.ifEmpty { null },
                    isLoading = nowPlaying.isEmpty() && promos.isEmpty()
                ))
                
                // Coming Soon Section: Lọc linh hoạt tại Client
                val comingSoon = movies.filter {
                    val s = it.status?.lowercase() ?: ""
                    (s.contains("coming") || s.contains("soon") || s == "upcoming" || s == "inactive")
                }
                list.add(HomeItem.ComingSoonSection(
                    comingSoon.ifEmpty { null },
                    isLoading = comingSoon.isEmpty() && promos.isEmpty()
                ))
                
                list.add(HomeItem.PromoSection(promos.ifEmpty { null }, isLoading = promos.isEmpty()))
                list.add(HomeItem.ServiceSection(services.ifEmpty { null }, isLoading = services.isEmpty()))
                list.add(HomeItem.MovieNewsSection(news.ifEmpty { null }, isLoading = news.isEmpty()))
                
                // Videos Section
                list.add(HomeItem.VideosSection(nowPlaying.take(5).ifEmpty { null }, isLoading = nowPlaying.isEmpty()))
                list
            }.collect {
                _homeItems.value = it
                _isLoading.value = false
            }
        }
    }

    fun searchMovies(query: String) {
        _searchQuery.value = query
    }

    fun fetchMovieDetail(id: String) {
        viewModelScope.launch {
            _movieDetail.value = null
            _detailError.value = null

            repository.getMovieDetailsById(id)
                .catch { e ->
                    _detailError.value = "Error: ${e.localizedMessage}"
                }
                .collect { movieData ->
                    _movieDetail.value = MovieResponse(data = movieData)
                }
        }
    }

    fun fetchMovieShowtimes(movieId: String) {
        viewModelScope.launch {
            _movieShowtimes.value = emptyList()
            _showtimeError.value = null

            repository.getMovieShowtimes(movieId)
                .catch { e ->
                    _showtimeError.value = "Error: ${e.localizedMessage}"
                }
                .collect { showtimes ->
                    _movieShowtimes.value = showtimes
                }
        }
    }

    fun deleteMovie(id: String, status: String? = null) {
        viewModelScope.launch {
            try {
                val success = repository.deleteMovie(id)
                _movieActionSuccess.emit(success)
                if (success) {
                    loadAdminMovies(status)
                    _movieActionMessage.emit("Movie deleted successfully")
                } else {
                    _movieActionMessage.emit("Failed to delete movie")
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit("Failed to delete movie: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun addMovie(request: com.example.movie_booking_app.data.model.request.AddMovieRequest, posterFile: File? = null) {
        viewModelScope.launch {
            try {
                val success = repository.addMovie(request, posterFile)
                _movieActionSuccess.emit(success)
                if (success) {
                    loadAdminMovies()
                    loadHomeData()
                    _movieActionMessage.emit("Movie added successfully")
                } else {
                    _movieActionMessage.emit("Failed to add movie")
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit("Failed to add movie: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun updateMovie(id: String, request: com.example.movie_booking_app.data.model.request.UpdateMovieRequest, posterFile: File? = null, status: String? = null) {
        viewModelScope.launch {
            try {
                val success = repository.updateMovie(id, request, posterFile)
                _movieActionSuccess.emit(success)
                if (success) {
                    loadAdminMovies(status)
                    loadHomeData()
                    _movieActionMessage.emit("Movie updated successfully")
                } else {
                    _movieActionMessage.emit("Failed to update movie")
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit("Failed to update movie: ${e.message ?: "Unknown error"}")
            }
        }
    }

    private val _reviewActionSuccess = MutableSharedFlow<Boolean>()
    val reviewActionSuccess = _reviewActionSuccess.asSharedFlow()

    private val _movieReviews = MutableStateFlow<List<com.example.movie_booking_app.data.model.response.ReviewItem>>(emptyList())
    val movieReviews: StateFlow<List<com.example.movie_booking_app.data.model.response.ReviewItem>> = _movieReviews.asStateFlow()

    private val _reviewsTotalCount = MutableStateFlow(0)
    val reviewsTotalCount: StateFlow<Int> = _reviewsTotalCount.asStateFlow()

    fun submitReview(movieId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                val success = repository.submitReview(movieId, rating, comment)
                _reviewActionSuccess.emit(success)
                if (success) {
                    fetchMovieDetail(movieId)
                    fetchMovieReviews(movieId)
                }
            } catch (e: Exception) {
                _reviewActionSuccess.emit(false)
            }
        }
    }

    fun fetchMovieReviews(movieId: String, page: Int = 1, limit: Int = 5) {
        viewModelScope.launch {
            try {
                val reviews = repository.getMovieReviews(movieId, page, limit)
                _movieReviews.value = reviews
            } catch (e: Exception) {
                _movieReviews.value = emptyList()
            }
        }
    }
}
