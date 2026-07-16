package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.data.model.request.CreateStaffRequest
import com.example.movie_booking_app.data.model.request.UpdateStaffRequest
import com.example.movie_booking_app.data.repository.admin.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminStaffUiState(
    val isLoading: Boolean = false,
    val staffList: List<Staff> = emptyList(),
    val memberList: List<com.example.movie_booking_app.data.model.response.MemberItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class AdminStaffViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminStaffUiState())
    val uiState: StateFlow<AdminStaffUiState> = _uiState.asStateFlow()

    private val _cinemas = MutableStateFlow<List<Cinema>>(emptyList())
    val cinemas: StateFlow<List<Cinema>> = _cinemas.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess: SharedFlow<Boolean> = _actionSuccess.asSharedFlow()

    private val _actionMessage = MutableSharedFlow<String>()
    val actionMessage: SharedFlow<String> = _actionMessage.asSharedFlow()

    private val _scoreAdjustState = MutableSharedFlow<com.example.movie_booking_app.data.model.response.ScoreAdjustState>()
    val scoreAdjustState: SharedFlow<com.example.movie_booking_app.data.model.response.ScoreAdjustState> = _scoreAdjustState.asSharedFlow()

    init {
        loadStaff()
        loadMembers()
        loadCinemas()
    }

    fun loadCinemas() {
        viewModelScope.launch {
            repository.getCinemasFlow()
                .catch { e -> _actionMessage.emit(e.message ?: "Error loading cinemas") }
                .collect { _cinemas.value = it }
        }
    }

    fun loadStaff(searchQuery: String? = null, showLoading: Boolean = true) {
        viewModelScope.launch {
            repository.getStaffListFlow(page = 1, limit = 100, search = searchQuery)
                .onStart {
                    if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { staffItems ->
                    val mappedStaffs = staffItems.map { dbStaff ->
                        Staff(
                            id = dbStaff.id,
                            username = dbStaff.account?.username ?: "",
                            fullName = dbStaff.fullName ?: "",
                            dob = "15/06/1996",
                            gender = "Nam",
                            email = dbStaff.account?.email ?: "",
                            identityCard = dbStaff.identityCard ?: "",
                            phone = dbStaff.phone ?: "",
                            address = dbStaff.address ?: "",
                            position = dbStaff.position ?: "",
                            salary = dbStaff.salary ?: 0.0,
                            cinemaId = dbStaff.cinemaId ?: "",
                            startDate = dbStaff.startDate ?: "",
                            kpiPoints = dbStaff.kpiPoints ?: 0,
                            ticketSold = dbStaff.ticketSold ?: 0,
                            rating = dbStaff.rating ?: 0.0,
                            totalRatings = dbStaff.totalRatings ?: 0,
                            totalStars = dbStaff.totalStars ?: 0,
                            status = dbStaff.account?.status
                        )
                    }
                    _uiState.update { it.copy(isLoading = false, staffList = mappedStaffs, error = null) }
                }
        }
    }

    fun createStaff(request: CreateStaffRequest) {
        viewModelScope.launch {
            repository.createStaffFlow(request)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit("Employee added successfully!")
                    loadStaff(showLoading = false)
                }
        }
    }

    fun updateStaff(id: String, request: UpdateStaffRequest) {
        viewModelScope.launch {
            repository.updateStaffFlow(id, request)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit("Employee updated successfully!")
                    loadStaff(showLoading = false)
                }
        }
    }

    fun deleteStaff(id: String) {
        viewModelScope.launch {
            repository.deleteStaffFlow(id)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit("Employee account disabled!")
                    loadStaff(showLoading = false)
                }
        }
    }

    // Member Management Methods
    fun loadMembers(searchQuery: String? = null, status: Int? = null, showLoading: Boolean = true) {
        viewModelScope.launch {
            repository.getMemberListFlow(search = searchQuery, status = status)
                .onStart {
                    if (showLoading) _uiState.update { it.copy(isLoading = true, error = null) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { members ->
                    _uiState.update { it.copy(isLoading = false, memberList = members) }
                }
        }
    }

    fun adjustMemberScore(memberId: String, currentPoints: Int, adjustment: Int, reason: String) {
        if (currentPoints + adjustment < 0) {
            viewModelScope.launch { _actionMessage.emit("Total points cannot be less than 0") }
            return
        }
        viewModelScope.launch {
            repository.adjustMemberScoreFlow(memberId, adjustment, reason)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _scoreAdjustState.emit(com.example.movie_booking_app.data.model.response.ScoreAdjustState(isSuccess = true, message = "Points adjusted!"))
                    loadMembers(showLoading = false)
                }
        }
    }

    fun updateMemberStatus(memberId: String, status: Int) {
        viewModelScope.launch {
            repository.updateMemberStatusFlow(memberId, status)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _actionMessage.emit(if (status == 1) "Member activated!" else "Member locked!")
                    loadMembers(showLoading = false)
                }
        }
    }
}
