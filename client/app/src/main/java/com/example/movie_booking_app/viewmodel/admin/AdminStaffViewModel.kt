package com.example.movie_booking_app.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.data.model.request.CreateStaffRequest
import com.example.movie_booking_app.data.model.request.UpdateStaffRequest
import com.example.movie_booking_app.data.repository.admin.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
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
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.getCinemas()
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            _cinemas.value = response.body()?.data ?: emptyList()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error loading cinemas", e)
                }
            }
        }
    }

    fun loadStaff(searchQuery: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.getStaffList(page = 1, limit = 100, search = searchQuery)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body() != null) {
                            val dbStaffItems = response.body()?.data?.staffs ?: emptyList()
                            val mappedStaffs = dbStaffItems.map { dbStaff ->
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
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                staffList = mappedStaffs,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load employee list"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error loading staff list", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Network connection error"
                        )
                    }
                }
            }
        }
    }

    fun createStaff(request: CreateStaffRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.createStaff(request)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit("Employee added successfully!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Failed to add employee")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error creating staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Network connection error")
                    }
                }
            }
        }
    }

    fun updateStaff(id: String, request: UpdateStaffRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.updateStaff(id, request)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit("Employee updated successfully!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Failed to update employee")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error updating staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Network connection error")
                    }
                }
            }
        }
    }

    fun deleteStaff(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.deleteStaff(id)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            _actionMessage.emit("Employee account disabled!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Failed to disable account")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error deleting staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Network connection error")
                    }
                }
            }
        }
    }

    fun updateStaffStatus(id: String, status: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            withContext(Dispatchers.IO) {
                try {
                    val response = repository.updateStaffStatus(id, status)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        if (response.isSuccessful) {
                            _actionSuccess.emit(true)
                            val statusMsg = if (status != 0) "Employee account reactivated!" else "Employee account disabled!"
                            _actionMessage.emit(statusMsg)
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Failed to update account status")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error updating staff status", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Network connection error")
                    }
                }
            }
        }
    }

    // Member Management Methods
    fun loadMembers(searchQuery: String? = null, status: Int? = null) {
        viewModelScope.launch {
            repository.getMemberListFlow(search = searchQuery, status = status)
                .onStart {
                    _uiState.update { it.copy(isLoading = true, error = null) }
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
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _uiState.update { it.copy(isLoading = false) }
                    _scoreAdjustState.emit(com.example.movie_booking_app.data.model.response.ScoreAdjustState(isSuccess = true, message = "Points adjusted!"))
                    loadMembers()
                }
        }
    }

    fun updateMemberStatus(memberId: String, status: Int) {
        viewModelScope.launch {
            repository.updateMemberStatusFlow(memberId, status)
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(e.message ?: "Error")
                }
                .collect {
                    _uiState.update { it.copy(isLoading = false) }
                    _actionMessage.emit(if (status == 1) "Member activated!" else "Member locked!")
                    loadMembers()
                }
        }
    }
}
