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
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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
                            Toast.makeText(this@RegisterActivity, "Registration successful", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnRegister.isEnabled = true
                        Toast.makeText(this@RegisterActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
