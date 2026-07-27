package com.example.movie_booking_app.utils

import android.util.Patterns
import com.example.movie_booking_app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

    fun validateLoginId(loginId: String?): Int? {
        val trimmed = loginId?.trim() ?: ""
        return if (trimmed.isEmpty()) R.string.error_username_required else null
    }

    fun validateUsername(username: String?): Int? {
        val trimmed = username?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_username_required
            trimmed.length !in 3..30 -> R.string.error_username_length
            !trimmed.matches(Regex("^[a-zA-Z0-9_]+$")) -> R.string.error_username_invalid
            else -> null
        }
    }

    fun validateFullName(fullName: String?): Int? {
        val trimmed = fullName?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_fullname_required
            trimmed.contains(Regex("\\d")) -> R.string.error_fullname_invalid
            else -> null
        }
    }

    fun validateEmail(email: String?): Int? {
        val trimmed = email?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_email_required
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() || !trimmed.endsWith("@gmail.com", ignoreCase = true) -> R.string.error_email_invalid
            else -> null
        }
    }

    fun validatePhone(phone: String?): Int? {
        val trimmed = phone?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_phone_required
            !trimmed.matches(Regex("^0\\d{9}$")) -> R.string.error_phone_invalid
            else -> null
        }
    }

    fun validatePassword(password: String?): Int? {
        val pass = password ?: ""
        return when {
            pass.isEmpty() -> R.string.error_password_required
            pass.length < 6 -> R.string.error_password_length
            else -> null
        }
    }

    fun validateConfirmPassword(password: String?, confirm: String?): Int? {
        val conf = confirm ?: ""
        return when {
            conf.isEmpty() -> R.string.error_confirm_password_required
            conf != password -> R.string.error_confirm_password_mismatch
            else -> null
        }
    }

    fun validateAddress(address: String?): Int? {
        val trimmed = address?.trim() ?: ""
        return if (trimmed.isEmpty()) R.string.error_address_required else null
    }

    fun validateIdCard(idCard: String?): Int? {
        val trimmed = idCard?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_idcard_required
            !trimmed.matches(Regex("^[0-9]{9}$|^[0-9]{12}$")) -> R.string.error_idcard_invalid
            else -> null
        }
    }

    fun validateOtp(otp: String?): Int? {
        val trimmed = otp?.trim() ?: ""
        return when {
            trimmed.isEmpty() -> R.string.error_otp_invalid
            trimmed.length != 6 -> R.string.error_otp_invalid
            else -> null
        }
    }

    fun validateDob(dob: String?): Int? {
        val trimmed = dob?.trim() ?: ""
        if (trimmed.isEmpty()) {
            return R.string.error_dob_required
        }

        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        formatter.isLenient = false

        return try {
            val parsedDate = formatter.parse(trimmed) ?: return R.string.error_dob_invalid
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

            when {
                selectedCalendar.after(today) -> R.string.error_dob_future
                selectedCalendar.after(maxValidDate) -> R.string.error_dob_min_age
                selectedCalendar.before(minValidDate) -> R.string.error_dob_max_age
                else -> null
            }
        } catch (_: Exception) {
            R.string.error_dob_invalid
        }
    }
}
