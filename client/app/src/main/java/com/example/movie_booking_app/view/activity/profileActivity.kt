package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityProfileBinding
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

/**
 * Activity representing the User Profile Screen.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by lazy {
        ViewModelProvider(this)[ProfileViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile
    }

    /**
     * Initializes the bottom navigation selection and listener.
     */
    private fun setupBottomNavigation() {
        // Pre-select the Profile tab in the bottom bar
        binding.bottomNavigationView.selectedItemId = R.id.nav_profile

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = android.content.Intent(this, MainActivity::class.java)
                    intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_ticket -> {
                    Toast.makeText(this, "Ticket Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_movie -> {
                    Toast.makeText(this, "Movie Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_profile -> {
                    // Already on profile
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Wires up click handlers for editing profile and menu options.
     */
    private fun setupClickListeners() {
        binding.ivEditProfile.setOnClickListener {
            Toast.makeText(this, "Edit Profile Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutMyTicket.setOnClickListener {
            Toast.makeText(this, "My Ticket Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutChangeLanguage.setOnClickListener {
            Toast.makeText(this, "Change Language Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutChangePassword.setOnClickListener {
            Toast.makeText(this, "Change Password Clicked", Toast.LENGTH_SHORT).show()
        }

        binding.layoutLogout.setOnClickListener {
            val sharedPref = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                remove("auth_token")
                remove("username")
                apply()
            }
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    /**
     * Observes live updates from the Profile ViewModel.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { userProfile ->
                    userProfile?.let {
                        binding.tvName.text = it.name
                        binding.tvPhone.text = it.phone
                        binding.tvEmail.text = it.email
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(this@ProfileActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
