package com.example.movie_booking_app.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.entity.FoodBeverage
import com.example.movie_booking_app.data.model.entity.InventoryDeduction
import com.example.movie_booking_app.data.repository.FoodRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

data class AdminFoodUiState(
    val menuItems: List<FoodBeverage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AdminFoodViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminFoodUiState())
    val uiState: StateFlow<AdminFoodUiState> = _uiState.asStateFlow()

    init {
        loadMenu()
    }

    fun loadMenu() {
        viewModelScope.launch {
            repository.getAdminMenu()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .collect { items ->
                    _uiState.update { it.copy(menuItems = items, isLoading = false) }
                }
        }
    }

    fun saveProduct(
        id: String?,
        name: String,
        price: Int,
        category: String,
        description: String,
        isActive: Boolean,
        imagePart: MultipartBody.Part?,
        inventoryDeductions: List<InventoryDeduction>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val parts = mutableMapOf<String, RequestBody>()
            parts["name"] = name.toRequestBody("text/plain".toMediaTypeOrNull())
            parts["price"] = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            parts["category"] = category.toRequestBody("text/plain".toMediaTypeOrNull())
            parts["type"] = (if (category == "combo") "combo" else "single").toRequestBody("text/plain".toMediaTypeOrNull())
            parts["description"] = description.toRequestBody("text/plain".toMediaTypeOrNull())
            parts["isActive"] = isActive.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            
            // Serialize list of deductions to JSON string for multipart body
            val deductionsJson = Gson().toJson(inventoryDeductions)
            parts["inventoryDeductions"] = deductionsJson.toRequestBody("application/json".toMediaTypeOrNull())

            val result = if (id.isNullOrBlank()) {
                repository.addProduct(parts, imagePart)
            } else {
                repository.updateProduct(id, parts, imagePart)
            }

            result.onSuccess { 
                loadMenu()
                _uiState.update { it.copy(successMessage = if (id.isNullOrBlank()) "Added successfully" else "Updated successfully") }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteProduct(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.deleteProduct(id)
                .onSuccess { 
                    loadMenu()
                    _uiState.update { it.copy(successMessage = "Deactivated successfully") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
        }
    }

    fun resetStatus() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}
