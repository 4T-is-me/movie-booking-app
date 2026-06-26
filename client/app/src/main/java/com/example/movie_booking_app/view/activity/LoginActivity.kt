package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.LoginRequest
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val tvRegisterLink = findViewById<TextView>(R.id.tvRegisterLink)
        tvRegisterLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = LoginRequest(username, password)
                    val response = RetrofitClient.authApi.login(request)
                    
                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()
                            val token = body?.accessToken
                            if (token != null) {
                                val dbUsername = body?.account?.username ?: username
                                val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("auth_token", token)
                                    putString("username", dbUsername)
                                    apply()
                                }
                            }
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                            Toast.makeText(this@LoginActivity, "Login failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
