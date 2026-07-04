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
import android.app.Activity
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.example.movie_booking_app.data.model.request.GoogleLoginRequest
import com.example.movie_booking_app.data.local.TokenManager
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                handleGoogleLogin(idToken)
            } ?: showNotification("Google token is null", false)
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            val errorMsg = when (statusCode) {
                10 -> "Developer Error (10): Mismatched SHA-1 or Client ID"
                12500 -> "Sign In Failed (12500): Check Consent Screen or SHA-1"
                12501 -> "Sign In Canceled (12501): User closed the dialog"
                16 -> "Sign In Canceled (16): No network or disabled"
                else -> "Google sign in failed: $statusCode"
            }
            showNotification(errorMsg, false)
        }
    }

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
                        val otpReq = com.example.movie_booking_app.data.model.request.OtpRequest(email)
                        val otpRes = RetrofitClient.authApi.sendOtp(otpReq)
                        withContext(Dispatchers.Main) {
                            btnRegister.isEnabled = true
                            if (otpRes.isSuccessful) {
                                showNotification("Đã gửi OTP đến email", true)
                                showOtpDialog(email, request)
                            } else {
                                showNotification("Lỗi gửi OTP", false)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            btnRegister.isEnabled = true
                            showNotification("Lỗi kết nối", false)
                        }
                    }
                }
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(com.example.movie_booking_app.BuildConfig.GOOGLE_CLIENT_ID.trim())
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleRegister = findViewById<Button>(R.id.btnGoogleRegister)
        btnGoogleRegister.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun showOtpDialog(email: String, request: RegisterRequest) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Xác thực OTP")
        builder.setMessage("Nhập mã 6 số đã gửi tới $email")
        
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.filters = arrayOf(android.text.InputFilter.LengthFilter(6))
        val padding = (16 * resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)
        builder.setView(input)
        
        builder.setPositiveButton("Xác nhận", null) 
        builder.setNegativeButton("Hủy") { dialog, _ -> dialog.cancel() }
        
        val dialog = builder.create()
        dialog.show()
        
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val otpCode = input.text.toString().trim()
            if (otpCode.length != 6) {
                showNotification("OTP phải có 6 số", false)
                return@setOnClickListener
            }
            
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false
            
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val verifyReq = com.example.movie_booking_app.data.model.request.VerifyOtpRequest(email, otpCode)
                    val verifyRes = RetrofitClient.authApi.verifyOtp(verifyReq)
                    withContext(Dispatchers.Main) {
                        if (verifyRes.isSuccessful) {
                            dialog.dismiss()
                            performRegistration(request)
                        } else {
                            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                            showNotification("Mã OTP sai hoặc đã hết hạn", false)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        showNotification("Lỗi kết nối", false)
                    }
                }
            }
        }
    }

    private fun performRegistration(request: RegisterRequest) {
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        btnRegister.isEnabled = false
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

    private fun handleGoogleLogin(idToken: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = GoogleLoginRequest(idToken)
                val response = RetrofitClient.authApi.googleLogin(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()
                        val token = body?.accessToken
                        if (token != null) {
                            val dbUsername = body?.account?.username ?: ""
                            val roleNameStr = body?.account?.roleId?.roleName ?: body?.account?.role ?: "user"
                            val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            with(sharedPref.edit()) {
                                putString("auth_token", token)
                                putString("username", dbUsername)
                                putString("role", roleNameStr)
                                apply()
                            }
                            TokenManager.saveTokens(
                                this@RegisterActivity,
                                token,
                                body?.refreshToken ?: ""
                            )
                        }
                        showNotification("Google Registration successful", true)
                        val roleName = body?.account?.roleId?.roleName ?: body?.account?.role
                        if (roleName != null && roleName.contains("admin", ignoreCase = true)) {
                            val intent = Intent(this@RegisterActivity, AdminActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        val errorStr = response.errorBody()?.string()
                        val message = try {
                            if (errorStr != null && errorStr.trim().startsWith("{")) {
                                JSONObject(errorStr).optString("message", "Google Registration Failed")
                            } else {
                                "Google Registration Failed"
                            }
                        } catch (e: Exception) {
                            "Google Registration Failed"
                        }
                        showNotification(message, false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showNotification("Connection error: ${e.localizedMessage}", false)
                }
            }
        }
    }
}
