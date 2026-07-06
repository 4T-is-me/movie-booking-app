package com.example.movie_booking_app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.view.activity.LoginActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AuthUtils {

    fun showLogoutConfirmationDialog(activity: Activity) {
        val dialog = android.app.Dialog(activity)
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_confirm_logout)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnCancelLogout)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnConfirmLogout)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
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
        Toast.makeText(activity, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(activity, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        activity.startActivity(intent)
        activity.finish()
    }
}
