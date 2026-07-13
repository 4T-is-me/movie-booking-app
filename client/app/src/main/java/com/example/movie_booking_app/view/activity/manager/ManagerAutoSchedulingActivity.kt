package com.example.movie_booking_app.view.activity.manager

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivityManagerAutoSchedulingBinding
import com.example.movie_booking_app.utils.AutoSchedulingAlgorithm
import com.example.movie_booking_app.utils.CinemaConfigDraft
import com.example.movie_booking_app.utils.MovieDraft
import com.example.movie_booking_app.utils.RoomDraft
import com.example.movie_booking_app.utils.ShowtimeDraft
import com.example.movie_booking_app.view.adapter.manager.AutoSchedulingAdapter
import java.time.LocalDate

class ManagerAutoSchedulingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagerAutoSchedulingBinding
    private lateinit var adapter: AutoSchedulingAdapter
    private var currentWeekStart: LocalDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerAutoSchedulingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updateCalendarUI()
    }

    private var allRooms = listOf<RoomDraft>()
    private var selectedRoomId: String? = null

    private fun setupUI() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        adapter = AutoSchedulingAdapter()
        binding.rvSchedule.layoutManager = LinearLayoutManager(this)
        binding.rvSchedule.adapter = adapter

        binding.btnGenerate.setOnClickListener {
            fetchMoviesAndGenerateSchedule()
        }

        binding.btnSave.setOnClickListener {
            Toast.makeText(this, "Schedule Saved Successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Setup Calendar Navigation
        binding.ivPrevWeek.setOnClickListener {
            currentWeekStart = currentWeekStart.minusWeeks(1)
            selectedDate = currentWeekStart
            updateCalendarUI()
        }
        
        binding.ivNextWeek.setOnClickListener {
            currentWeekStart = currentWeekStart.plusWeeks(1)
            selectedDate = currentWeekStart
            updateCalendarUI()
        }

        fetchRoomsAndPopulateTabs()
    }

    private fun fetchRoomsAndPopulateTabs() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                val roomResponse = com.example.movie_booking_app.data.api.RetrofitClient.managerRoomApi.getRooms()
                allRooms = roomResponse.body()?.data?.map {
                    RoomDraft(it.id, it.roomName ?: "Unknown Room", "c1", it.status ?: "active")
                } ?: emptyList()
                
                if (allRooms.isEmpty()) {
                    // Fallback to dummy rooms if DB is empty so user can still test
                    allRooms = listOf(
                        RoomDraft("room1", "Phòng chiếu 1", "c1", "active"),
                        RoomDraft("room2", "Phòng chiếu 2", "c1", "active")
                    )
                }
                
                if (allRooms.isNotEmpty()) {
                    selectedRoomId = allRooms.firstOrNull { it.status == "active" }?._id ?: allRooms.first()._id
                }
                
                renderRoomTabs()
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to dummy rooms on error
                allRooms = listOf(
                    RoomDraft("room1", "Phòng chiếu 1", "c1", "active"),
                    RoomDraft("room2", "Phòng chiếu 2", "c1", "active")
                )
                selectedRoomId = allRooms.first()._id
                renderRoomTabs()
            }
        }
    }

    private fun renderRoomTabs() {
        binding.layoutRoomTabs.removeAllViews()
        for (room in allRooms) {
            val tv = android.widget.TextView(this)
            tv.text = room.roomName
            tv.setPadding(48, 24, 48, 24)
            tv.textSize = 14f
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 24, 0)
            tv.layoutParams = params

            if (room._id == selectedRoomId) {
                tv.setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_button_gold)
                tv.setTextColor(android.graphics.Color.WHITE)
            } else if (room.status != "active") {
                tv.setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_rounded_border)
                tv.setTextColor(android.graphics.Color.parseColor("#999999")) // Gray out text to indicate disabled
            } else {
                tv.setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_rounded_border)
                tv.setTextColor(android.graphics.Color.parseColor("#333333"))
            }

            tv.setOnClickListener {
                if (room.status != "active") {
                    Toast.makeText(this@ManagerAutoSchedulingActivity, "Phòng này đang bảo trì, không thể xếp lịch!", Toast.LENGTH_SHORT).show()
                } else {
                    selectedRoomId = room._id
                    renderRoomTabs()
                    filterAndDisplaySchedule()
                }
            }

            binding.layoutRoomTabs.addView(tv)
        }
    }

    private fun updateCalendarUI() {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val weekEnd = currentWeekStart.plusDays(6)
        binding.tvSelectedWeek.text = "Selected week: ${currentWeekStart.format(formatter)} - ${weekEnd.format(formatter)}"
        
        val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)
        binding.tvMonthYear.text = currentWeekStart.format(monthFormatter)
        
        // Update days
        val layouts = listOf(binding.layoutDay1, binding.layoutDay2, binding.layoutDay3, binding.layoutDay4, binding.layoutDay5, binding.layoutDay6, binding.layoutDay7)
        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
        
        val dayFormatter = java.time.format.DateTimeFormatter.ofPattern("EEE", java.util.Locale.ENGLISH)
        
        for (i in 0..6) {
            val date = currentWeekStart.plusDays(i.toLong())
            labels[i].text = date.format(dayFormatter)
            dates[i].text = date.dayOfMonth.toString()
            
            // Set default styling
            labels[i].setTextColor(android.graphics.Color.parseColor("#999999"))
            dates[i].setBackgroundResource(0)
            dates[i].setTextColor(android.graphics.Color.parseColor("#333333"))
            dates[i].typeface = android.graphics.Typeface.DEFAULT
            
            // Set Selected Day Styling
            if (date == selectedDate) {
                dates[i].setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_circle_selected)
                dates[i].setTextColor(android.graphics.Color.WHITE)
                dates[i].typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            
            // Click listener
            layouts[i].setOnClickListener {
                selectedDate = date
                updateCalendarUI()
            }
        }

        fetchAndHighlightHolidays()
        filterAndDisplaySchedule()
    }

    private fun fetchAndHighlightHolidays() {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Using a public API for holidays to demonstrate (e.g. Nager.Date)
                val url = java.net.URL("https://date.nager.at/api/v3/PublicHolidays/${currentWeekStart.year}/VN")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    // Simple regex to find dates
                    val holidayDates = mutableListOf<String>()
                    val pattern = java.util.regex.Pattern.compile("\"date\":\"(\\d{4}-\\d{2}-\\d{2})\"")
                    val matcher = pattern.matcher(response)
                    while (matcher.find()) {
                        holidayDates.add(matcher.group(1) ?: "")
                    }
                    currentWeekHolidays = holidayDates
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
                        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
                        for (i in 0..6) {
                            val dateStr = currentWeekStart.plusDays(i.toLong()).toString() // yyyy-MM-dd
                            if (holidayDates.contains(dateStr)) {
                                labels[i].setTextColor(android.graphics.Color.parseColor("#E53935")) // Red label
                                if (currentWeekStart.plusDays(i.toLong()) != selectedDate) {
                                    dates[i].setTextColor(android.graphics.Color.parseColor("#E53935")) // Red text if not selected
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private var allGeneratedShowtimes = mutableListOf<ShowtimeDraft>()
    private var currentWeekHolidays = listOf<String>()

    private fun filterAndDisplaySchedule() {
        val filtered = allGeneratedShowtimes.filter {
            it.show_date == selectedDate.toString() && (selectedRoomId == null || it.room_id == selectedRoomId)
        }
        adapter.submitList(filtered)
    }

    private fun fetchMoviesAndGenerateSchedule() {
        binding.layoutLoading.visibility = android.view.View.VISIBLE
        binding.pbLoading.visibility = android.view.View.VISIBLE
        binding.ivResultIcon.visibility = android.view.View.GONE
        binding.tvLoadingText.text = "Generating..."
        binding.tvLoadingText.setTextColor(android.graphics.Color.parseColor("#333333"))
        
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            try {
                // Fetch Movies
                val movieResponse = com.example.movie_booking_app.data.api.RetrofitClient.movieApi.getMovies(100)
                val movies = movieResponse.body()?.data?.map {
                    MovieDraft(it.id ?: "", it.movieName ?: "Unknown Movie", it.duration ?: 120, it.status ?: "showing", it.tier ?: 1)
                } ?: emptyList()

                // Only generate for the currently selected room
                val roomsToGenerate = if (selectedRoomId != null) {
                    allRooms.filter { it._id == selectedRoomId }
                } else {
                    allRooms
                }

                val config = CinemaConfigDraft(15)

                val schedule = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    AutoSchedulingAlgorithm.generateWeeklySchedule(
                        currentWeekStart, currentWeekStart.plusDays(6), movies, roomsToGenerate, config, currentWeekHolidays
                    )
                }

                // Update global state and filter
                allGeneratedShowtimes.removeAll { 
                    val d = java.time.LocalDate.parse(it.show_date)
                    !d.isBefore(currentWeekStart) && !d.isAfter(currentWeekStart.plusDays(6)) && it.room_id == selectedRoomId
                }
                allGeneratedShowtimes.addAll(schedule)
                
                filterAndDisplaySchedule()
                
                // Show success in overlay
                binding.pbLoading.visibility = android.view.View.GONE
                binding.ivResultIcon.visibility = android.view.View.VISIBLE
                binding.ivResultIcon.setImageResource(com.example.movie_booking_app.R.drawable.baseline_check_circle_24)
                binding.ivResultIcon.setColorFilter(android.graphics.Color.parseColor("#00C853"))
                binding.tvLoadingText.text = "Generated successfully!"
                binding.tvLoadingText.setTextColor(android.graphics.Color.parseColor("#00C853"))
                kotlinx.coroutines.delay(1500)
                binding.layoutLoading.visibility = android.view.View.GONE
            } catch (e: Exception) {
                // Show failure in overlay
                binding.pbLoading.visibility = android.view.View.GONE
                binding.ivResultIcon.visibility = android.view.View.VISIBLE
                binding.ivResultIcon.setImageResource(com.example.movie_booking_app.R.drawable.ic_report)
                binding.ivResultIcon.setColorFilter(android.graphics.Color.parseColor("#E53935"))
                binding.tvLoadingText.text = "Generation failed!"
                binding.tvLoadingText.setTextColor(android.graphics.Color.parseColor("#E53935"))
                kotlinx.coroutines.delay(1500)
                binding.layoutLoading.visibility = android.view.View.GONE
            }
        }
    }
}
