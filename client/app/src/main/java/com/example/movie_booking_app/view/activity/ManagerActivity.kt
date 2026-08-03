package com.example.movie_booking_app.view.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.response.WeeklyRevenueResponse
import com.example.movie_booking_app.databinding.ActivityManagerBinding
import com.example.movie_booking_app.databinding.LayoutMenuBadgeBinding
import com.example.movie_booking_app.viewmodel.NotificationViewModel
import com.example.movie_booking_app.view.adapter.manager.ManagerShowtimeAdapter
import com.example.movie_booking_app.view.fragment.manager.ManagerImportOrderFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerNotificationsFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerPayrollFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerProfileFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerReportFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerRoomFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerScheduleFragment
import com.example.movie_booking_app.view.fragment.manager.ManagerStaffFragment
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerBinding
    private val viewModel: ManagerViewModel by viewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            notificationViewModel.loadNotifications()
        }
    }

    private lateinit var showtimeAdapter : ManagerShowtimeAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpRecyclerView()
        setCurrentDate()
        viewModel.loadDashboard()
        observeDashboardData()
        setupNavigationEvents()
        binding.tvGreeting.text = getGreetingMessage()

        // Handle incoming notifications
        handleIncomingIntent(intent)
        observeNotificationUnreadCount()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val type = intent?.getStringExtra("type") ?: intent?.getStringExtra("notificationType")
        val referenceId = intent?.getStringExtra("referenceId") ?: intent?.getStringExtra("refId")
        val title = intent?.getStringExtra("title") ?: intent?.getStringExtra("gcm.notification.title")
        val body = intent?.getStringExtra("body") ?: intent?.getStringExtra("message")

        if (!type.isNullOrEmpty() || !referenceId.isNullOrEmpty() || !title.isNullOrEmpty()) {
            val destIntent = Intent(this, NotificationActivity::class.java).apply {
                putExtra("type", type)
                putExtra("referenceId", referenceId)
                putExtra("title", title)
                putExtra("body", body)
            }
            startActivity(destIntent)
        }
    }

    private fun setUpRecyclerView() {
        showtimeAdapter = ManagerShowtimeAdapter()
        binding.rvTodayShowtimes.apply {
            layoutManager = LinearLayoutManager(this@ManagerActivity)
            adapter = showtimeAdapter
            isNestedScrollingEnabled = false
        }

    }

    private fun performLogout() {
        TokenManager.clearTokens(this)
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("auth_token")
            remove("username")
            remove("role")
            apply()
        }
        Toast.makeText(this, getString(R.string.auto_str_logged_out_successfully), Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun observeDashboardData() {
        lifecycleScope.launch {
            viewModel.dashboardData.collect { response ->
                response?.data?.let {data ->
                    val kpi = data.kpiSummary

                    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                    val tvManager = data.managerId.slice(0..2).uppercase()
                    binding.tvAllRoom.text = "MGR-${tvManager}  ${kpi.allScreenToday} Rooms"
                    binding.tvManagerName.text = data.managerName
                    if (!data.cinemaName.isNullOrEmpty()) {
                        binding.tvCinemaName.text = data.cinemaName
                    }

                    binding.tvRevenue.text = currencyFormatter.format(kpi.revenueToday)
                    binding.tvTicketsSold.text =kpi.ticketSoldToday.toString()
                    binding.tvOccupancyRate.text = kpi.occupancyRate
                    binding.tvFoodRevenue.text = currencyFormatter.format(kpi.foodRevenueToday)
                    binding.tvRunningMovies.text = kpi.runningMoviesToday.toString()
                    binding.tvActiveScreens.text = "${kpi.activeScreenToday}/${kpi.allScreenToday}"


                    val listShowtime = data.todayShowtimes.orEmpty()

                    val upcomingShowtimes = listShowtime.filter { showtime ->
                        !showtime.status.equals("DONE", ignoreCase = true)
                    }

                    if (upcomingShowtimes.isEmpty()) {
                        binding.rvTodayShowtimes.visibility = View.GONE
                        binding.tvNoShowtimes.visibility = View.VISIBLE
                    } else {
                       // get 5 showtime
                        val previewShowtimes = upcomingShowtimes.take(5)
                        showtimeAdapter.submitList(previewShowtimes)

                        binding.rvTodayShowtimes.visibility = View.VISIBLE
                        binding.tvNoShowtimes.visibility = View.GONE
                    }

                    val weeklyData = data.weeklyRevenue.orEmpty()
                    //Timber.tag("CheckShowtime").d("Data: $weeklyData")
                    if(weeklyData.isNotEmpty()) {
                        setUpWeeklyChart(weeklyData)
                    }
                }
            }
        }
    }

    private fun setUpWeeklyChart(weeklyList: List<WeeklyRevenueResponse>) {
        val entries = ArrayList<Entry>()
        val lables = ArrayList<String>()

        val yellowColor = ContextCompat.getColor(this, R.color.color_gold)
        val darkGridColor = ContextCompat.getColor(this, R.color.dark_grid_color)

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("E", Locale("en", "US"))
        //load data
        weeklyList.forEachIndexed { index, item ->
            entries.add(Entry(index.toFloat(), item.totalAmount.toFloat()))
            val dayOfWeekName = try {
                val date = inputFormat.parse(item.date)
                date?.let { outputFormat.format(it) } ?: item.date
            } catch (e: Exception) {
                item.date // Returns the original date
            }
            lables.add(dayOfWeekName)
        }

        //lines data
        val lineDataSet = LineDataSet(entries, "Revenue (VND)").apply {
            color = yellowColor
            setCircleColor(yellowColor)
            lineWidth = 3f                      // Line thickness
            circleRadius = 4f                   // Node size
            setDrawCircleHole(false)
            valueTextColor = Color.WHITE
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER    // Smooth and curve the line
            setDrawFilled(true)                    // Apply a subtle gradient fill below the curved line
            fillColor = yellowColor
            fillAlpha = 25                          // Opacity of the bottom shadow area

        }

        val lineData = LineData(lineDataSet)
        binding.lineChartWeekly.apply {
            data = lineData
            description.isEnabled = false
            legend.textColor = Color.WHITE

            // Configure X-axis (bottom horizontal axis)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                valueFormatter = IndexAxisValueFormatter(lables)
                granularity = 1f
                setDrawGridLines(false)               // Hide vertical grid lines for a cleaner app look
            }
            // Configure left Y-axis (Display amount)
            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = darkGridColor // Dark grid color blends with the cinema's black background
            }
            // Hide the right Y-axis
            axisRight.isEnabled = false
            // Animation effect that runs when the chart loads
            animateX(1000)
            invalidate() // Refresh chart
        }

    }

    private fun setupNavigationEvents() {
        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.ivNotification.setOnClickListener {
            replaceFragment(ManagerNotificationsFragment(), getString(R.string.auto_str_notifications))
        }
        binding.tvViewAll.setOnClickListener {
            replaceFragment(ManagerScheduleFragment(), getString(R.string.auto_str_weekly_timetable))
        }
        binding.layoutSchedule.setOnClickListener {
            replaceFragment(ManagerScheduleFragment(), getString(R.string.auto_str_weekly_timetable))
        }
        binding.layoutRooms.setOnClickListener {
            replaceFragment(ManagerRoomFragment(), getString(R.string.auto_str_rooms))
        }
        binding.layoutStaff.setOnClickListener {
            replaceFragment(ManagerStaffFragment(), getString(R.string.auto_str_staff))
        }
        binding.layoutReports.setOnClickListener {
            replaceFragment(ManagerReportFragment(), getString(R.string.auto_str_reports))
        }


        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> performLogout()
                R.id.nav_dashboard -> {
                    showDashboard()
                }
                R.id.nav_notifications -> {
                    replaceFragment(ManagerNotificationsFragment(), getString(R.string.auto_str_notifications))
                }
                R.id.nav_rooms -> {
                    replaceFragment(ManagerRoomFragment(), getString(R.string.auto_str_rooms))
                }
                R.id.nav_reports -> {
                    replaceFragment(ManagerReportFragment(), getString(R.string.auto_str_reports))
                }
                R.id.nav_staff -> {
                    replaceFragment(ManagerStaffFragment(), getString(R.string.auto_str_staff))
                }
                R.id.nav_schedule -> {
                    replaceFragment(ManagerScheduleFragment(), getString(R.string.auto_str_weekly_timetable))
                }
                R.id.nav_import_orders -> {
                    replaceFragment(ManagerImportOrderFragment(), "Goods Intake Orders")
                }
                R.id.nav_profile -> {
                    replaceFragment(ManagerProfileFragment(), getString(R.string.auto_str_my_profile))
                }
                R.id.nav_payroll -> {
                    replaceFragment(ManagerPayrollFragment(), getString(R.string.auto_str_payroll))
                }
                else -> Toast.makeText(this, getString(R.string.auto_str_feature_coming_soon), Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    fun replaceFragment(fragment: Fragment, title: String) {
        binding.tvHeaderTitle.text = title
        binding.scrollDashboard.visibility = View.GONE
        binding.managerContainer.visibility = View.VISIBLE
        supportFragmentManager.beginTransaction()
            .replace(R.id.manager_container, fragment)
            .commit()
    }

    private fun showDashboard() {
        binding.tvHeaderTitle.text = getString(R.string.auto_str_dashboard)
        binding.managerContainer.visibility = View.GONE
        binding.scrollDashboard.visibility = View.VISIBLE
    }


    private fun setCurrentDate() {
        try {
            val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.ENGLISH)
            binding.tvDate.text = sdf.format(Date())
        }catch (e: Exception) {
            binding.tvDate.text = getString(R.string.auto_str_today)
        }
    }
    private fun getGreetingMessage(): String {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when (currentHour) {
            in 5..11 -> "Good morning ☀️"
            in 12..16 -> "Good afternoon 🌤️"
            in 17..21 -> "Good evening 🌙"
            else -> "Good night 💤"
        }
    }

    override fun onResume() {
        super.onResume()
        notificationViewModel.loadNotifications()
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

    private fun observeNotificationUnreadCount() {
        lifecycleScope.launch {
            notificationViewModel.unreadCount.collectLatest { count ->
                updateNotificationBadge(count)
            }
        }
    }

    private fun updateNotificationBadge(count: Int) {
        val menuItem = binding.navigationView.menu.findItem(R.id.nav_notifications)
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
