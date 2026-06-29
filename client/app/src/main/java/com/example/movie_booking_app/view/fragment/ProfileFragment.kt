package com.example.movie_booking_app.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.databinding.ActivityProfileBinding
import com.example.movie_booking_app.view.activity.LoginActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: ActivityProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private var progressDialog: android.app.Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityProfileBinding.inflate(inflater, container, false)
        // Hide local redundant bottom navigation
        binding.bottomNav.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        progressDialog = null
        _binding = null
    }

    private fun setupClickListeners() {
        binding.ivEditProfile.setOnClickListener {
            val profile = viewModel.userProfile.value
            val context = requireContext()
            
            val builder = androidx.appcompat.app.AlertDialog.Builder(context)
            builder.setTitle("Edit Profile")
            
            val container = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                setPadding(48, 24, 48, 24)
            }
            
            val tvEmailLabel = android.widget.TextView(context).apply {
                text = "Email"
                setTextColor(android.graphics.Color.WHITE)
            }
            val etEmail = android.widget.EditText(context).apply {
                setText(profile?.email ?: "")
                setTextColor(android.graphics.Color.WHITE)
                setHintTextColor(android.graphics.Color.GRAY)
                hint = "Enter email address"
            }
            
            val tvPhoneLabel = android.widget.TextView(context).apply {
                text = "Phone Number"
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 24, 0, 0)
            }
            val etPhone = android.widget.EditText(context).apply {
                setText(profile?.phone ?: "")
                setTextColor(android.graphics.Color.WHITE)
                setHintTextColor(android.graphics.Color.GRAY)
                hint = "Enter phone number"
            }

            val tvAddressLabel = android.widget.TextView(context).apply {
                text = "Address"
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 24, 0, 0)
            }
            val etAddress = android.widget.EditText(context).apply {
                setText(profile?.address ?: "")
                setTextColor(android.graphics.Color.WHITE)
                setHintTextColor(android.graphics.Color.GRAY)
                hint = "Enter address"
            }

            container.addView(tvEmailLabel)
            container.addView(etEmail)
            container.addView(tvPhoneLabel)
            container.addView(etPhone)
            container.addView(tvAddressLabel)
            container.addView(etAddress)
            
            builder.setView(container)
            
            builder.setPositiveButton("Save") { dialog, _ ->
                val newEmail = etEmail.text.toString().trim()
                val newPhone = etPhone.text.toString().trim()
                val newAddress = etAddress.text.toString().trim()
                
                if (newEmail.isEmpty()) {
                    Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (newPhone.isEmpty()) {
                    Toast.makeText(context, "Phone number cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                viewModel.updateProfile(
                    email = newEmail,
                    phone = newPhone,
                    address = if (newAddress.isEmpty()) null else newAddress
                )
                dialog.dismiss()
            }
            
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            
            val dialog = builder.create()
            dialog.setOnShowListener {
                dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#121212")))
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.parseColor("#FFC107"))
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.WHITE)
            }
            dialog.show()
        }

        binding.layoutMyTicket.setOnClickListener {
            Toast.makeText(context, "My Ticket Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.layoutChangeLanguage.setOnClickListener {
            Toast.makeText(context, "Change Language Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.layoutChangePassword.setOnClickListener {
            Toast.makeText(context, "Change Password Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutLogout.setOnClickListener {
            val act = requireActivity()
            TokenManager.clearTokens(act)
            val sharedPref = act.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("auth_token")
                remove("username")
                remove("role")
                apply()
            }
            val intent = Intent(act, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            act.finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        val act = activity ?: return
        if (isLoading) {
            if (progressDialog == null) {
                progressDialog = android.app.Dialog(act).apply {
                    setContentView(android.widget.ProgressBar(act))
                    window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    setCancelable(false)
                }
            }
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { userProfile ->
                    binding.tvName.text = userProfile?.fullName ?: "-"
                    binding.tvPhone.text = userProfile?.phone ?: "-"
                    binding.tvEmail.text = userProfile?.email ?: "-"
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        viewModel.resetError()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateSuccess.collect { success ->
                    if (success) {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateSuccess()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isUnauthorized.collect { unauthorized ->
                    if (unauthorized) {
                        val act = activity ?: return@collect
                        Toast.makeText(act, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        TokenManager.clearTokens(act)
                        val sharedPref = act.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("auth_token")
                            remove("username")
                            remove("role")
                            apply()
                        }
                        val intent = Intent(act, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        act.finish()
                    }
                }
            }
        }
    }
}
