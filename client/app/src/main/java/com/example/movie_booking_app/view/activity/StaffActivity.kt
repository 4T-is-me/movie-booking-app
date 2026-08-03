package com.example.movie_booking_app.view.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.AuthEvent
import com.example.movie_booking_app.data.local.FcmTokenHelper
import com.example.movie_booking_app.databinding.ActivityStaffBinding
import com.example.movie_booking_app.databinding.LayoutMenuBadgeBinding
import com.example.movie_booking_app.utils.AuthUtils
import com.example.movie_booking_app.view.fragment.staff.StaffNotificationsFragment
import com.example.movie_booking_app.view.fragment.staff.StaffPayrollFragment
import com.example.movie_booking_app.view.fragment.staff.StaffProfileFragment
import com.example.movie_booking_app.view.fragment.staff.StaffRatingFragment
import com.example.movie_booking_app.view.fragment.staff.StaffRequestsFragment
import com.example.movie_booking_app.view.fragment.staff.StaffSalesFragment
import com.example.movie_booking_app.view.fragment.staff.StaffScheduleFragment
import com.example.movie_booking_app.view.fragment.staff.StaffValidatorFragment
import com.example.movie_booking_app.viewmodel.AuthViewModel
import com.example.movie_booking_app.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffActivity : BaseActivity() {

    private lateinit var binding: ActivityStaffBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    @Inject
    lateinit var fcmTokenHelper: FcmTokenHelper

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notificationViewModel.fetchStaffNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register FCM Registration Token
        lifecycleScope.launch {
            fcmTokenHelper.registerCurrentToken("StaffActivity")
        }

        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_staff_checkin -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_ticket_validator)
                    loadFragment(StaffValidatorFragment())
                }
                R.id.nav_staff_sales -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_ticket_sales)
                    loadFragment(StaffSalesFragment())
                }
                R.id.nav_staff_schedule -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_work_schedule)
                    loadFragment(StaffScheduleFragment())
                }
                R.id.nav_staff_requests -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_leave_requests)
                    loadFragment(StaffRequestsFragment())
                }
                R.id.nav_staff_ratings -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_my_ratings)
                    loadFragment(StaffRatingFragment())
                }
                R.id.nav_staff_notifications -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_notifications)
                    loadFragment(StaffNotificationsFragment())
                }
                R.id.nav_staff_profile -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_my_profile)
                    loadFragment(StaffProfileFragment())
                }
                R.id.nav_staff_settings -> {
                    binding.tvHeaderTitle.text = getString(R.string.auto_str_payroll)
                    loadFragment(StaffPayrollFragment())
                }
                R.id.nav_staff_logout -> {
                    AuthUtils.showLogoutConfirmationDialog(this)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Set default selection to Work Schedule
        if (savedInstanceState == null) {
            handleIncomingIntent(intent)
        }

        observeAuthEvents()
        observeNotificationUnreadCount()
        notificationViewModel.fetchStaffNotifications()
    }

    override fun onResume() {
        super.onResume()
        notificationViewModel.fetchStaffNotifications()
        ContextCompat.registerReceiver(
            this,
            notificationReceiver,
            IntentFilter("com.example.movie_booking_app.NEW_NOTIFICATION"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(notificationReceiver)
        } catch (e: Exception) {
            // Ignore
        }
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

        val isStaffNotif = type == "SHIFT_ASSIGNED" || type == "SHIFT_UPDATED" ||
                type == "LEAVE_APPROVED" || type == "LEAVE_REJECTED" || type == "LEAVE_CANCELLED" ||
                type?.startsWith("LEAVE_") == true || type?.startsWith("SHIFT_") == true

        if (isStaffNotif) {
            binding.navigationView.setCheckedItem(R.id.nav_staff_notifications)
            binding.tvHeaderTitle.text = getString(R.string.auto_str_notifications_1)
            loadFragment(StaffNotificationsFragment())
        } else if (!type.isNullOrEmpty() || !referenceId.isNullOrEmpty() || !title.isNullOrEmpty()) {
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
        binding.navigationView.setCheckedItem(R.id.nav_staff_schedule)
        binding.tvHeaderTitle.text = getString(R.string.auto_str_work_schedule_1)
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
        val menuItem = binding.navigationView.menu.findItem(R.id.nav_staff_notifications)
        val actionView = menuItem?.actionView ?: return
        val badgeBinding = LayoutMenuBadgeBinding.bind(actionView)

        if (count > 0) {
            badgeBinding.tvNotificationBadge.visibility = View.VISIBLE
            badgeBinding.tvNotificationBadge.text = if (count > 99) "99+" else count.toString()
        } else {
            badgeBinding.tvNotificationBadge.visibility = View.GONE
        }
    }
}
