package com.example.movie_booking_app.view.fragment.manager

import android.content.Intent
import android.os.Bundle
import com.example.movie_booking_app.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.databinding.FragmentManagerProfileBinding
import com.example.movie_booking_app.view.activity.ChangePasswordActivity
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerProfileFragment : Fragment() {

    private var _binding: FragmentManagerProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide local toolbar
        binding.toolbar.visibility = View.GONE

        setupListeners()
        observeViewModel()
        
        viewModel.loadUserProfile()
    }

    private fun setupListeners() {
        binding.btnSaveChanges.setOnClickListener {
            val email = binding.fieldEmail.getValue()
            val phone = binding.fieldPhone.getValue()
            val fullName = binding.fieldFullName.getValue()
            if (validateInput(fullName, email, phone)) {
                viewModel.updateProfile(fullName, email, phone, null)
            }
        }
        binding.btnChangePassword.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), ChangePasswordActivity::class.java))
        }
    }

    private fun validateInput(fullName: String, email: String, phone: String): Boolean {
        var isValid = true

        if (fullName.trim().isEmpty()) {
            binding.fieldFullName.getEditText().error = getString(R.string.full_name_required)
            isValid = false
        } else if (fullName.contains(Regex("\\d"))) {
            binding.fieldFullName.getEditText().error = getString(R.string.full_name_no_numbers)
            isValid = false
        } else {
            binding.fieldFullName.getEditText().error = null
        }

        if (email.trim().isEmpty()) {
            binding.fieldEmail.getEditText().error = getString(R.string.email_required)
            isValid = false
        } else if (!email.trim().endsWith("@gmail.com")) {
            binding.fieldEmail.getEditText().error = getString(R.string.email_invalid_format)
            isValid = false
        } else {
            binding.fieldEmail.getEditText().error = null
        }

        if (phone.trim().isEmpty()) {
            binding.fieldPhone.getEditText().error = getString(R.string.phone_required)
            isValid = false
        } else if (!phone.trim().matches(Regex("^0[0-9]{9}$"))) {
            binding.fieldPhone.getEditText().error = getString(R.string.phone_invalid_format)
            isValid = false
        } else {
            binding.fieldPhone.getEditText().error = null
        }

        return isValid
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { errorMsg ->
                errorMsg?.let {
                    Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                    viewModel.resetError()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.updateSuccess.collectLatest { success ->
                if (success) {
                    Snackbar.make(binding.root, getString(R.string.profile_updated_successfully), Snackbar.LENGTH_SHORT).show()
                    viewModel.resetUpdateSuccess()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
