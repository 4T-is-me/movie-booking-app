package com.example.movie_booking_app.view.activity

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.databinding.ActivityChangePasswordBinding
import com.example.movie_booking_app.viewmodel.ChangePasswordUiState
import com.example.movie_booking_app.viewmodel.ChangePasswordViewModel
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
        }

        binding.edtNewPassword.doAfterTextChanged { text ->
            viewModel.onNewPasswordChanged(text?.toString() ?: "")
        }

        binding.edtConfirmPassword.doAfterTextChanged { text ->
            viewModel.onConfirmPasswordChanged(text?.toString() ?: "")
        }

        binding.edtCurrentPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvCurrentPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.cinema_primary))
            } else {
                binding.tvCurrentPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.auto_color_9E9E9E))
            }
        }

        binding.edtNewPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvNewPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.cinema_primary))
            } else {
                binding.tvNewPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.auto_color_9E9E9E))
            }
        }

        binding.edtConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.tvConfirmPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.cinema_primary))
            } else {
                binding.tvConfirmPasswordLabel.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.auto_color_9E9E9E))
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
                    
                    binding.btnUpdatePassword.isEnabled = !state.isLoading && state.confirmMatches

                    if (state.isSuccess) {
                        Toast.makeText(this@ChangePasswordActivity, getString(R.string.success_password_updated), Toast.LENGTH_SHORT).show()
                        binding.edtCurrentPassword.setText("")
                        binding.edtNewPassword.setText("")
                        binding.edtConfirmPassword.setText("")
                        binding.edtCurrentPassword.clearFocus()
                        binding.edtNewPassword.clearFocus()
                        binding.edtConfirmPassword.clearFocus()
                        hideKeyboard()
                        viewModel.resetState()
                        finish()
                    }

                    if (state.error != null) {
                        showError(getString(state.error))
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
            binding.btnUpdatePassword.text = getString(R.string.auto_str_update_password)
            binding.progressLoading.visibility = View.GONE
        }
    }

    private fun updateValidationCard(state: ChangePasswordUiState) {
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
            binding.cardRequirements.strokeColor = ContextCompat.getColor(this@ChangePasswordActivity, R.color.cinema_primary)
            binding.cardRequirements.strokeWidth = dpToPx(2)
        } else {
            binding.cardRequirements.strokeColor = ContextCompat.getColor(this@ChangePasswordActivity, R.color.divider_gray)
            binding.cardRequirements.strokeWidth = dpToPx(1)
        }
    }

    private fun updateRequirementItem(isValid: Boolean, iconView: ImageView, textView: TextView) {
        if (isValid) {
            iconView.setImageResource(R.drawable.ic_check_circle_gold)
            iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ChangePasswordActivity, R.color.iconPlus))
            textView.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.iconPlus))
        } else {
            iconView.setImageResource(R.drawable.ic_circle_gray)
            iconView.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(this@ChangePasswordActivity, R.color.admin_hint_gray))
            textView.setTextColor(ContextCompat.getColor(this@ChangePasswordActivity, R.color.admin_hint_gray))
        }
    }

    private fun updateInlineErrors(state: ChangePasswordUiState) {
        binding.tilCurrentPassword.error = state.currentPasswordError?.let { getString(it) }
        binding.tilNewPassword.error = state.newPasswordError?.let { getString(it) }
        binding.tilConfirmPassword.error = state.confirmPasswordError?.let { getString(it) }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun handleSessionExpired() {
        Toast.makeText(this, getString(R.string.error_session_expired), Toast.LENGTH_LONG).show()
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
