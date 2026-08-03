package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.databinding.ActivityManagerProfileBinding
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeViewModel()
        
        viewModel.loadUserProfile()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnSaveChanges.setOnClickListener {
            val email = binding.fieldEmail.getValue()
            val phone = binding.fieldPhone.getValue()
            val fullName = binding.fieldFullName.getValue()
            if (validateInput(fullName, email, phone)) {
                viewModel.updateProfile(fullName, email, phone, null)
            }
        }
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }
    }

    private fun validateInput(fullName: String, email: String, phone: String): Boolean {
        var isValid = true

        if (fullName.trim().isEmpty()) {
            binding.fieldFullName.getEditText().error = "Full Name is required"
            isValid = false
        } else if (fullName.contains(Regex("\\d"))) {
            binding.fieldFullName.getEditText().error = "Full Name cannot contain numbers"
            isValid = false
        } else {
            binding.fieldFullName.getEditText().error = null
        }

        if (email.trim().isEmpty()) {
            binding.fieldEmail.getEditText().error = "Email is required"
            isValid = false
        } else if (!email.trim().endsWith("@gmail.com")) {
            binding.fieldEmail.getEditText().error = "Email must end with @gmail.com"
            isValid = false
        } else {
            binding.fieldEmail.getEditText().error = null
        }

        if (phone.trim().isEmpty()) {
            binding.fieldPhone.getEditText().error = "Phone number is required"
            isValid = false
        } else if (!phone.trim().matches(Regex("^0[0-9]{9}$"))) {
            binding.fieldPhone.getEditText().error = "Phone must be 10 digits and start with 0"
            isValid = false
        } else {
            binding.fieldPhone.getEditText().error = null
        }

        return isValid
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.userProfile.collectLatest { profile ->
                profile?.let {
                    binding.fieldFullName.setValue(it.fullName ?: "")
                    binding.fieldEmail.setValue(it.email ?: "")
                    binding.fieldPhone.setValue(it.phone ?: "")
                    
                    val initials = it.fullName?.split(" ")
                        ?.mapNotNull { word -> word.firstOrNull()?.uppercase() }
                        ?.take(2)?.joinToString("") ?: "M"
                    binding.ivAvatar.text = initials
                }
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.error.collectLatest { errorMsg ->
                errorMsg?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.resetError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, "Profile updated successfully!", Snackbar.LENGTH_SHORT).show()
                    viewModel.resetUpdateSuccess()
                }
            }
        }
    }
}
