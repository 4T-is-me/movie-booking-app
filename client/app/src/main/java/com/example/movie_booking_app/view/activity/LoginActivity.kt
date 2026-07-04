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
import com.example.movie_booking_app.data.model.request.LoginRequest
import com.example.movie_booking_app.data.model.request.GoogleLoginRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
class LoginActivity : AppCompatActivity() {

    private lateinit var txtError: TextView
    private lateinit var passwordLayout: TextInputLayout
    
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                handleGoogleLogin(idToken)
            } ?: showError("Google token is null")
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            val errorMsg = when (statusCode) {
                10 -> "Developer Error (10): Mismatched SHA-1 or Client ID"
                12500 -> "Sign In Failed (12500): Check Consent Screen or SHA-1"
                12501 -> "Sign In Canceled (12501): User closed the dialog"
                16 -> "Sign In Canceled (16): No network or disabled"
                else -> "Google sign in failed: $statusCode"
            }
            showError(errorMsg)
        }
    }

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(com.example.movie_booking_app.BuildConfig.GOOGLE_CLIENT_ID.trim())
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val btnGoogleLogin = findViewById<Button>(R.id.btnGoogleLogin)
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
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
                    val request = LoginRequest(username, password)
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
                                TokenManager.saveTokens(
                                    this@LoginActivity,
                                    token,
                                    body?.refreshToken ?: ""
                                )
                            }
                            Toast.makeText(this@LoginActivity, "Login successful", Toast.LENGTH_SHORT).show()
                            val roleName = body?.account?.roleId?.roleName ?: body?.account?.role
                            if (roleName != null && roleName.contains("admin", ignoreCase = true)) {
                                val intent = Intent(this@LoginActivity, AdminActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            }
                        } else {
                            val errorStr = response.errorBody()?.string()
                            val message = try {
                                if (errorStr != null && errorStr.trim().startsWith("{")) {
                                    JSONObject(errorStr).optString("message", "Invalid credentials")
                                } else {
                                    "Invalid username or password"
                                }
                            } catch (e: Exception) {
                                "Invalid username or password"
                            }
                            showError(message)

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
                                this@LoginActivity,
                                token,
                                body?.refreshToken ?: ""
                            )
                        }
                        Toast.makeText(this@LoginActivity, "Google Login successful", Toast.LENGTH_SHORT).show()
                        val roleName = body?.account?.roleId?.roleName ?: body?.account?.role
                        if (roleName != null && roleName.contains("admin", ignoreCase = true)) {
                            val intent = Intent(this@LoginActivity, AdminActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        val errorStr = response.errorBody()?.string()
                        val message = try {
                            if (errorStr != null && errorStr.trim().startsWith("{")) {
                                JSONObject(errorStr).optString("message", "Google Login Failed")
                            } else {
                                "Google Login Failed"
                            }
                        } catch (e: Exception) {
                            "Google Login Failed"
                        }
                        showError(message)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Connection error: ${e.localizedMessage}")
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
