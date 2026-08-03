package com.example.movie_booking_app.view.activity.manager

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.ShowtimeBulkItem
import com.example.movie_booking_app.data.model.request.ShowtimeBulkRequest
import com.example.movie_booking_app.databinding.ActivityManagerAutoSchedulingBinding
import com.example.movie_booking_app.utils.AutoSchedulingAlgorithm
import com.example.movie_booking_app.utils.CinemaConfigDraft
import com.example.movie_booking_app.utils.MovieDraft
import com.example.movie_booking_app.utils.RoomDraft
import com.example.movie_booking_app.utils.ShowtimeDraft
import com.example.movie_booking_app.view.adapter.manager.AutoSchedulingAdapter
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                Toast.makeText(this, getString(R.string.auto_str_no_new_showtimes_to_save), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Map ShowtimeDraft -> ShowtimeBulkItem
            val bulkItems = draftsToSave.map {
                ShowtimeBulkItem(
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
            val requestBody = ShowtimeBulkRequest(bulkItems)

            lifecycleScope.launch {
                try {
                    // Start loading animation or block UI if needed
                    binding.pbLoading.visibility = View.VISIBLE
                    binding.layoutLoading.visibility = View.VISIBLE
                    binding.tvLoadingText.text = getString(R.string.auto_str_saving_to_database)

                    val response = RetrofitClient.managerApi.createBulkShowtimes(requestBody)

                    if (response.isSuccessful) {
                        binding.pbLoading.visibility = View.GONE
                        binding.ivResultIcon.visibility = View.VISIBLE
                        binding.ivResultIcon.setImageResource(R.drawable.baseline_check_circle_24)
                        binding.tvLoadingText.text = getString(R.string.auto_str_saved_successfully)
                        delay(1500)
                        binding.layoutLoading.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        Toast.makeText(this@ManagerAutoSchedulingActivity, getString(R.string.auto_str_showtimes_saved_to_database), Toast.LENGTH_SHORT).show()
                        fetchExistingShowtimes(currentWeekStart, currentWeekStart.plusDays(6))
                    } else {
                        binding.layoutLoading.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this@ManagerAutoSchedulingActivity, "Server error: ${response.message()}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    binding.layoutLoading.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                    e.printStackTrace()
                    Toast.makeText(this@ManagerAutoSchedulingActivity, "Network connection error: ${e.message}", Toast.LENGTH_LONG).show()
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
        lifecycleScope.launch {
            try {
                val roomResponse = RetrofitClient.managerRoomApi.getRooms()
                allRooms = roomResponse.body()?.data?.map {
                    RoomDraft(it.id, it.roomName ?: "Unknown Room", "c1", it.status ?: "active")
                } ?: emptyList()
                
                if (allRooms.isNotEmpty()) {
                    selectedRoomId = allRooms.firstOrNull { it.status == "active" }?._id ?: allRooms.first()._id
                }
                
                renderRoomTabs()
            } catch (e: Exception) {
                e.printStackTrace()
                renderRoomTabs()
            }
        }
    }

    private fun renderRoomTabs() {
        binding.layoutRoomTabs.removeAllViews()
        for (room in allRooms) {
            val tv = TextView(this)
            tv.text = room.roomName
            tv.setPadding(48, 24, 48, 24)
            tv.textSize = 14f
            tv.setTypeface(null, Typeface.BOLD)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 24, 0)
            tv.layoutParams = params

            if (room._id == selectedRoomId) {
                tv.setBackgroundResource(R.drawable.bg_button_gold)
                tv.setTextColor(Color.WHITE)
            } else if (room.status != "active") {
                tv.setBackgroundResource(R.drawable.bg_rounded_border)
                tv.setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_999999)) // Gray out text to indicate disabled
            } else {
                tv.setBackgroundResource(R.drawable.bg_rounded_border)
                tv.setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_333333))
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
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val weekEnd = currentWeekStart.plusDays(6)
        binding.tvSelectedWeek.text = "Selected week: ${currentWeekStart.format(formatter)} - ${weekEnd.format(formatter)}"
        
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        binding.tvMonthYear.text = currentWeekStart.format(monthFormatter)
        
        // Disable generating and saving if the week is in the past
        val thisWeekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
        if (currentWeekStart.isBefore(thisWeekStart)) {
            binding.btnGenerate.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
        } else {
            binding.btnGenerate.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
        }

        // Update days
        val layouts = listOf(binding.layoutDay1, binding.layoutDay2, binding.layoutDay3, binding.layoutDay4, binding.layoutDay5, binding.layoutDay6, binding.layoutDay7)
        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
        
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        
        for (i in 0..6) {
            val date = currentWeekStart.plusDays(i.toLong())
            labels[i].text = date.format(dayFormatter)
            dates[i].text = date.dayOfMonth.toString()
            
            // Set default styling
            labels[i].setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_999999))
            dates[i].setBackgroundResource(0)
            dates[i].setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_333333))
            dates[i].typeface = Typeface.DEFAULT
            
            // Set Selected Day Styling
            if (date == selectedDate) {
                dates[i].setBackgroundResource(R.drawable.bg_circle_selected)
                dates[i].setTextColor(Color.WHITE)
                dates[i].typeface = Typeface.DEFAULT_BOLD
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Using a public API for holidays to demonstrate (e.g. Nager.Date)
                val url = URL("https://date.nager.at/api/v3/PublicHolidays/${currentWeekStart.year}/VN")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    // Simple regex to find dates
                    val holidayDates = mutableListOf<String>()
                    val pattern = Pattern.compile("\"date\":\"(\\d{4}-\\d{2}-\\d{2})\"")
                    val matcher = pattern.matcher(response)
                    while (matcher.find()) {
                        holidayDates.add(matcher.group(1) ?: "")
                    }
                    currentWeekHolidays = holidayDates
                    
                    withContext(Dispatchers.Main) {
                        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
                        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
                        for (i in 0..6) {
                            val dateStr = currentWeekStart.plusDays(i.toLong()).toString() // yyyy-MM-dd
                            if (holidayDates.contains(dateStr)) {
                                labels[i].setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_E53935)) // Red label
                                if (currentWeekStart.plusDays(i.toLong()) != selectedDate) {
                                    dates[i].setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_E53935)) // Red text if not selected
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
                binding.pbLoading.visibility = View.VISIBLE
                val startStr = startDate.toString()
                val endStr = endDate.toString()
                
                val response = RetrofitClient.managerApi.getShowtimes(startStr, endStr)
                if (response.isSuccessful) {
                    val apiShowtimes = response.body()?.data ?: emptyList()
                    allGeneratedShowtimes.clear()
                    
                    for (apiItem in apiShowtimes) {
                        if (apiItem.movieId != null && apiItem.roomId != null) {
                            
                            val startLocal = try {
                                ZonedDateTime.parse(apiItem.startTime)
                                    .withZoneSameInstant(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            } catch (e: Exception) { apiItem.startTime }
                            
                            val endLocal = try {
                                ZonedDateTime.parse(apiItem.endTime)
                                    .withZoneSameInstant(ZoneId.systemDefault())
                                    .toLocalDateTime()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
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
                Toast.makeText(this@ManagerAutoSchedulingActivity, "Error loading showtimes: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.pbLoading.visibility = View.GONE
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
        binding.layoutLoading.visibility = View.VISIBLE
        binding.pbLoading.visibility = View.VISIBLE
        binding.layoutLoading.visibility = View.VISIBLE
        binding.tvLoadingText.text = getString(R.string.auto_str_generating)
        binding.tvLoadingText.setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_333333))
        
        lifecycleScope.launch {
            try {
                // Fetch Movies
                val movieResponse = RetrofitClient.movieApi.getMovies()
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

                val schedule = withContext(Dispatchers.Default) {
                    AutoSchedulingAlgorithm.generateWeeklySchedule(
                        currentWeekStart, currentWeekStart.plusDays(6), movies, roomsToGenerate, config, currentWeekHolidays, existingOtherRooms
                    )
                }

                // Update global state and filter
                allGeneratedShowtimes.removeAll { 
                    val d = LocalDate.parse(it.show_date)
                    !d.isBefore(currentWeekStart) && !d.isAfter(currentWeekStart.plusDays(6)) && 
                    it.room_id == selectedRoomId && !d.isBefore(LocalDate.now())
                }
                allGeneratedShowtimes.addAll(schedule)
                
                withContext(Dispatchers.Main) {
                    filterAndDisplaySchedule()
                }
                
                // Show success in overlay
                binding.pbLoading.visibility = View.GONE
                binding.ivResultIcon.visibility = View.VISIBLE
                binding.ivResultIcon.setImageResource(R.drawable.baseline_check_circle_24)
                binding.ivResultIcon.setColorFilter(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_00C853))
                binding.tvLoadingText.text = getString(R.string.auto_str_generated_successfully)
                binding.tvLoadingText.setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_00C853))
                delay(1500)
                binding.layoutLoading.visibility = View.GONE
            } catch (e: Exception) {
                // Show failure in overlay
                binding.pbLoading.visibility = View.GONE
                binding.ivResultIcon.visibility = View.VISIBLE
                binding.ivResultIcon.setImageResource(R.drawable.ic_error)
                binding.ivResultIcon.setColorFilter(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_E53935))
                binding.tvLoadingText.text = getString(R.string.auto_str_generation_failed)
                binding.tvLoadingText.setTextColor(ContextCompat.getColor(this@ManagerAutoSchedulingActivity, R.color.auto_color_E53935))
                delay(1500)
                binding.layoutLoading.visibility = View.GONE
            }
        }
    }
}
