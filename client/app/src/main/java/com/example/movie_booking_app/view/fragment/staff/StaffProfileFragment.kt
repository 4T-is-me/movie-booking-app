package com.example.movie_booking_app.view.fragment.staff

import android.content.Intent
import android.os.Bundle
import com.example.movie_booking_app.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.databinding.FragmentStaffProfileBinding
import com.example.movie_booking_app.view.activity.ChangePasswordActivity
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import com.example.movie_booking_app.viewmodel.staff.StaffScheduleViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffProfileFragment : Fragment() {

    private var _binding: FragmentStaffProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val scheduleViewModel: StaffScheduleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        observeViewModel()
        refreshProfileData()
    }

    override fun onResume() {
        super.onResume()
        refreshProfileData()
    }

    private fun refreshProfileData() {
        viewModel.loadUserProfile()
        scheduleViewModel.loadShifts()
    }

    private fun setupListeners() {
        binding.btnSaveChanges.setOnClickListener {
            val email = binding.fieldEmail.getValue()
            val phone = binding.fieldPhone.getValue()
            val fullName = binding.fieldFullName.getValue()
            val address = binding.fieldAddress.getValue()
            if (validateInput(fullName, email, phone)) {
                viewModel.updateProfile(fullName, email, phone, address)
            }
        }
        binding.btnChangePassword.setOnClickListener {
            startActivity(Intent(requireContext(), ChangePasswordActivity::class.java))
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
                    binding.fieldAddress.setValue(it.address ?: "")

                    binding.tvStaffKpi.text = (it.kpiPoints ?: 0).toString()
                    binding.tvStaffTickets.text = (it.ticketSold ?: 0).toString()
                    binding.tvStaffRating.text = String.format(Locale.US, "%.1f", it.rating ?: 0.0)

                    val initials = it.fullName?.split(" ")
                        ?.mapNotNull { word -> word.firstOrNull()?.uppercase() }
                        ?.take(2)?.joinToString("") ?: "S"
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                scheduleViewModel.shifts.collectLatest { shifts ->
                    if (shifts.isNotEmpty()) {
                        val firstShift = shifts.first()
                        // Update rating if not already set by profile API
                        if (binding.tvStaffRating.text == "0.0" || binding.tvStaffRating.text == "0,0") {
                            val rating = firstShift.rating ?: 0.0
                            binding.tvStaffRating.text = String.format(Locale.US, "%.1f", rating)
                        }
                        // Update KPI if not already set
                        if (binding.tvStaffKpi.text == "0") {
                            val kpi = firstShift.kpiPoints ?: 0
                            binding.tvStaffKpi.text = kpi.toString()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
