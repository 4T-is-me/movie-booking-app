package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.data.model.admin.StatsOverview
import com.example.movie_booking_app.data.repository.admin.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminStatsUiState(
    val isLoading: Boolean = false,
    val overview: StatsOverview? = null,
    val dailyRevenue: List<DailyRevenueItem> = emptyList(),
    val monthlyRevenue: List<MonthlyRevenueItem> = emptyList(),
    val movieRevenue: List<MovieRevenueItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminStatsViewModel @Inject constructor(
    private val repository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStatsUiState())
    val uiState: StateFlow<AdminStatsUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Giả lập thời gian tải để xem hiệu ứng Shimmer
            //kotlinx.coroutines.delay(5000)

            try {
                // Load parallelly using async/awaitAll as requested
                val overviewDeferred = async { repository.getOverviewStats().first() }
                val dailyRevenueDeferred = async { repository.getDailyRevenue("", "").first() }
                val monthlyRevenueDeferred = async { repository.getMonthlyRevenue(2024).first() }
                val movieRevenueDeferred = async { repository.getMovieRevenue().first() }

                val results = awaitAll(overviewDeferred, dailyRevenueDeferred, monthlyRevenueDeferred, movieRevenueDeferred)

                val overviewResponse = results[0] as retrofit2.Response<com.example.movie_booking_app.data.model.response.OverviewStatsResponse>
                val dailyResponse = results[1] as retrofit2.Response<com.example.movie_booking_app.data.model.response.DailyRevenueResponse>
                val monthlyResponse = results[2] as retrofit2.Response<com.example.movie_booking_app.data.model.response.MonthlyRevenueResponse>
                val movieResponse = results[3] as retrofit2.Response<com.example.movie_booking_app.data.model.response.MovieRevenueResponse>

                if (overviewResponse.isSuccessful && dailyResponse.isSuccessful && 
                    monthlyResponse.isSuccessful && movieResponse.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        overview = overviewResponse.body()?.data,
                        dailyRevenue = dailyResponse.body()?.data ?: emptyList(),
                        monthlyRevenue = monthlyResponse.body()?.data ?: emptyList(),
                        movieRevenue = movieResponse.body()?.data ?: emptyList()
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load some data"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An unknown error occurred"
                )
            }
        }
    }
}
