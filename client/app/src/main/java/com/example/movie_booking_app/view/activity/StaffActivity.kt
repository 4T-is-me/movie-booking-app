package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.fragment.staff.StaffRequestsFragment
import com.example.movie_booking_app.view.fragment.staff.StaffSalesFragment
import com.example.movie_booking_app.view.fragment.staff.StaffScheduleFragment
import com.example.movie_booking_app.view.fragment.staff.StaffValidatorFragment
import com.example.movie_booking_app.view.fragment.staff.StaffNotificationsFragment
import com.example.movie_booking_app.view.fragment.staff.StaffProfileFragment
import com.example.movie_booking_app.view.fragment.staff.StaffSettingsFragment
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.viewmodel.AuthViewModel
import com.example.movie_booking_app.data.api.AuthEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvHeaderTitle: TextView
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)

        val ivMenu = findViewById<ImageView>(R.id.ivMenu)
        ivMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_staff_checkin -> {
                    tvHeaderTitle.text = "Ticket Validator"
                    loadFragment(StaffValidatorFragment())
                }
                R.id.nav_staff_sales -> {
                    tvHeaderTitle.text = "Ticket Sales"
                    loadFragment(StaffSalesFragment())
                }
                R.id.nav_staff_schedule -> {
                    tvHeaderTitle.text = "Work Schedule"
                    loadFragment(StaffScheduleFragment())
                }
                R.id.nav_staff_requests -> {
                    tvHeaderTitle.text = "Leave Requests"
                    loadFragment(StaffRequestsFragment())
                }
                R.id.nav_staff_notifications -> {
                    tvHeaderTitle.text = "Notifications"
                    loadFragment(StaffNotificationsFragment())
                }
                R.id.nav_staff_profile -> {
                    tvHeaderTitle.text = "My Profile"
                    loadFragment(StaffProfileFragment())
                }
                R.id.nav_staff_settings -> {
                    tvHeaderTitle.text = "Settings"
                    loadFragment(StaffSettingsFragment())
                }
                R.id.nav_staff_logout -> {
                    com.example.movie_booking_app.utils.AuthUtils.showLogoutConfirmationDialog(this)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set default selection to Work Schedule
        if (savedInstanceState == null) {
            navigationView.setCheckedItem(R.id.nav_staff_schedule)
            tvHeaderTitle.text = "Work Schedule"
            loadFragment(StaffScheduleFragment())
        }

        observeAuthEvents()
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.staff_container, fragment)
            .commit()
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            authViewModel.authEvents.collectLatest { event ->
                if (event is AuthEvent.SessionExpired) {
                    val intent = Intent(this@StaffActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}
