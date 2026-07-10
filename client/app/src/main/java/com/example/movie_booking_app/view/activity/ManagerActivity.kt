package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.graphics.Color
import com.github.mikephil.charting.data.Entry
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.data.model.response.WeeklyRevenueResponse
import com.example.movie_booking_app.databinding.ActivityManagerBinding
import com.example.movie_booking_app.view.activity.manager.ManagerReportActivity
import com.example.movie_booking_app.view.activity.manager.ManagerRoomListActivity
import com.example.movie_booking_app.view.adapter.manager.ManagerShowtimeAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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
                    //Log.d("CheckShowtime", "Showtimes List: $listShowtime")
                    //send data to the adapter
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

                    val weeklyData = data.weeklyRevenue.orEmpty()
                    //Log.d("CheckShowtime", "Data: $weeklyData")
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
        //nạp data
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

            // Cấu hình trục X (Trục ngang dưới cùng)
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
        binding.tvViewAll.setOnClickListener {
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
            startActivity(Intent(this, ManagerReportActivity::class.java))
        }


        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_logout -> performLogout()
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, ManagerActivity ::class.java))
                }
                R.id.nav_rooms -> {
                    startActivity(Intent(this, ManagerRoomListActivity::class.java))
                }
                R.id.nav_reports -> {
                    startActivity(Intent(this, ManagerReportActivity::class.java))
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
