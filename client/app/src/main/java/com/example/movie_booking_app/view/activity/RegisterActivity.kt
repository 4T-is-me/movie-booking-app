package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.RegisterRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import android.graphics.Color
import android.view.View
import androidx.cardview.widget.CardView
import android.widget.LinearLayout
import android.widget.TextView

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        val edtDob = findViewById<TextInputEditText>(R.id.edtDob)
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
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }

        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPhone = findViewById<TextInputEditText>(R.id.edtPhone)
        val edtIdCard = findViewById<TextInputEditText>(R.id.edtIdCard)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val edtAddress = findViewById<TextInputEditText>(R.id.edtAddress)

        btnRegister.setOnClickListener {
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
                else -> ""
            }

            if (username.isEmpty() || fullName.isEmpty() || dob.isEmpty() || gender.isEmpty() || email.isEmpty() ||
                phone.isEmpty() || idCard.isEmpty() || password.isEmpty() || address.isEmpty()) {
                showNotification("Please fill all fields", false)
                return@setOnClickListener
            }

            if (username.length !in 3..30) {
                showNotification("Username must be between 3 and 30 characters", false)
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showNotification("Invalid email format", false)
                return@setOnClickListener
            }

            if (password.length < 6) {
                showNotification("Password must be at least 6 characters", false)
                return@setOnClickListener
            }

            if (!phone.matches(Regex("^0\\d{9}$"))) {
                showNotification("Phone number must be 10 digits starting with 0", false)
                return@setOnClickListener
            }

            if (!idCard.matches(Regex("^[0-9]{12}$"))) {
                showNotification("Identity card must be exactly 12 digits", false)
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                showNotification("Passwords do not match", false)
                return@setOnClickListener
            }

            // Simulate API Error for username "admin"
            if (username.lowercase() == "admin") {
                showNotification("Đăng ký thất bại: Username đã tồn tại trong hệ thống!", false)
                return@setOnClickListener
            }

            btnRegister.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = RegisterRequest(
                        username = username,
                        fullName = fullName,
                        dateOfBirth = dob,
                        gender = gender,
                        email = email,
                        phoneNumber = phone,
                        identityCard = idCard,
                        address = address,
                        password = password
                    )
                    val response = RetrofitClient.authApi.register(request)

                    withContext(Dispatchers.Main) {
                        btnRegister.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            showNotification("Đăng ký tài khoản thành công!", true)
                            btnRegister.postDelayed({ finish() }, 1500)
                        } else {
                            showNotification("Registration failed", false)
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
