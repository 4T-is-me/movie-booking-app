package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.icu.number.NumberFormatter
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.databinding.ActivityManagerBinding
import com.example.movie_booking_app.view.activity.manager.ManagerRoomListActivity
import com.example.movie_booking_app.view.adapter.manager.ManagerShowtimeAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.System.console
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ManagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManagerBinding
    private val viewModel: ManagerViewModel by viewModels()

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
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
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

                    binding.tvRevenue.text = currencyFormatter.format(kpi.revenueToday)
                    binding.tvTicketsSold.text =kpi.ticketSoldToday.toString()
                    binding.tvOccupancyRate.text = kpi.occupancyRate
                    binding.tvFoodRevenue.text = currencyFormatter.format(kpi.foodRevenueToday)
                    binding.tvRunningMovies.text = kpi.runningMoviesToday.toString()
                    binding.tvActiveScreens.text = "${kpi.activeScreenToday}/${kpi.allScreenToday}"


                    val listShowtime = data.todayShowtimes.orEmpty()
                    //Log.d("CheckShowtime", "Danh sách lịch chiếu: $listShowtime")
                    //gửi data cho adapter
                    if (listShowtime.isEmpty()) {
                        binding.rvTodayShowtimes.visibility = android.view.View.GONE
                        binding.tvNoShowtimes.visibility = android.view.View.VISIBLE
                    } else {

                        val previewShowtimes = listShowtime.take(5)
                        showtimeAdapter.submitList(previewShowtimes)
                       // Log.d("CheckShowtime", "Danh sách lịch chiếu: $previewShowtimes")
                        binding.rvTodayShowtimes.visibility = android.view.View.VISIBLE
                        binding.tvNoShowtimes.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }

    private fun setupNavigationEvents() {
        binding.ivMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.tvViewAll.setOnClickListener {
//      // Lưu ý: Để truyền trực tiếp list này, ShowtimeTodayResponse của bạn cần implement java.io.Serializable
//      putExtra("EXTRA_FULL_SHOWTIMES", ArrayList(listShowtime))
            Toast.makeText(this, "No showtimes available to view", Toast.LENGTH_SHORT).show()
        }
        binding.layoutSchedule.setOnClickListener {
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.layoutRooms.setOnClickListener {
            startActivity(Intent(this, ManagerRoomListActivity::class.java))
        }
        binding.layoutStaff.setOnClickListener {
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }
        binding.layoutReports.setOnClickListener {
            Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        }


        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> performLogout()
                R.id.nav_rooms -> {
                    startActivity(Intent(this, ManagerRoomListActivity::class.java))
                }
                else -> Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setCurrentDate() {
        try {
            val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale.ENGLISH)
            binding.tvDate.text = sdf.format(Date())
        }catch (e: Exception) {
            binding.tvDate.text = "Today"
        }
    }
    private fun getGreetingMessage(): String {
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        return when (currentHour) {
            in 5..11 -> "Good morning ☀️"
            in 12..16 -> "Good afternoon 🌤️"
            in 17..21 -> "Good evening 🌙"
            else -> "Good night 💤"
        }
    }
}
