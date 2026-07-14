package com.example.movie_booking_app.viewmodel.admin

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.data.model.request.*
import com.example.movie_booking_app.data.model.response.*
import com.example.movie_booking_app.data.repository.admin.StaffAdminRepository
import com.example.movie_booking_app.utils.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdminStaffViewModel @Inject constructor(
    private val repository: StaffAdminRepository
) : ViewModel() {

    // LiveData for Staff
    private val _staffList = MutableLiveData<List<EmployeeItem>>()
    val staffList: LiveData<List<EmployeeItem>> = _staffList

    // LiveData for Members
    private val _memberList = MutableLiveData<List<MemberItem>>()
    val memberList: LiveData<List<MemberItem>> = _memberList

    // Common State
    private val _isLoadingStaff = MutableLiveData<Boolean>()
    val isLoadingStaff: LiveData<Boolean> = _isLoadingStaff

    private val _isLoadingMembers = MutableLiveData<Boolean>()
    val isLoadingMembers: LiveData<Boolean> = _isLoadingMembers

    private val _error = SingleLiveEvent<String>()
    val error: LiveData<String> = _error

    private val _cinemas = MutableLiveData<List<Cinema>>()
    val cinemas: LiveData<List<Cinema>> = _cinemas

    private val _actionSuccess = SingleLiveEvent<Boolean>()
    val actionSuccess: LiveData<Boolean> = _actionSuccess

    private val _actionMessage = SingleLiveEvent<String>()
    val actionMessage: LiveData<String> = _actionMessage

    private val _scoreAdjustState = MutableLiveData<ScoreAdjustState>()
    val scoreAdjustState: LiveData<ScoreAdjustState> = _scoreAdjustState

    private var currentSearchQuery: String? = null

    init {
        loadStaff()
        loadMembers()
        loadCinemas()
    }

    fun loadCinemas() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = repository.getCinemas()
                if (response.isSuccessful) {
                    _cinemas.postValue(response.body()?.data ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e("AdminStaffVM", "Error loading cinemas", e)
            }
        }
    }

    fun loadStaff(searchQuery: String? = null) {
        currentSearchQuery = searchQuery
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingStaff.postValue(true)
            try {
                val response = repository.getStaffList(page = 1, limit = 100, search = searchQuery)
                if (response.isSuccessful && response.body() != null) {
                    _staffList.postValue(response.body()?.data?.staffs ?: emptyList())
                } else {
                    _error.postValue(parseErrorMessage(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                Log.e("AdminStaffVM", "Error loading staff", e)
                _error.postValue(e.localizedMessage ?: "Network connection error")
            } finally {
                _isLoadingStaff.postValue(false)
            }
        }
    }

    fun createStaff(request: CreateStaffRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingStaff.postValue(true)
            try {
                val response = repository.createStaff(request)
                if (response.isSuccessful) {
                    _actionSuccess.postValue(true)
                    _actionMessage.postValue("Employee added successfully!")
                    loadStaff(currentSearchQuery)
                } else {
                    _error.postValue(parseErrorMessage(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                _isLoadingStaff.postValue(false)
            }
        }
    }

    fun updateStaff(id: String, request: UpdateStaffRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingStaff.postValue(true)
            try {
                // 1. Update general staff info
                val response = repository.updateStaff(id, request)
                
                // 2. If general update success AND status is provided, update status separately 
                // because current backend 'updateStaff' might not handle 'status' field in Account table
                if (response.isSuccessful && request.status != null) {
                    repository.updateStaffStatus(id, request.status)
                }

                if (response.isSuccessful) {
                    _actionSuccess.postValue(true)
                    _actionMessage.postValue("Employee updated successfully!")
                    loadStaff(currentSearchQuery)
                } else {
                    _error.postValue(parseErrorMessage(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                _isLoadingStaff.postValue(false)
            }
        }
    }

    fun deleteStaff(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Bỏ _isLoadingStaff.postValue(true) để tránh gây cảm giác reload trang
            try {
                Log.d("DeleteStaff", "Starting delete for ID: $id")
                val response = repository.deleteStaff(id)
                
                if (response.isSuccessful) {
                    // Cập nhật UI ngay lập tức trong danh sách hiện tại
                    val currentList = _staffList.value?.toMutableList() ?: mutableListOf()
                    val index = currentList.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val currentStaff = currentList[index]
                        val updatedAccount = (currentStaff.account ?: DbStaffAccount(id = id)).copy(status = 0)
                        currentList[index] = currentStaff.copy(account = updatedAccount)
                        _staffList.postValue(currentList)
                    }
                    _actionMessage.postValue("Account locked!")
                } else {
                    _error.postValue(parseErrorMessage(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun updateStaffStatus(id: String, status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Bỏ loading spinner cho Toggle
            try {
                val response = repository.updateStaffStatus(id, status)
                if (response.isSuccessful) {
                    val currentList = _staffList.value?.toMutableList() ?: mutableListOf()
                    val index = currentList.indexOfFirst { it.id == id }
                    if (index != -1) {
                        val updatedStaff = currentList[index].copy(
                            account = currentList[index].account?.copy(status = status)
                        )
                        currentList[index] = updatedStaff
                        _staffList.postValue(currentList)
                    }
                    _actionMessage.postValue(if (status == 1) "Account activated!" else "Account locked!")
                } else {
                    _error.postValue("Failed to update status")
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            }
        }
    }

    // Member Methods
    fun loadMembers(searchQuery: String? = null, status: Int? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMembers.postValue(true)
            try {
                val response = repository.getMemberList(search = searchQuery, status = status)
                if (response.isSuccessful && response.body() != null) {
                    _memberList.postValue(response.body()?.data?.members ?: emptyList())
                } else {
                    _error.postValue("Failed to load members")
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            } finally {
                _isLoadingMembers.postValue(false)
            }
        }
    }

    fun adjustMemberScore(memberId: String, currentPoints: Int, adjustment: Int, reason: String) {
        if (currentPoints + adjustment < 0) {
            _error.postValue("Total points cannot be less than 0")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Bỏ loading spinner cho Adjustment
            try {
                val response = repository.adjustMemberScore(memberId, adjustment, reason)
                if (response.isSuccessful) {
                    val currentList = _memberList.value?.toMutableList() ?: mutableListOf()
                    val index = currentList.indexOfFirst { it.id == memberId }
                    if (index != -1) {
                        val updatedMember = currentList[index].copy(
                            rewardPoints = (currentList[index].rewardPoints ?: 0) + adjustment
                        )
                        currentList[index] = updatedMember
                        _memberList.postValue(currentList)
                    }
                    _scoreAdjustState.postValue(ScoreAdjustState(isSuccess = true, message = "Points adjusted!"))
                } else {
                    _error.postValue(parseErrorMessage(response.errorBody()?.string()))
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            }
        }
    }

    fun updateMemberStatus(memberId: String, status: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // Bỏ loading spinner cho Toggle
            try {
                val response = repository.updateMemberStatus(memberId, status)
                if (response.isSuccessful) {
                    val currentList = _memberList.value?.toMutableList() ?: mutableListOf()
                    val index = currentList.indexOfFirst { it.id == memberId }
                    if (index != -1) {
                        val updatedMember = currentList[index].copy(
                            account = currentList[index].account?.copy(status = status)
                        )
                        currentList[index] = updatedMember
                        _memberList.postValue(currentList)
                    }
                    _actionMessage.postValue(if (status == 1) "Member activated!" else "Member locked!")
                } else {
                    _error.postValue("Failed to update member status")
                }
            } catch (e: Exception) {
                _error.postValue(e.localizedMessage ?: "Network error")
            }
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "Unknown error occurred"
        return try {
            val jsonObject = org.json.JSONObject(errorBody)
            jsonObject.optString("message", "An error occurred")
        } catch (e: Exception) {
            errorBody ?: "Unknown error occurred"
        }
    }
}
