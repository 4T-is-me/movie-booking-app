package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StaffScheduleViewModel @Inject constructor(
    private val repository: StaffRepository
) : ViewModel() {

    private val _shifts = MutableStateFlow<List<ShiftItemResponse>>(emptyList())
    val shifts: StateFlow<List<ShiftItemResponse>> = _shifts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _leaveRequestStatus = MutableStateFlow<Boolean?>(null)
    val leaveRequestStatus: StateFlow<Boolean?> = _leaveRequestStatus.asStateFlow()

    private val _replacementStatus = MutableStateFlow<Boolean?>(null)
    val replacementStatus: StateFlow<Boolean?> = _replacementStatus.asStateFlow()

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

    fun requestTimeOff(shiftId: String, reason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createLeaveRequest(shiftId, reason).collect { success ->
                _leaveRequestStatus.value = success
                if (success) {
                    loadShifts()
                }
                _isLoading.value = false
            }
        }
    }

    fun acceptReplacement(leaveRequestId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.acceptReplacementShift(leaveRequestId).collect { success ->
                _replacementStatus.value = success
                if (success) {
                    loadShifts()
                }
                _isLoading.value = false
            }
        }
    }

    fun resetRequestStatus() {
        _leaveRequestStatus.value = null
        _replacementStatus.value = null
    }
}
