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
        
        edtDob.addTextChangedListener(object : android.text.TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isFormatting) return
                isFormatting = true

                val str = s?.toString()?.replace("-", "")?.replace("/", "") ?: ""
                val builder = StringBuilder()
                for (i in str.indices) {
                    if (i == 8) break
                    if (i == 2 || i == 4) {
                        builder.append("-")
                    }
                    builder.append(str[i])
                }
                
                val newText = builder.toString()
                if (s?.toString() != newText) {
                    edtDob.setText(newText)
                    edtDob.setSelection(newText.length)
                }
                
                isFormatting = false
            }
        })
        
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

        tilDob.setStartIconOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%02d-%02d-%04d", selectedDay, selectedMonth + 1, selectedYear)
                    edtDob.setText(formattedDate)
                    tilDob.error = null
                },
                year,
                month,
                day
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        btnRegister.setOnClickListener {
            if (validateFields()) {
                val username = edtUsername.text.toString().trim()
                val fullName = edtFullName.text.toString().trim()
                val dobStr = edtDob.text.toString().trim()
                var dob = dobStr
                try {
                    val sdfIn = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                    val sdfOut = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdfIn.parse(dobStr)?.let { dob = sdfOut.format(it) }
                } catch (e: Exception) {}
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
                                val errorStr = response.errorBody()?.string()
                                var errorMessage = "Registration failed"
                                try {
                                    if (errorStr != null) {
                                        val jsonObject = org.json.JSONObject(errorStr)
                                        errorMessage = jsonObject.optString("message", jsonObject.optString("error", errorStr))
                                    }
                                } catch (e: Exception) {
                                    errorMessage = errorStr ?: "Registration failed"
                                }

                                val lowerError = errorMessage.lowercase()
                                val isExistError = lowerError.contains("exist") || lowerError.contains("tồn tại") || lowerError.contains("already") || lowerError.contains("duplicate")
                                
                                if (isExistError) {
                                    var matched = false
                                    if (lowerError.contains("username") || lowerError.contains("tài khoản")) {
                                        findViewById<TextInputLayout>(R.id.tilUsernameReg).error = "Username already exists"
                                        findViewById<TextInputEditText>(R.id.edtUsername).setText("")
                                        matched = true
                                    }
                                    if (lowerError.contains("email")) {
                                        findViewById<TextInputLayout>(R.id.tilEmail).error = "Email already exists"
                                        findViewById<TextInputEditText>(R.id.edtEmail).setText("")
                                        matched = true
                                    }
                                    if (lowerError.contains("phone") || lowerError.contains("điện thoại") || lowerError.contains("sdt")) {
                                        findViewById<TextInputLayout>(R.id.tilPhone).error = "Phone number already exists"
                                        findViewById<TextInputEditText>(R.id.edtPhone).setText("")
                                        matched = true
                                    }
                                    if (lowerError.contains("id") || lowerError.contains("identity") || lowerError.contains("cccd") || lowerError.contains("cmnd")) {
                                        findViewById<TextInputLayout>(R.id.tilIdCard).error = "ID Card already exists"
                                        findViewById<TextInputEditText>(R.id.edtIdCard).setText("")
                                        matched = true
                                    }
                                    
                                    if (matched) {
                                        showNotification("Please use a different value", false)
                                    } else {
                                        showNotification(errorMessage, false)
                                    }
                                } else {
                                    showNotification(errorMessage, false)
                                }
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
            tilUsername.error = "Username must be between 3 and 30 characters"
            edtUsername.setText("")
            isValid = false
        } else {
            tilUsername.error = null
        }

        val tilFullName = findViewById<TextInputLayout>(R.id.tilFullName)
        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        val fullName = edtFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            tilFullName.error = "Full Name is required"
            isValid = false
        } else if (fullName.any { it.isDigit() }) {
            tilFullName.error = "Full Name cannot contain numbers"
            edtFullName.setText("")
            isValid = false
        } else {
            tilFullName.error = null
        }

        val tilDob = findViewById<TextInputLayout>(R.id.tilDob)
        val edtDob = findViewById<TextInputEditText>(R.id.edtDob)
        val dobStr = edtDob.text.toString().trim()
        if (dobStr.isEmpty()) {
            tilDob.error = "Date of birth is required"
            isValid = false
        } else {
            try {
                val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                sdf.isLenient = false
                val dobDate = sdf.parse(dobStr)
                if (dobDate == null) {
                    tilDob.error = "Invalid date"
                    edtDob.setText("")
                    isValid = false
                } else {
                    val dobCalendar = java.util.Calendar.getInstance()
                    dobCalendar.time = dobDate
                    val today = java.util.Calendar.getInstance()
                    
                    var age = today.get(java.util.Calendar.YEAR) - dobCalendar.get(java.util.Calendar.YEAR)
                    if (today.get(java.util.Calendar.DAY_OF_YEAR) < dobCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                        age--
                    }
                    
                    if (dobDate.after(today.time)) {
                        tilDob.error = "Date of birth cannot be in the future"
                        edtDob.setText("")
                        isValid = false
                    } else if (age <= 15) {
                        tilDob.error = "You must be older than 15 years"
                        edtDob.setText("")
                        isValid = false
                    } else {
                        tilDob.error = null
                    }
                }
            } catch (e: Exception) {
                if (dobStr.matches(Regex("^\\d{2}-\\d{2}-\\d{4}$"))) {
                    tilDob.error = "Invalid date"
                } else {
                    tilDob.error = "Format must be DD-MM-YYYY"
                }
                edtDob.setText("")
                isValid = false
            }
        }

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val email = edtEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Invalid email format"
            edtEmail.setText("")
            isValid = false
        } else {
            tilEmail.error = null
        }

        val tilPhone = findViewById<TextInputLayout>(R.id.tilPhone)
        val edtPhone = findViewById<TextInputEditText>(R.id.edtPhone)
        val phone = edtPhone.text.toString().trim()
        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
        } else if (!phone.startsWith("0")) {
            tilPhone.error = "Phone number must start with 0"
            edtPhone.setText("")
            isValid = false
        } else if (phone.length != 10) {
            tilPhone.error = "Phone number must be exactly 10 digits"
            edtPhone.setText("")
            isValid = false
        } else if (!phone.matches(Regex("^0\\d{9}$"))) {
            tilPhone.error = "Invalid phone number format"
            edtPhone.setText("")
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
        } else if (idCard.length != 12) {
            tilIdCard.error = "ID Card must be exactly 12 digits"
            edtIdCard.setText("")
            isValid = false
        } else if (!idCard.matches(Regex("^[0-9]{12}$"))) {
            tilIdCard.error = "Invalid ID Card format"
            edtIdCard.setText("")
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
            tilPassword.error = "Password must be at least 6 characters"
            edtPassword.setText("")
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
            edtConfirmPassword.setText("")
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
