package com.example.movie_booking_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movie_booking_app.data.model.request.UpdateProfileRequest
import com.example.movie_booking_app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.getUserProfile()
                .onStart {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Failed to load profile details"
                    )
                }
                .collect { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = profile,
                        avatarUrl = profile.image
                    )
                }
        }
    }

    fun setUploadingState(isUploading: Boolean) {
        _uiState.value = _uiState.value.copy(isUploadingAvatar = isUploading)
    }

    fun uploadAvatar(imageFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingAvatar = true, error = null)
            try {
                val body = withContext(Dispatchers.IO) {
                    val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("avatar", imageFile.name, requestFile)
                }

                repository.uploadAvatar(body)
                    .catch { exception ->
                        _uiState.value = _uiState.value.copy(
                            isUploadingAvatar = false,
                            error = exception.message ?: "Unable to update profile. Please try again."
                        )
                    }
                    .collect { response ->
                        if (response.success) {
                            val updateReq = UpdateProfileRequest(avatar = response.avatarUrl)
                            repository.updateProfile(updateReq)
                                .catch { exception ->
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingAvatar = false,
                                        error = exception.message ?: "Unable to update profile. Please try again."
                                    )
                                }
                                .collect { updatedProfile ->
                                    _uiState.value = _uiState.value.copy(
                                        isUploadingAvatar = false,
                                        avatarUrl = response.avatarUrl,
                                        userProfile = updatedProfile
                                    )
                                }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isUploadingAvatar = false,
                                error = "Unable to update profile. Please try again."
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUploadingAvatar = false,
                    error = "Unable to update profile. Please try again."
                )
            }
        }
    }

    fun saveProfile(
        fullName: String,
        dob: String,
        gender: String,
        phone: String,
        identityCard: String,
        email: String,
        address: String,
        favoriteCinema: String,
        favoriteGenres: List<String>
    ) {
        // Run Validations
        var hasError = false
        var nameErr: String? = null
        var phoneErr: String? = null
        var idCardErr: String? = null
        var dobErr: String? = null
        var addressErr: String? = null

        if (fullName.isEmpty()) {
            nameErr = "Full name cannot be empty"
            hasError = true
        }

        if (!phone.matches(Regex("^0\\d{9}$"))) {
            phoneErr = "Phone number must be 10 digits and start with 0"
            hasError = true
        }

        if (!identityCard.matches(Regex("^\\d{12}$"))) {
            idCardErr = "Identity card must be exactly 12 digits"
            hasError = true
        }

        if (address.isEmpty()) {
            addressErr = "Address cannot be empty"
            hasError = true
        }

        // Validate Date of Birth is not in the future
        if (dob.isNotEmpty()) {
            try {
                // Support both standard formats (dd/MM/yyyy or yyyy-MM-dd)
                val sdf = if (dob.contains("-")) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                } else {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                }
                val date = sdf.parse(dob)
                if (date != null && date.after(Calendar.getInstance().time)) {
                    dobErr = "Date of birth cannot be in the future"
                    hasError = true
                }
            } catch (e: Exception) {
                dobErr = "Invalid date format"
                hasError = true
            }
        } else {
            dobErr = "Date of birth cannot be empty"
            hasError = true
        }

        if (hasError) {
            _uiState.value = _uiState.value.copy(
                nameError = nameErr,
                phoneError = phoneErr,
                identityCardError = idCardErr,
                dateOfBirthError = dobErr,
                addressError = addressErr
            )
            return
        }

        // Clear validation errors and call API
        _uiState.value = _uiState.value.copy(
            nameError = null,
            phoneError = null,
            identityCardError = null,
            dateOfBirthError = null,
            addressError = null
        )

        viewModelScope.launch {
            val req = withContext(Dispatchers.Default) {
                var apiDob = dob
                if (dob.contains("/")) {
                    try {
                        val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        displayFormat.parse(dob)?.let { parsedDate ->
                            apiDob = apiFormat.format(parsedDate)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                UpdateProfileRequest(
                    email = email,
                    phone = phone,
                    address = address,
                    fullName = fullName,
                    dob = apiDob,
                    gender = gender,
                    identityCard = identityCard,
                    favoriteCinema = favoriteCinema,
                    favoriteGenres = favoriteGenres,
                    avatar = _uiState.value.avatarUrl
                )
            }

            repository.updateProfile(req)
                .onStart {
                    _uiState.value = _uiState.value.copy(isSaving = true, error = null)
                }
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = exception.message ?: "Failed to update profile"
                    )
                }
                .collect { updatedProfile ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveSuccess = true,
                        userProfile = updatedProfile
                    )
                }
        }
    }

    fun setAvatarUrl(url: String?) {
        _uiState.value = _uiState.value.copy(avatarUrl = url)
    }

    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }

    fun resetError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
