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
            binding.btnSave.isEnabled = false
            val draftsToSave = allGeneratedShowtimes.filter { it.is_auto_generated && it.room_id == selectedRoomId }
            if (draftsToSave.isEmpty()) {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Không có lịch chiếu mới nào cần lưu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Map ShowtimeDraft -> ShowtimeBulkItem
            val bulkItems = draftsToSave.map {
                com.example.movie_booking_app.data.model.request.ShowtimeBulkItem(
                    movieId = it.movie_id,
                    cinemaId = it.cinema_id,
                    roomId = it.room_id,
                    showDate = it.show_date,
                    startTime = it.start_time,
                    endTime = it.end_time,
                    status = "published",
                    ticketPrice = 85000
                )
            }
            val requestBody = com.example.movie_booking_app.data.model.request.ShowtimeBulkRequest(bulkItems)

            lifecycleScope.launch {
                try {
                    // Start loading animation or block UI if needed
                    binding.pbLoading.visibility = android.view.View.VISIBLE
                    binding.layoutLoading.visibility = android.view.View.VISIBLE
                    binding.tvLoadingText.text = "Đang lưu vào Database..."

                    val response = com.example.movie_booking_app.data.api.RetrofitClient.managerApi.createBulkShowtimes(requestBody)

                    if (response.isSuccessful) {
                        binding.pbLoading.visibility = android.view.View.GONE
                        binding.ivResultIcon.visibility = android.view.View.VISIBLE
                        binding.ivResultIcon.setImageResource(com.example.movie_booking_app.R.drawable.baseline_check_circle_24)
                        binding.tvLoadingText.text = "Lưu thành công!"
                        kotlinx.coroutines.delay(1500)
                        binding.layoutLoading.visibility = android.view.View.GONE
                        
                        Toast.makeText(this@ManagerAutoSchedulingActivity, "Lịch chiếu đã được lưu vào Database!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        binding.layoutLoading.visibility = android.view.View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this@ManagerAutoSchedulingActivity, "Lỗi server: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    binding.layoutLoading.visibility = android.view.View.GONE
                    binding.btnSave.isEnabled = true
                    e.printStackTrace()
                    Toast.makeText(this@ManagerAutoSchedulingActivity, "Lỗi kết nối mạng: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Setup Calendar Navigation
        binding.ivPrevWeek.setOnClickListener {
            currentWeekStart = currentWeekStart.minusWeeks(1)
            selectedDate = currentWeekStart
            updateCalendarUI()
            fetchExistingShowtimes(currentWeekStart, currentWeekStart.plusDays(6))
        }
        
        binding.ivNextWeek.setOnClickListener {
            currentWeekStart = currentWeekStart.plusWeeks(1)
            selectedDate = currentWeekStart
            updateCalendarUI()
            fetchExistingShowtimes(currentWeekStart, currentWeekStart.plusDays(6))
        }

        fetchRoomsAndPopulateTabs()
        fetchExistingShowtimes(currentWeekStart, currentWeekStart.plusDays(6))
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
                selectedRoomId = room._id
                renderRoomTabs()
                filterAndDisplaySchedule()
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
        
        // Disable generating and saving if the week is in the past
        val thisWeekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
        if (currentWeekStart.isBefore(thisWeekStart)) {
            binding.btnGenerate.visibility = android.view.View.GONE
            binding.btnSave.visibility = android.view.View.GONE
        } else {
            binding.btnGenerate.visibility = android.view.View.VISIBLE
            binding.btnSave.visibility = android.view.View.VISIBLE
        }

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

    private fun fetchExistingShowtimes(startDate: LocalDate, endDate: LocalDate) {
        lifecycleScope.launch {
            try {
                binding.pbLoading.visibility = android.view.View.VISIBLE
                val startStr = startDate.toString()
                val endStr = endDate.toString()
                
                val response = com.example.movie_booking_app.data.api.RetrofitClient.managerApi.getShowtimes(startStr, endStr)
                if (response.isSuccessful) {
                    val apiShowtimes = response.body()?.data ?: emptyList()
                    allGeneratedShowtimes.clear()
                    
                    for (apiItem in apiShowtimes) {
                        if (apiItem.movieId != null && apiItem.roomId != null) {
                            
                            val startLocal = try {
                                java.time.ZonedDateTime.parse(apiItem.startTime)
                                    .withZoneSameInstant(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            } catch (e: Exception) { apiItem.startTime }
                            
                            val endLocal = try {
                                java.time.ZonedDateTime.parse(apiItem.endTime)
                                    .withZoneSameInstant(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            } catch (e: Exception) { apiItem.endTime }

                            allGeneratedShowtimes.add(ShowtimeDraft(
                                show_date = apiItem.showDate.substring(0, 10),
                                room_id = apiItem.roomId._id,
                                room_name = apiItem.roomId.roomName,
                                movie_id = apiItem.movieId._id,
                                movie_name = apiItem.movieId.movieName,
                                cinema_id = "", // Not strictly needed for UI display
                                start_time = startLocal,
                                end_time = endLocal,
                                status = apiItem.status,
                                is_auto_generated = false
                            ))
                        }
                    }
                    filterAndDisplaySchedule()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ManagerAutoSchedulingActivity, "Lỗi tải lịch chiếu: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbLoading.visibility = android.view.View.GONE
            }
        }
    }

    private fun filterAndDisplaySchedule() {
        val filtered = allGeneratedShowtimes.filter {
            it.show_date == selectedDate.toString() && (selectedRoomId == null || it.room_id == selectedRoomId)
        }.sortedBy { it.start_time }
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
                val movieResponse = com.example.movie_booking_app.data.api.RetrofitClient.movieApi.getMovies()
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

                // Pass existing showtimes from OTHER rooms to avoid cross-room conflicts
                val existingOtherRooms = allGeneratedShowtimes.filter { it.room_id != selectedRoomId }

                val schedule = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                    AutoSchedulingAlgorithm.generateWeeklySchedule(
                        currentWeekStart, currentWeekStart.plusDays(6), movies, roomsToGenerate, config, currentWeekHolidays, existingOtherRooms
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
