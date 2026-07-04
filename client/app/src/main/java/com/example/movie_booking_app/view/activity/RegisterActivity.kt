package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.RegisterRequest
import com.example.movie_booking_app.databinding.ActivityRegisterBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish()
        }

        // Initialize state: Hide password by default and set "eye closed" icon
        binding.edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)

        binding.edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        binding.tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)

        // Handle password visibility manually
        binding.tilPassword.setEndIconOnClickListener {
            val selection = binding.edtPassword.selectionEnd
            if (binding.edtPassword.transformationMethod is PasswordTransformationMethod) {
                binding.edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.tilPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                binding.edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            binding.edtPassword.setSelection(selection)
        }

        binding.tilConfirmPassword.setEndIconOnClickListener {
            val selection = binding.edtConfirmPassword.selectionEnd
            if (binding.edtConfirmPassword.transformationMethod is PasswordTransformationMethod) {
                binding.edtConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                binding.edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            binding.edtConfirmPassword.setSelection(selection)
        }

        binding.edtDob.setOnClickListener {
            showDatePicker(binding.edtDob, binding.tilDob)
        }

        binding.edtDob.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateDobField(binding.edtDob, binding.tilDob)
            }
        }

        binding.btnRegister.setOnClickListener {
            if (validateFields()) {
                val username = binding.edtUsername.text.toString().trim()
                val fullName = binding.edtFullName.text.toString().trim()
                val dob = binding.edtDob.text.toString().trim()
                val email = binding.edtEmail.text.toString().trim()
                val phone = binding.edtPhone.text.toString().trim()
                val idCard = binding.edtIdCard.text.toString().trim()
                val password = binding.edtPassword.text.toString().trim()
                val address = binding.edtAddress.text.toString().trim()

                val selectedGenderId = binding.rgGender.checkedRadioButtonId
                val gender = when (selectedGenderId) {
                    R.id.rbMale -> "Male"
                    R.id.rbFemale -> "Female"
                    else -> "Male"
                }

                binding.btnRegister.isEnabled = false

                val request = RegisterRequest(
                    username = username,
                    password = password,
                    fullName = fullName,
                    dateOfBirth = dob,
                    gender = gender,
                    identityCard = idCard,
                    email = email,
                    address = address,
                    phoneNumber = phone
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = RetrofitClient.authApi.register(request)

                        withContext(Dispatchers.Main) {
                            binding.btnRegister.isEnabled = true
                            if (response.isSuccessful && response.body() != null) {
                                showNotification("Registration successful!", true)
                                binding.btnRegister.postDelayed({ finish() }, 1500)
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                                showNotification(errorMsg, false)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            binding.btnRegister.isEnabled = true
                            showNotification("Error: ${e.message}", false)
                        }
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val username = binding.edtUsername.text.toString().trim()
        if (username.isEmpty()) {
            binding.tilUsernameReg.error = "Username is required"
            isValid = false
        } else if (username.length !in 3..30) {
            binding.tilUsernameReg.error = "Username must be 3-30 characters"
            isValid = false
        } else {
            binding.tilUsernameReg.error = null
        }

        if (binding.edtFullName.text.toString().trim().isEmpty()) {
            binding.tilFullName.error = "Full Name is required"
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (!validateDobField(binding.edtDob, binding.tilDob)) {
            isValid = false
        }

        val email = binding.edtEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        val phone = binding.edtPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone is required"
            isValid = false
        } else if (!phone.matches(Regex("^0\\d{9}$"))) {
            binding.tilPhone.error = "Must be 10 digits starting with 0"
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        val password = binding.edtPassword.text.toString().trim()
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "At least 6 characters"
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        val confirmPassword = binding.edtConfirmPassword.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm password"
            isValid = false
        } else if (confirmPassword != password) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (binding.edtAddress.text.toString().trim().isEmpty()) {
            binding.tilAddress.error = "Address is required"
            isValid = false
        } else {
            binding.tilAddress.error = null
        }

        return isValid
    }

    private fun showDatePicker(edtDob: TextInputEditText, tilDob: TextInputLayout) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)

                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)

                if (selectedCalendar.after(today)) {
                    tilDob.error = "Date of birth cannot be in the future"
                    edtDob.setText("")
                } else {
                    val formattedDate = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    edtDob.setText(formattedDate)
                    tilDob.error = null
                }
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun validateDobField(edtDob: TextInputEditText, tilDob: TextInputLayout): Boolean {
        val dob = edtDob.text.toString().trim()
        if (dob.isEmpty()) {
            tilDob.error = "Date of birth is required"
            return false
        }

        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        formatter.isLenient = false

        return try {
            val parsedDate = formatter.parse(dob)
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)

            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.time = parsedDate
            selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
            selectedCalendar.set(Calendar.MINUTE, 0)
            selectedCalendar.set(Calendar.SECOND, 0)
            selectedCalendar.set(Calendar.MILLISECOND, 0)

            if (selectedCalendar.after(today)) {
                tilDob.error = "Date of birth cannot be in the future"
                false
            } else {
                tilDob.error = null
                true
            }
        } catch (_: Exception) {
            tilDob.error = "Invalid date format (yyyy-MM-dd)"
            false
        }
    }

    private var hideNotificationRunnable: Runnable? = null

    private fun showNotification(message: String, isSuccess: Boolean) {
        binding.tvNotificationMessage.text = message
        if (isSuccess) {
            binding.llNotificationBg.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
        } else {
            binding.llNotificationBg.setBackgroundColor(Color.parseColor("#F44336")) // Red
        }

        binding.cvNotification.visibility = View.VISIBLE

        if (hideNotificationRunnable != null) {
            binding.cvNotification.removeCallbacks(hideNotificationRunnable)
        }

        hideNotificationRunnable = Runnable {
            binding.cvNotification.visibility = View.GONE
        }
        binding.cvNotification.postDelayed(hideNotificationRunnable, 3000)
    }
}
