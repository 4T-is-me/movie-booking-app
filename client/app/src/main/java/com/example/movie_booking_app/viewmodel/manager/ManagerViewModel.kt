package com.example.movie_booking_app.viewmodel.manager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.ManagerDashboardResponse
import com.example.movie_booking_app.data.repository.manager.ManagerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagerViewModel @Inject constructor(private val repository: ManagerRepository) : ViewModel() {
    private val _dashboardData = MutableStateFlow<ManagerDashboardResponse?>(null)
    val dashboardData: StateFlow<ManagerDashboardResponse?> = _dashboardData.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            repository.getDashboard().collect { response ->
                _dashboardData.value = response
            }
        }
    }
}

