package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.RegisterRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        
        val tilDob = findViewById<TextInputLayout>(R.id.tilDob)
        val edtDob = findViewById<TextInputEditText>(R.id.edtDob)
        
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPhone = findViewById<TextInputEditText>(R.id.edtPhone)
        val edtIdCard = findViewById<TextInputEditText>(R.id.edtIdCard)
        
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)

        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        
        val edtAddress = findViewById<TextInputEditText>(R.id.edtAddress)
        
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        // Initialize state: Hide password by default and set "eye closed" icon
        edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
        
        edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)

        // Handle password visibility manually
        tilPassword.setEndIconOnClickListener {
            val selection = edtPassword.selectionEnd
            if (edtPassword.transformationMethod is PasswordTransformationMethod) {
                edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tilPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tilPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            edtPassword.setSelection(selection)
        }

        tilConfirmPassword.setEndIconOnClickListener {
            val selection = edtConfirmPassword.selectionEnd
            if (edtConfirmPassword.transformationMethod is PasswordTransformationMethod) {
                edtConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                tilConfirmPassword.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            edtConfirmPassword.setSelection(selection)
        }

        edtDob.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    edtDob.setText(formattedDate)
                    tilDob.error = null
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        btnRegister.setOnClickListener {
            if (validateFields()) {
                val username = edtUsername.text.toString().trim()
                val fullName = edtFullName.text.toString().trim()
                val dob = edtDob.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val phone = edtPhone.text.toString().trim()
                val idCard = edtIdCard.text.toString().trim()
                val password = edtPassword.text.toString().trim()
                val confirmPassword = edtConfirmPassword.text.toString().trim()
                val address = edtAddress.text.toString().trim()

                val selectedGenderId = rgGender.checkedRadioButtonId
                val gender = when (selectedGenderId) {
                    R.id.rbMale -> "Male"
                    R.id.rbFemale -> "Female"
                    else -> "Male"
                }

                btnRegister.isEnabled = false

                val request = RegisterRequest(
                    username = username,
                    password = password,
                    confirmPassword = confirmPassword,
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
                            btnRegister.isEnabled = true
                            if (response.isSuccessful && response.body() != null) {
                                showNotification("Registration successful!", true)
                                btnRegister.postDelayed({ finish() }, 1500)
                            } else {
                                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                                showNotification(errorMsg, false)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            btnRegister.isEnabled = true
                            showNotification("Error: ${e.message}", false)
                        }
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val tilUsername = findViewById<TextInputLayout>(R.id.tilUsernameReg)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val username = edtUsername.text.toString().trim()
        if (username.isEmpty()) {
            tilUsername.error = "Username is required"
            isValid = false
        } else if (username.length !in 3..30) {
            tilUsername.error = "Username must be 3-30 characters"
            isValid = false
        } else {
            tilUsername.error = null
        }

        val tilFullName = findViewById<TextInputLayout>(R.id.tilFullName)
        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        if (edtFullName.text.toString().trim().isEmpty()) {
            tilFullName.error = "Full Name is required"
            isValid = false
        } else {
            tilFullName.error = null
        }

        val tilDob = findViewById<TextInputLayout>(R.id.tilDob)
        val edtDob = findViewById<TextInputEditText>(R.id.edtDob)
        if (edtDob.text.toString().trim().isEmpty()) {
            tilDob.error = "Required"
            isValid = false
        } else {
            tilDob.error = null
        }

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val email = edtEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Invalid email format"
            isValid = false
        } else {
            tilEmail.error = null
        }

        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)
        val edtPhone = findViewById<TextInputEditText>(R.id.edtPhone)
        val phone = edtPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Phone is required"
            isValid = false
        } else if (!phone.matches(Regex("^0\\d{9}$"))) {
            tilPhone.error = "Must be 10 digits starting with 0"
            isValid = false
        } else {
            tilPhone.error = null
        }

        val tilIdCard = findViewById<TextInputLayout>(R.id.tilIdCard)
        val edtIdCard = findViewById<TextInputEditText>(R.id.edtIdCard)
        val idCard = edtIdCard.text.toString().trim()
        if (idCard.isEmpty()) {
            tilIdCard.error = "ID Card is required"
            isValid = false
        } else if (!idCard.matches(Regex("^[0-9]{12}$"))) {
            tilIdCard.error = "Must be 12 digits"
            isValid = false
        } else {
            tilIdCard.error = null
        }

        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val password = edtPassword.text.toString().trim()
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "At least 6 characters"
            isValid = false
        } else {
            tilPassword.error = null
        }

        val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val confirmPassword = edtConfirmPassword.text.toString().trim()
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Please confirm password"
            isValid = false
        } else if (confirmPassword != password) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        } else {
            tilConfirmPassword.error = null
        }

        val tilAddress = findViewById<TextInputLayout>(R.id.tilAddress)
        val edtAddress = findViewById<TextInputEditText>(R.id.edtAddress)
        if (edtAddress.text.toString().trim().isEmpty()) {
            tilAddress.error = "Address is required"
            isValid = false
        } else {
            tilAddress.error = null
        }

        return isValid
    }

    private var hideNotificationRunnable: Runnable? = null

    private fun showNotification(message: String, isSuccess: Boolean) {
        val cvNotification = findViewById<CardView>(R.id.cvNotification)
        val llNotificationBg = findViewById<LinearLayout>(R.id.llNotificationBg)
        val tvNotificationMessage = findViewById<TextView>(R.id.tvNotificationMessage)

        tvNotificationMessage.text = message
        if (isSuccess) {
            llNotificationBg.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
        } else {
            llNotificationBg.setBackgroundColor(Color.parseColor("#F44336")) // Red
        }

        cvNotification.visibility = View.VISIBLE

        if (hideNotificationRunnable != null) {
            cvNotification.removeCallbacks(hideNotificationRunnable)
        }

        hideNotificationRunnable = Runnable {
            cvNotification.visibility = View.GONE
        }
        cvNotification.postDelayed(hideNotificationRunnable, 3000)
    }
}
