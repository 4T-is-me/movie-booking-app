package com.example.movie_booking_app.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.data.model.admin.StatsOverview
import com.example.movie_booking_app.data.repository.admin.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

            withContext(Dispatchers.IO) {
                try {
                    val calendar = Calendar.getInstance()
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val endDate = sdf.format(calendar.time)
                    calendar.add(Calendar.DAY_OF_YEAR, -30)
                    val startDate = sdf.format(calendar.time)
                    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                    // Call all APIs in parallel
                    val overviewDef = async { try { repository.getOverviewStats().first() } catch (e: Exception) { null } }
                    val dailyDef = async { try { repository.getDailyRevenue(startDate, endDate).first() } catch (e: Exception) { null } }
                    val monthlyDef = async { try { repository.getMonthlyRevenue(currentYear).first() } catch (e: Exception) { null } }
                    val movieDef = async { try { repository.getMovieRevenue().first() } catch (e: Exception) { null } }

                    val overviewRes = overviewDef.await()
                    val dailyRes = dailyDef.await()
                    val monthlyRes = monthlyDef.await()
                    val movieRes = movieDef.await()

                    withContext(Dispatchers.Main) {
                        Log.d("AdminStatsVM", "Overview response: ${overviewRes?.body()?.data}")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            overview = if (overviewRes?.isSuccessful == true) overviewRes.body()?.data else _uiState.value.overview,
                            dailyRevenue = if (dailyRes?.isSuccessful == true) dailyRes.body()?.data ?: emptyList() else _uiState.value.dailyRevenue,
                            monthlyRevenue = if (monthlyRes?.isSuccessful == true) monthlyRes.body()?.data ?: emptyList() else _uiState.value.monthlyRevenue,
                            movieRevenue = if (movieRes?.isSuccessful == true) movieRes.body()?.data ?: emptyList() else _uiState.value.movieRevenue,
                            error = if (overviewRes == null && dailyRes == null && monthlyRes == null && movieRes == null) 
                                "Connection error or data parsing failed" else null
                        )
                    }

                } catch (e: Exception) {
                    Log.e("AdminStatsVM", "Error loading dashboard", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.message ?: "An unknown error occurred"
                        )
                    }
                }
            }
        }
    }
}
