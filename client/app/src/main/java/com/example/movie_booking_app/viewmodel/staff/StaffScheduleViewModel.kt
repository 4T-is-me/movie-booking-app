package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StaffScheduleViewModel @Inject constructor(
    private val repository: StaffRepository
) : ViewModel() {

    private val _shifts = MutableStateFlow<List<ShiftItemResponse>>(emptyList())
    val shifts: StateFlow<List<ShiftItemResponse>> = _shifts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()



    fun loadShifts(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getStaffShifts(startDate, endDate).collect { response ->
                val unsortedList = response?.data ?: emptyList()
                val sortedList = unsortedList.sortedWith(compareBy<ShiftItemResponse> { it.workDate }.thenBy { 
                    when (it.shift) {
                        "Morning" -> 1
                        "Afternoon" -> 2
                        "Evening" -> 3
                        "Night" -> 4
                        else -> 5
                    }
                })
                _shifts.value = sortedList
                _isLoading.value = false
            }
        }
    }


 
}
