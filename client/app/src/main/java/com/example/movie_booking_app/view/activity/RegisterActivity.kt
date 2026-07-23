package com.example.movie_booking_app.view.activity

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.FcmTokenHelper
import com.example.movie_booking_app.data.model.request.RegisterRequest
import com.example.movie_booking_app.databinding.ActivityRegisterBinding
import com.example.movie_booking_app.viewmodel.RegisterUiState
import com.example.movie_booking_app.viewmodel.RegisterViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    @Inject
    lateinit var fcmTokenHelper: FcmTokenHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.edtDob.addTextChangedListener(object : android.text.TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (isUpdating) return
                isUpdating = true

                val originalText = s.toString()
                val cursorPosition = binding.edtDob.selectionEnd

                var digitsBeforeCursor = 0
                for (i in 0 until cursorPosition) {
                    if (i < originalText.length && originalText[i].isDigit()) {
                        digitsBeforeCursor++
                    }
                }

                val digitsOnly = originalText.replace("/", "")
                val formatted = java.lang.StringBuilder()

                for (i in digitsOnly.indices) {
                    if (i == 2 || i == 4) {
                        formatted.append("/")
                    }
                    formatted.append(digitsOnly[i])
                }

                val formattedString = formatted.toString()
                binding.edtDob.setText(formattedString)

                var newCursorPosition = 0
                var digitsCount = 0
                while (newCursorPosition < formattedString.length && digitsCount < digitsBeforeCursor) {
                    if (formattedString[newCursorPosition].isDigit()) {
                        digitsCount++
                    }
                    newCursorPosition++
                }

                if (newCursorPosition < formattedString.length && formattedString[newCursorPosition] == '/') {
                    newCursorPosition++
                }

                binding.edtDob.setSelection(newCursorPosition.coerceIn(0, formattedString.length))

                isUpdating = false
            }
        })

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
                val password = binding.edtPassword.text.toString()
                val address = binding.edtAddress.text.toString().trim()

                val selectedGenderId = binding.rgGender.checkedRadioButtonId
                val gender = when (selectedGenderId) {
                    R.id.rbMale -> "Male"
                    R.id.rbFemale -> "Female"
                    else -> "Male"
                }

                var formattedApiDob = dob
                try {
                    val inputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val date = inputFormat.parse(dob)
                    if (date != null) {
                        formattedApiDob = outputFormat.format(date)
                    }
                } catch (e: Exception) {
                    // Ignored
                }

                val request = RegisterRequest(
                    username = username,
                    password = password,
                    fullName = fullName,
                    dateOfBirth = formattedApiDob,
                    gender = gender,
                    identityCard = idCard,
                    email = email,
                    address = address,
                    phoneNumber = phone
                )

                viewModel.register(request)
            }
        }


        observeViewModel()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is RegisterUiState.Idle -> {
                        binding.btnRegister.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                    }
                    is RegisterUiState.Loading -> {
                        binding.btnRegister.isEnabled = false
                        binding.pbLoading.visibility = View.VISIBLE
                    }
                    is RegisterUiState.Success -> {
                        binding.btnRegister.isEnabled = true
                        binding.pbLoading.visibility = View.GONE

                        // Regular registration: user is redirected to OTP verification first.
                        // No JWT yet, so FCM token will be sent after the first login.
                        showNotification("Please check your email for the OTP!", true)
                        val intent = Intent(this@RegisterActivity, VerifyOtpActivity::class.java)
                        intent.putExtra("email", binding.edtEmail.text.toString().trim())
                        intent.putExtra("type", "register")
                        startActivity(intent)
                        finish()

                        viewModel.resetState()
                    }
                    is RegisterUiState.Error -> {
                        binding.btnRegister.isEnabled = true
                        binding.pbLoading.visibility = View.GONE
                        showNotification(state.message, false)
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var isValid = true

        val username = binding.edtUsername.text.toString().trim()
        if (username.isEmpty()) {
            binding.tilUsernameReg.error = getString(R.string.error_username_required)
            isValid = false
        } else if (username.length !in 3..30) {
            binding.tilUsernameReg.error = getString(R.string.error_username_length)
            isValid = false
        } else {
            binding.tilUsernameReg.error = null
        }

        if (binding.edtFullName.text.toString().trim().isEmpty()) {
            binding.tilFullName.error = getString(R.string.error_fullname_required)
            isValid = false
        } else {
            binding.tilFullName.error = null
        }

        if (!validateDobField(binding.edtDob, binding.tilDob)) {
            isValid = false
        }

        val email = binding.edtEmail.text.toString().trim()
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_invalid)
            isValid = false
        } else {
            binding.tilEmail.error = null
        }

        val phone = binding.edtPhone.text.toString().trim()
        if (phone.isEmpty()) {
            binding.tilPhone.error = getString(R.string.error_phone_required)
            isValid = false
        } else if (!phone.matches(Regex("^0\\d{9}$"))) {
            binding.tilPhone.error = getString(R.string.error_phone_invalid)
            isValid = false
        } else {
            binding.tilPhone.error = null
        }

        val password = binding.edtPassword.text.toString()
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_length)
            isValid = false
        } else {
            binding.tilPassword.error = null
        }

        val confirmPassword = binding.edtConfirmPassword.text.toString()
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = getString(R.string.error_confirm_password_required)
            isValid = false
        } else if (confirmPassword != password) {
            binding.tilConfirmPassword.error = getString(R.string.error_confirm_password_mismatch)
            isValid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (binding.edtAddress.text.toString().trim().isEmpty()) {
            binding.tilAddress.error = getString(R.string.error_address_required)
            isValid = false
        } else {
            binding.tilAddress.error = null
        }

        val idCard = binding.edtIdCard.text.toString().trim()
        if (idCard.isEmpty()) {
            binding.tilIdCard.error = getString(R.string.error_idcard_required)
            isValid = false
        } else if (idCard.length != 9 && idCard.length != 12) {
            binding.tilIdCard.error = getString(R.string.error_idcard_invalid)
            isValid = false
        } else {
            binding.tilIdCard.error = null
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

                val maxValidDate = Calendar.getInstance()
                maxValidDate.add(Calendar.YEAR, -15)

                val minValidDate = Calendar.getInstance()
                minValidDate.add(Calendar.YEAR, -80)

                if (selectedCalendar.after(today)) {
                    tilDob.error = getString(R.string.error_dob_future)
                    edtDob.setText("")
                } else if (selectedCalendar.after(maxValidDate)) {
                    tilDob.error = getString(R.string.error_dob_min_age)
                    edtDob.setText("")
                } else if (selectedCalendar.before(minValidDate)) {
                    tilDob.error = getString(R.string.error_dob_max_age)
                    edtDob.setText("")
                } else {
                    val formattedDate = String.format(Locale.US, "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    edtDob.setText(formattedDate)
                    tilDob.error = null
                }
            },
            year,
            month,
            day
        )
        
        val maxValidDate = Calendar.getInstance()
        maxValidDate.add(Calendar.YEAR, -15)
        
        val minValidDate = Calendar.getInstance()
        minValidDate.add(Calendar.YEAR, -80)
        
        datePickerDialog.datePicker.maxDate = maxValidDate.timeInMillis
        datePickerDialog.datePicker.minDate = minValidDate.timeInMillis
        
        datePickerDialog.show()
    }

    private fun validateDobField(edtDob: TextInputEditText, tilDob: TextInputLayout): Boolean {
        val dob = edtDob.text.toString().trim()
        if (dob.isEmpty()) {
            tilDob.error = getString(R.string.error_dob_required)
            return false
        }

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
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

            val maxValidDate = Calendar.getInstance()
            maxValidDate.set(Calendar.HOUR_OF_DAY, 0)
            maxValidDate.set(Calendar.MINUTE, 0)
            maxValidDate.set(Calendar.SECOND, 0)
            maxValidDate.set(Calendar.MILLISECOND, 0)
            maxValidDate.add(Calendar.YEAR, -15)

            val minValidDate = Calendar.getInstance()
            minValidDate.set(Calendar.HOUR_OF_DAY, 0)
            minValidDate.set(Calendar.MINUTE, 0)
            minValidDate.set(Calendar.SECOND, 0)
            minValidDate.set(Calendar.MILLISECOND, 0)
            minValidDate.add(Calendar.YEAR, -80)

            if (selectedCalendar.after(today)) {
                tilDob.error = getString(R.string.error_dob_future)
                false
            } else if (selectedCalendar.after(maxValidDate)) {
                tilDob.error = getString(R.string.error_dob_min_age)
                false
            } else if (selectedCalendar.before(minValidDate)) {
                tilDob.error = getString(R.string.error_dob_max_age)
                false
            } else {
                tilDob.error = null
                true
            }
        } catch (_: Exception) {
            tilDob.error = getString(R.string.error_dob_invalid)
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
