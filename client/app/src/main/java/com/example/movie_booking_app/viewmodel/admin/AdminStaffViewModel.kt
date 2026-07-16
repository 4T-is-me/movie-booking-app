package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.MyApplication
import com.example.movie_booking_app.R
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

    private val _managers = MutableStateFlow<List<Staff>>(emptyList())
    val managers: StateFlow<List<Staff>> = _managers.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess: SharedFlow<Boolean> = _actionSuccess.asSharedFlow()

    private val _actionMessage = MutableSharedFlow<String>()
    val actionMessage: SharedFlow<String> = _actionMessage.asSharedFlow()

    private val _scoreAdjustState = MutableSharedFlow<com.example.movie_booking_app.data.model.response.ScoreAdjustState>()
    val scoreAdjustState: SharedFlow<com.example.movie_booking_app.data.model.response.ScoreAdjustState> = _scoreAdjustState.asSharedFlow()

    private var currentRole: String = "Manager"

    init {
        loadMembers()
        loadCinemas()
        loadManagers()
    }

    fun setRole(role: String) {
        currentRole = role
        loadStaff()
    }

    private fun getString(resId: Int): String {
        return MyApplication.getContext().getString(resId)
    }

    fun loadCinemas() {
        viewModelScope.launch {
            repository.getCinemasFlow()
                .catch { e -> _actionMessage.emit(e.message ?: getString(R.string.auto_str_failed_to_load_cinema_rooms)) }
                .collect { _cinemas.value = it }
        }
    }

    private fun loadManagers() {
        viewModelScope.launch {
            repository.getStaffListFlow(page = 1, limit = 100, search = null, role = "Manager")
                .catch { }
                .collect { staffItems ->
                    val mappedStaffs = staffItems.map { dbStaff ->
                        Staff(
                            id = dbStaff.id,
                            cinemaId = dbStaff.cinemaId ?: ""
                        )
                    }
                    _managers.value = mappedStaffs
                }
        }
    }

    fun loadStaff(searchQuery: String? = null, showLoading: Boolean = true) {
        viewModelScope.launch {
            repository.getStaffListFlow(page = 1, limit = 100, search = searchQuery, role = currentRole)
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
                            username = dbStaff.account?.username ?: getString(R.string.user_name_placeholder),
                            fullName = dbStaff.fullName ?: getString(R.string.user_name_placeholder),
                            dob = getString(R.string.user_name_placeholder),
                            gender = getString(R.string.user_name_placeholder),
                            email = dbStaff.account?.email ?: getString(R.string.user_name_placeholder),
                            identityCard = dbStaff.identityCard ?: getString(R.string.user_name_placeholder),
                            phone = dbStaff.phone ?: getString(R.string.user_name_placeholder),
                            address = dbStaff.address ?: getString(R.string.user_name_placeholder),
                            position = dbStaff.position ?: getString(R.string.user_name_placeholder),
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
                    _actionMessage.emit(e.message ?: getString(R.string.admin_error_failed_add_employee))
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit(getString(R.string.admin_success_employee_added))
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
                    _actionMessage.emit(e.message ?: getString(R.string.admin_error_failed_update_employee))
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit(getString(R.string.admin_success_employee_updated))
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
                    _actionMessage.emit(e.message ?: getString(R.string.admin_error_failed_disable_account))
                }
                .collect {
                    _actionSuccess.emit(true)
                    _actionMessage.emit(getString(R.string.admin_success_employee_disabled))
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
            viewModelScope.launch { _actionMessage.emit(getString(R.string.admin_error_points_negative)) }
            return
        }
        viewModelScope.launch {
            repository.adjustMemberScoreFlow(memberId, adjustment, reason)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: getString(R.string.admin_error_network))
                }
                .collect {
                    _scoreAdjustState.emit(com.example.movie_booking_app.data.model.response.ScoreAdjustState(isSuccess = true, message = getString(R.string.admin_success_points_adjusted)))
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
                    _actionMessage.emit(e.message ?: getString(R.string.admin_error_network))
                }
                .collect {
                    _actionMessage.emit(if (status == 1) getString(R.string.admin_success_member_activated) else getString(R.string.admin_success_member_deleted))
                    loadMembers(showLoading = false)
                }
        }
    }
}
