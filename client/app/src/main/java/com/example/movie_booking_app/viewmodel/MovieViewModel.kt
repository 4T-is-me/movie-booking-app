package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.data.model.request.AddMovieRequest
import com.example.movie_booking_app.data.model.request.UpdateMovieRequest
import com.example.movie_booking_app.data.model.response.MovieResponse
import com.example.movie_booking_app.data.model.response.ReviewItem
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.NewsRepository
import com.example.movie_booking_app.data.repository.ProfileRepository
import com.example.movie_booking_app.utils.Constants
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import android.content.Context
import com.example.movie_booking_app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

@HiltViewModel
class MovieViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MovieRepository,
    private val profileRepository: ProfileRepository,
    private val newsRepository: NewsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _homeItems = MutableStateFlow<List<HomeItem>>(initialHomeState())
    val homeItems: StateFlow<List<HomeItem>> = _homeItems.asStateFlow()

    private var cachedHomeItems: List<HomeItem>? = null
    private var searchJob: Job? = null

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

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        _searchQuery
            .debounce(Constants.BookingConfig.DEBOUNCE_SEARCH_MS)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    // Restore from cache if available, otherwise load fresh
                    cachedHomeItems?.let { 
                        _homeItems.value = it
                    } ?: loadHomeData()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
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
                val movieStatus = Constants.MovieStatus
                
                // Helper to filter status
                fun Movie.hasStatus(vararg keys: String) = keys.any { status?.lowercase()?.contains(it) == true }

                // Now Playing Section
                val nowPlaying = movies.filter { it.hasStatus(movieStatus.SHOWING, movieStatus.ACTIVE) }
                list.add(HomeItem.NowPlayingSection(
                    nowPlaying.ifEmpty { null },
                    isLoading = nowPlaying.isEmpty() && promos.isEmpty()
                ))
                
                // Coming Soon Section: Hide if empty
                val comingSoon = movies.filter { it.hasStatus(movieStatus.COMING_SOON, movieStatus.SOON, movieStatus.UPCOMING, movieStatus.INACTIVE) }
                if (comingSoon.isNotEmpty()) {
                    list.add(HomeItem.ComingSoonSection(comingSoon, isLoading = false))
                }
                
                list.add(HomeItem.PromoSection(promos.ifEmpty { null }, isLoading = promos.isEmpty()))
                list.add(HomeItem.ServiceSection(services.ifEmpty { null }, isLoading = services.isEmpty()))
                list.add(HomeItem.MovieNewsSection(news.ifEmpty { null }, isLoading = news.isEmpty()))
                
                // Videos Section
                list.add(HomeItem.VideosSection(nowPlaying.take(5).ifEmpty { null }, isLoading = nowPlaying.isEmpty()))
                list
            }.collect {
                _homeItems.value = it
                cachedHomeItems = it // Cache for quick restoration
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
                    _detailError.value = context.getString(R.string.error_localized, e.localizedMessage)
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
                    _showtimeError.value = context.getString(R.string.error_localized, e.localizedMessage)
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
                    _movieActionMessage.emit(Constants.MovieActionMessage.DELETED_SUCCESS)
                } else {
                    _movieActionMessage.emit(Constants.MovieActionMessage.DELETE_FAILED)
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit(Constants.MovieActionMessage.ERROR_UNKNOWN)
            }
        }
    }

    fun addMovie(request: AddMovieRequest, posterFile: File? = null) {
        viewModelScope.launch {
            try {
                val success = repository.addMovie(request, posterFile)
                _movieActionSuccess.emit(success)
                if (success) {
                    loadAdminMovies()
                    loadHomeData()
                    _movieActionMessage.emit(Constants.MovieActionMessage.ADDED_SUCCESS)
                } else {
                    _movieActionMessage.emit(Constants.MovieActionMessage.ADD_FAILED)
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit(Constants.MovieActionMessage.ERROR_UNKNOWN)
            }
        }
    }

    fun updateMovie(id: String, request: UpdateMovieRequest, posterFile: File? = null, status: String? = null) {
        viewModelScope.launch {
            try {
                val success = repository.updateMovie(id, request, posterFile)
                _movieActionSuccess.emit(success)
                if (success) {
                    loadAdminMovies(status)
                    loadHomeData()
                    _movieActionMessage.emit(Constants.MovieActionMessage.UPDATED_SUCCESS)
                } else {
                    _movieActionMessage.emit(Constants.MovieActionMessage.UPDATE_FAILED)
                }
            } catch (e: Exception) {
                _movieActionSuccess.emit(false)
                _movieActionMessage.emit(Constants.MovieActionMessage.ERROR_UNKNOWN)
            }
        }
    }

    private val _reviewActionSuccess = MutableSharedFlow<Boolean>()
    val reviewActionSuccess = _reviewActionSuccess.asSharedFlow()

    private val _reviewErrorMessage = MutableSharedFlow<String?>()
    val reviewErrorMessage = _reviewErrorMessage.asSharedFlow()

    private val _movieReviews = MutableStateFlow<List<ReviewItem>>(emptyList())
    val movieReviews: StateFlow<List<ReviewItem>> = _movieReviews.asStateFlow()

    private val _reviewsTotalCount = MutableStateFlow(0)
    val reviewsTotalCount: StateFlow<Int> = _reviewsTotalCount.asStateFlow()

    fun submitReview(movieId: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                val response = repository.submitReviewWithResult(movieId, rating, comment)
                if (response.isSuccessful && response.body()?.success == true) {
                    _reviewActionSuccess.emit(true)
                    fetchMovieDetail(movieId)
                    fetchMovieReviews(movieId)
                } else {
                    val errorMsg = response.body()?.message ?: response.errorBody()?.string() ?: context.getString(R.string.error_unknown)
                    // Try to parse JSON error message if possible
                    val parsedMsg = try {
                        val json = JsonParser.parseString(errorMsg).asJsonObject
                        json.get("message")?.asString ?: errorMsg
                    } catch (_: Exception) {
                        errorMsg
                    }
                    _reviewErrorMessage.emit(parsedMsg)
                    _reviewActionSuccess.emit(false)
                }
            } catch (e: Exception) {
                _reviewErrorMessage.emit(e.localizedMessage ?: context.getString(R.string.error_network))
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
    fun toggleNewsLike(newsId: String) {
        viewModelScope.launch {
            newsRepository.toggleNewsLike(newsId)
                .catch { e ->
                    // Log or handle error
                }
                .collect {
                    loadHomeData()
                }
        }
    }
}
