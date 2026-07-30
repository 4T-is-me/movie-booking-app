package com.example.movie_booking_app.utils

import com.example.movie_booking_app.R

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.view.activity.LoginActivity

object AuthUtils {

    fun showLogoutConfirmationDialog(activity: Activity) {
        val binding = com.example.movie_booking_app.databinding.DialogConfirmLogoutBinding.inflate(activity.layoutInflater)
        val dialog = android.app.Dialog(activity)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.window?.setLayout(
            (320 * activity.resources.displayMetrics.density).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        binding.btnCancelLogout.setOnClickListener {
            dialog.dismiss()
        }

        binding.btnConfirmLogout.setOnClickListener {
            dialog.dismiss()
            performLogout(activity)
        }

        dialog.show()
    }

    fun performLogout(activity: Activity) {
        TokenManager.clearTokens(activity)
        val sharedPref = activity.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("auth_token")
            remove("username")
            remove("role")
            apply()
        }
        Toast.makeText(activity, activity.getString(R.string.auto_str_logged_out_successfully), Toast.LENGTH_SHORT).show()
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
