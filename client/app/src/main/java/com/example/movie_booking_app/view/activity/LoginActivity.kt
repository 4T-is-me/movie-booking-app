package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.request.loginRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
class LoginActivity : AppCompatActivity() {

    private lateinit var txtError: TextView
    private lateinit var passwordLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtError = findViewById(R.id.txtError)
        passwordLayout = findViewById(R.id.tilPassword)
        
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

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            // Handle forgot password
        }

        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val edtUsername = findViewById<TextInputEditText>(R.id.edtUsername)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)

        // Initialize eye icon state
        edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
        passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off)

        passwordLayout.setEndIconOnClickListener {
            val selection = edtPassword.selectionEnd
            if (edtPassword.transformationMethod is PasswordTransformationMethod) {
                edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility)
            } else {
                edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off)
            }
            edtPassword.setSelection(selection)
        }

        // Hide error when user starts typing
        edtUsername.addTextChangedListener { hideError() }
        edtPassword.addTextChangedListener { hideError() }

        btnLogin.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (username.isEmpty()) {
                showError("Please enter username")
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                showError("Please enter password")
                return@setOnClickListener
            }

            Log.d("LOGIN_DEBUG", "Username: $username")

            btnLogin.isEnabled = false
            hideError()

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val request = loginRequest(username, password)
                    val response = RetrofitClient.authApi.login(request)

                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()
                            val token = body?.accessToken
                            if (token != null) {
                                val dbUsername = body?.account?.username ?: username
                                val roleNameStr = body?.account?.roleId?.roleName ?: body?.account?.role ?: "user"
                                val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                with(sharedPref.edit()) {
                                    putString("auth_token", token)
                                    putString("username", dbUsername)
                                    putString("role", roleNameStr)
                                    apply()
                                }
                            }
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            val roleName = body?.account?.roleId?.roleName ?: body?.account?.role
                            val enteredOrDbUser = (body?.account?.username ?: username)
                            if ((roleName != null && roleName.contains("admin", ignoreCase = true)) || enteredOrDbUser.contains("admin", ignoreCase = true) || username.contains("admin", ignoreCase = true)) {
                                val intent = Intent(this@LoginActivity, AdminActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                finish()
                            }
                        } else {
                            val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                            Toast.makeText(this@LoginActivity, "Login failed: $errorMsg", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        btnLogin.isEnabled = true
                        showError("Connection error: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        txtError.text = message
        txtError.visibility = View.VISIBLE
    }

    private fun hideError() {
        txtError.visibility = View.GONE
    }
}
