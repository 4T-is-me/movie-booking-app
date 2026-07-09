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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AdminStaffUiState(
    val isLoading: Boolean = false,
    val staffList: List<Staff> = emptyList(),
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

    init {
        loadStaff()
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
                                    dob = "15/06/1996", // Sensible placeholder since DB Staff schema doesn't store dob
                                    gender = "Nam",      // Sensible placeholder since DB Staff schema doesn't store gender
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
                            _uiState.value = AdminStaffUiState(
                                isLoading = false,
                                staffList = mappedStaffs,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Lấy dữ liệu nhân viên thất bại"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error loading staff list", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = e.localizedMessage ?: "Lỗi kết nối mạng"
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
                            _actionMessage.emit("Thêm nhân viên thành công!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Thêm nhân viên thất bại")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error creating staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Lỗi kết nối mạng")
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
                            _actionMessage.emit("Cập nhật nhân viên thành công!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Cập nhật nhân viên thất bại")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error updating staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Lỗi kết nối mạng")
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
                            _actionMessage.emit("Đã khóa tài khoản nhân viên!")
                            loadStaff()
                        } else {
                            _actionSuccess.emit(false)
                            _actionMessage.emit(response.errorBody()?.string() ?: "Khóa tài khoản thất bại")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AdminStaffVM", "Error deleting staff", e)
                    withContext(Dispatchers.Main) {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        _actionSuccess.emit(false)
                        _actionMessage.emit(e.localizedMessage ?: "Lỗi kết nối mạng")
                    }
                }
            }
        }
    }
}
