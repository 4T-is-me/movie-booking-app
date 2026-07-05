package com.example.movie_booking_app.view.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.databinding.ActivityChangePasswordBinding
import com.example.movie_booking_app.viewmodel.ChangePasswordViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private val viewModel: ChangePasswordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupInputListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        // Hide/Clear passwords when Activity resumes for security
        binding.edtCurrentPassword.setText("")
        binding.edtNewPassword.setText("")
        binding.edtConfirmPassword.setText("")
        binding.edtCurrentPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.edtNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        
        viewModel.resetState()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupInputListeners() {
        binding.edtCurrentPassword.doAfterTextChanged { text ->
            viewModel.onCurrentPasswordChanged(text?.toString() ?: "")
            binding.tilCurrentPassword.error = null
        }

        binding.edtNewPassword.doAfterTextChanged { text ->
            viewModel.onNewPasswordChanged(text?.toString() ?: "")
        }

        binding.edtConfirmPassword.doAfterTextChanged { text ->
            viewModel.onConfirmPasswordChanged(text?.toString() ?: "")
            binding.tilConfirmPassword.error = null
        }

        binding.edtCurrentPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvCurrentPasswordLabel.setTextColor(android.graphics.Color.parseColor("#F5C542"))
            } else {
                binding.tvCurrentPasswordLabel.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }
        }

        binding.edtNewPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvNewPasswordLabel.setTextColor(android.graphics.Color.parseColor("#F5C542"))
            } else {
                binding.tvNewPasswordLabel.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }
        }

        binding.edtConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvConfirmPasswordLabel.setTextColor(android.graphics.Color.parseColor("#F5C542"))
            } else {
                binding.tvConfirmPasswordLabel.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }
        }

        binding.btnUpdatePassword.setOnClickListener {
            viewModel.submitChangePassword()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateLoadingState(state.isLoading)
                    updateValidationCard(state)
                    updateInlineErrors(state)
                    
                    binding.btnUpdatePassword.isEnabled = !state.isLoading && viewModel.isSubmitEnabled()

                    if (state.isSuccess) {
                        viewModel.resetState()
                        showSuccessDialog()
                    }

                    if (state.error != null) {
                        showError(state.error)
                    }

                    if (state.isUnauthorized) {
                        handleSessionExpired()
                    }
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.tilCurrentPassword.isEnabled = !isLoading
        binding.tilNewPassword.isEnabled = !isLoading
        binding.tilConfirmPassword.isEnabled = !isLoading
        
        if (isLoading) {
            binding.btnUpdatePassword.text = ""
            binding.progressLoading.visibility = View.VISIBLE
        } else {
            binding.btnUpdatePassword.text = "Update password"
            binding.progressLoading.visibility = View.GONE
        }
    }

    private fun updateValidationCard(state: com.example.movie_booking_app.viewmodel.ChangePasswordUiState) {
        // Requirement 1: Length
        updateRequirementItem(
            state.lengthValid,
            binding.ivReqLength,
            binding.tvReqLength
        )

        // Requirement 2: Uppercase
        updateRequirementItem(
            state.uppercaseValid,
            binding.ivReqUppercase,
            binding.tvReqUppercase
        )

        // Requirement 3: Lowercase
        updateRequirementItem(
            state.lowercaseValid,
            binding.ivReqLowercase,
            binding.tvReqLowercase
        )

        // Requirement 4: Number
        updateRequirementItem(
            state.numberValid,
            binding.ivReqNumber,
            binding.tvReqNumber
        )

        // Requirement 5: Special character
        updateRequirementItem(
            state.specialValid,
            binding.ivReqSpecial,
            binding.tvReqSpecial
        )

        // Highlight card border if all pass
        if (state.isNewPasswordValid) {
            binding.cardRequirements.strokeColor = Color.parseColor("#F5C542")
            binding.cardRequirements.strokeWidth = dpToPx(2)
        } else {
            binding.cardRequirements.strokeColor = Color.parseColor("#33FFFFFF")
            binding.cardRequirements.strokeWidth = dpToPx(1)
        }
    }

    private fun updateRequirementItem(isValid: Boolean, iconView: android.widget.ImageView, textView: android.widget.TextView) {
        if (isValid) {
            iconView.setImageResource(R.drawable.ic_check_circle_gold)
            iconView.imageTintList = null // Use default drawable gold color
            textView.setTextColor(Color.parseColor("#F5C542"))
        } else {
            iconView.setImageResource(R.drawable.ic_circle_gray)
            iconView.imageTintList = ColorStateList.valueOf(Color.parseColor("#808080"))
            textView.setTextColor(Color.parseColor("#808080"))
        }
    }

    private fun updateInlineErrors(state: com.example.movie_booking_app.viewmodel.ChangePasswordUiState) {
        binding.tilCurrentPassword.error = state.currentPasswordError
        binding.tilConfirmPassword.error = state.confirmPasswordError
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Password Updated")
            .setMessage("Your password has been changed successfully.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun handleSessionExpired() {
        Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
        TokenManager.clearTokens(this)
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("auth_token")
            remove("username")
            remove("role")
            apply()
        }
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
}
