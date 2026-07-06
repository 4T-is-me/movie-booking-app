package com.example.movie_booking_app.viewmodel.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    init {
        loadStaff()
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
                                    username = dbStaff.account?.username ?: "",
                                    fullName = dbStaff.fullName ?: "",
                                    dob = "15/06/1996", // Sensible placeholder since DB Staff schema doesn't store dob
                                    gender = "Nam",      // Sensible placeholder since DB Staff schema doesn't store gender
                                    email = dbStaff.account?.email ?: "",
                                    identityCard = dbStaff.identityCard ?: "",
                                    phone = dbStaff.phone ?: "",
                                    address = "Hà Nội" // Sensible placeholder since DB Staff schema doesn't store address directly
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
}
