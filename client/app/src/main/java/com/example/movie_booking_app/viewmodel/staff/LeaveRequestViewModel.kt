package com.example.movie_booking_app.viewmodel.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.response.AcceptShiftResponse
import com.example.movie_booking_app.data.model.response.CreateLeaveResponse
import com.example.movie_booking_app.data.model.response.LeaveRequestResponse
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestResponse
import com.example.movie_booking_app.data.model.response.ReplacementShiftResponse
import com.example.movie_booking_app.data.model.response.SimpleSuccessResponse
import com.example.movie_booking_app.data.repository.LeaveRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaveRequestViewModel@Inject constructor(private val repository: LeaveRequestRepository) : ViewModel() {
    // --- States cho Staff ---
    private val _createLeaveResult = MutableStateFlow<CreateLeaveResponse?>(null)
    val createLeaveResult: StateFlow<CreateLeaveResponse?> = _createLeaveResult.asStateFlow()

    private val _replacementShifts = MutableStateFlow<List<ReplacementShiftResponse>?>(null)
    val replacementShifts : StateFlow<List<ReplacementShiftResponse>?> = _replacementShifts.asStateFlow()

    private val _acceptShiftResult = MutableStateFlow<AcceptShiftResponse?>(null)
    val acceptShiftResult : StateFlow<AcceptShiftResponse?> = _acceptShiftResult.asStateFlow()

    // --- States cho Manager ---
    private val _managerLeaveRequests = MutableStateFlow<List<ManagerLeaveRequestResponse>?>(null)
    val managerLeaveRequests: StateFlow<List<ManagerLeaveRequestResponse>?> = _managerLeaveRequests.asStateFlow()

    private val _approveResult = MutableStateFlow<SimpleSuccessResponse?>(null)
    val approveResult: StateFlow<SimpleSuccessResponse?> = _approveResult.asStateFlow()

    private val _rejectResult = MutableStateFlow<SimpleSuccessResponse?>(null)
    val rejectResult: StateFlow<SimpleSuccessResponse?> = _rejectResult.asStateFlow()
    private val _leaveRequests = MutableStateFlow<List<LeaveRequestResponse>>(emptyList())
    val leaveRequests: StateFlow<List<LeaveRequestResponse>> = _leaveRequests.asStateFlow()

    private val _undoResult = MutableStateFlow<SimpleSuccessResponse?>(null)
    val undoResult: StateFlow<SimpleSuccessResponse?> = _undoResult.asStateFlow()

    private val _actionSuccessMessage = MutableStateFlow<String?>(null)
    val actionSuccessMessage: StateFlow<String?> = _actionSuccessMessage

    // --- Loading & Error UI ---
    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()



    fun createLeaveRequest(staffShift: String, reason: String) {
        viewModelScope.launch {
            repository.createLeaveRequest(staffShift, reason).collect { result ->
                result.onSuccess { response ->
                    _createLeaveResult.value = response
                }.onFailure { exception ->
                    _errorMessage.value = exception.message ?: "An unknown error occurred"
                }
            }
        }
    }

    fun loadReplacementShifts() {
        viewModelScope.launch {
            repository.getReplacementShift().collect {response ->
                _replacementShifts.value = response
            }
        }
    }

    fun loadMyLeaveRequests(status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getMyLeaveRequests(status).collectLatest { result ->
                _isLoading.value = false
                result.onSuccess { list ->
                    _leaveRequests.value = list
                }.onFailure { exception ->
                    _errorMessage.value = exception.message
                }
            }
        }
    }

    fun acceptReplacementShift(shiftId: String) {
        viewModelScope.launch {
            repository.acceptReplacementShift(shiftId).collect {response ->
                _acceptShiftResult.value = response
            }
        }
    }

    fun loadLeaveRequestForManager(status: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            repository.getLeaveRequestForManager(status).collect { response ->
                _isLoading.value = false
                if (response != null) {
                    _managerLeaveRequests.value = response
                } else {
                    _errorMessage.value = "Failed to load leave requests."
                }
            }
        }
    }

    fun approveLeaveRequest(leaveRequestId: String) {
        viewModelScope.launch {
            repository.approveLeaveRequest(leaveRequestId).collect {response ->
                _approveResult.value = response
            }
        }
    }

    fun rejectLeaveRequest(leaveRequestId: String, reason: String) {
        viewModelScope.launch {
            repository.rejectLeaveRequest(leaveRequestId, reason).collect { response ->
                _rejectResult.value = response
            }
        }
    }
    fun undoLeaveRequest(leaveRequestId: String) {
        viewModelScope.launch {
            repository.undoLeaveRequest(leaveRequestId).collect { response ->
                _rejectResult.value = response
            }
        }
    }

    fun updateLeaveReason(id: String, newReason: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.updateLeaveRequestReason(id, newReason).collectLatest { result ->
                _isLoading.value = false
                result.onSuccess {
                    _actionSuccessMessage.value = it.message
                    loadMyLeaveRequests()
                }.onFailure {
                    _errorMessage.value = it.message
                }
            }
        }
    }

    fun deleteLeaveRequest(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteLeaveRequest(id).collectLatest { result ->
                _isLoading.value = false
                result.onSuccess {
                    _actionSuccessMessage.value = it.message
                    loadMyLeaveRequests()
                }.onFailure {
                    _errorMessage.value = it.message
                }
            }
        }
    }


    fun clearError() {
        _errorMessage.value = null
    }

    fun clearCreateLeaveState() {
        _createLeaveResult.value = null
    }

    fun clearAcceptShiftState() {
        _acceptShiftResult.value = null
    }

    fun clearApproveState() {
        _approveResult.value = null
    }

    fun clearRejectState() {
        _rejectResult.value = null
    }
    fun clearUndoState() {
        _rejectResult.value = null
    }
    fun clearSuccessMessage() {
        _actionSuccessMessage.value = null 
    }
}