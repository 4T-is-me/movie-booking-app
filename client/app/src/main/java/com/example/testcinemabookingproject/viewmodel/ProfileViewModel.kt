package com.example.testcinemabookingproject.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.testcinemabookingproject.data.model.Profile
import com.example.testcinemabookingproject.R

class ProfileViewModel : ViewModel() {
    private val _userProfile = MutableLiveData<Profile>()

    val userProfile: LiveData<Profile> get() = _userProfile

    init {
        loadProfileData()
    }

    fun loadProfileData() {
        val dummyData = Profile(
            name = "Minus",
            job = "Developer",
            avatarResId = R.drawable.ic_launcher_background
        )
        _userProfile.value = dummyData
    }
}
