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
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.viewmodel.AuthViewModel
import com.example.movie_booking_app.data.api.AuthEvent
import com.example.movie_booking_app.viewmodel.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvHeaderTitle: TextView
    private val authViewModel: AuthViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

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
            handleIncomingIntent(intent)
        }

        observeAuthEvents()
        observeNotificationUnreadCount()
        notificationViewModel.loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        notificationViewModel.loadNotifications()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) {
            loadDefaultFragment()
            return
        }

        val type = intent.getStringExtra("type")
        val referenceId = intent.getStringExtra("referenceId")
        val title = intent.getStringExtra("title")
        val body = intent.getStringExtra("body")

        if (!type.isNullOrEmpty() || !referenceId.isNullOrEmpty() || !title.isNullOrEmpty()) {
            // First load default fragment so backstack is valid
            loadDefaultFragment()

            // Open NotificationActivity to save and show details
            val destIntent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("type", type)
                putExtra("referenceId", referenceId)
                putExtra("title", title)
                putExtra("body", body)
            }
            startActivity(destIntent)
        } else {
            loadDefaultFragment()
        }
    }

    private fun loadDefaultFragment() {
        navigationView.setCheckedItem(R.id.nav_staff_schedule)
        tvHeaderTitle.text = "Work Schedule"
        loadFragment(StaffScheduleFragment())
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

    // Observe the number of unread notifications and display the badge
    private fun observeNotificationUnreadCount() {
        lifecycleScope.launch {
            notificationViewModel.unreadCount.collectLatest { count ->
                updateNotificationBadge(count)
            }
        }
    }
    // Update the red UI badge on the navigation item
    private fun updateNotificationBadge(count: Int) {
        val menuItem = navigationView.menu.findItem(R.id.nav_staff_notifications)
        val actionView = menuItem?.actionView ?: return
        val tvBadge = actionView.findViewById<TextView>(R.id.tvNotificationBadge) ?: return

        if (count > 0) {
            tvBadge.visibility = View.VISIBLE
            tvBadge.text = if (count > 99) "99+" else count.toString()
        } else {
            tvBadge.visibility = View.GONE
        }
    }
}
