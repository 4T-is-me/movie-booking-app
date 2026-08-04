package com.example.movie_booking_app.view.fragment.manager

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.api.RetrofitClient
import com.example.movie_booking_app.data.model.request.ShowtimeBulkItem
import com.example.movie_booking_app.data.model.request.ShowtimeBulkRequest
import com.example.movie_booking_app.databinding.FragmentManagerAutoSchedulingBinding
import com.example.movie_booking_app.utils.AutoSchedulingAlgorithm
import com.example.movie_booking_app.utils.CinemaConfigDraft
import com.example.movie_booking_app.utils.MovieDraft
import com.example.movie_booking_app.utils.RoomDraft
import com.example.movie_booking_app.utils.ShowtimeDraft
import com.example.movie_booking_app.view.adapter.manager.AutoSchedulingAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

@AndroidEntryPoint
class ManagerScheduleFragment : Fragment() {

    private var _binding: FragmentManagerAutoSchedulingBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AutoSchedulingAdapter
    private var currentWeekStart: LocalDate = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
    private var selectedDate: LocalDate = LocalDate.now()
    private var allRooms = listOf<RoomDraft>()
    private var selectedRoomId: String? = null
    private var allGeneratedShowtimes = mutableListOf<ShowtimeDraft>()
    private var currentWeekHolidays = listOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerAutoSchedulingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide toolbar since ManagerActivity has its own header
        binding.toolbar.visibility = View.GONE

        setupUI()
        updateCalendarUI()
    }

    private fun setupUI() {
        adapter = AutoSchedulingAdapter()
        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSchedule.adapter = adapter

        binding.btnGenerate.setOnClickListener {
            fetchMoviesAndGenerateSchedule()
        }

        binding.btnSave.setOnClickListener {
            binding.btnSave.isEnabled = false
            val draftsToSave = allGeneratedShowtimes.filter { it.is_auto_generated && it.room_id == selectedRoomId }
            if (draftsToSave.isEmpty()) {
                binding.btnSave.isEnabled = true
                Toast.makeText(requireContext(), getString(R.string.auto_str_no_new_showtimes_to_save), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                    binding.pbLoading.visibility = View.VISIBLE
                    binding.layoutLoading.visibility = View.VISIBLE
                    binding.ivResultIcon.visibility = View.GONE
                    binding.tvLoadingText.text = getString(R.string.auto_str_saving_to_database)

                    val response = RetrofitClient.managerApi.createBulkShowtimes(requestBody)

                    if (response.isSuccessful) {
                        binding.pbLoading.visibility = View.GONE
                        binding.ivResultIcon.visibility = View.VISIBLE
                        binding.ivResultIcon.setImageResource(R.drawable.baseline_check_circle_24)
                        binding.ivResultIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.auto_color_00C853))
                        binding.tvLoadingText.text = getString(R.string.auto_str_saved_successfully)
                        
                        // Clear the drafts that were just saved
                        allGeneratedShowtimes.removeAll { it.is_auto_generated && it.room_id == selectedRoomId }
                        
                        delay(1500)
                        binding.layoutLoading.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                        
                        Toast.makeText(requireContext(), getString(R.string.auto_str_showtimes_saved_to_database), Toast.LENGTH_SHORT).show()
                        fetchExistingShowtimes(currentWeekStart, currentWeekStart.plusDays(6))
                    } else {
                        binding.pbLoading.visibility = View.GONE
                        binding.ivResultIcon.visibility = View.VISIBLE
                        binding.ivResultIcon.setImageResource(R.drawable.ic_error)
                        binding.ivResultIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
                        binding.tvLoadingText.text = "Server error: ${response.message()}"
                        delay(2000)
                        binding.layoutLoading.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                    }
                } catch (e: Exception) {
                    binding.pbLoading.visibility = View.GONE
                    binding.ivResultIcon.visibility = View.VISIBLE
                    binding.ivResultIcon.setImageResource(R.drawable.ic_error)
                    binding.ivResultIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
                    binding.tvLoadingText.text = "Connection error: ${e.message}"
                    delay(2000)
                    binding.layoutLoading.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }
            }
        }

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
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val roomResponse = RetrofitClient.managerRoomApi.getRooms()
                val apiRooms = roomResponse.body()?.data
                
                allRooms = if (apiRooms.isNullOrEmpty()) {
                    listOf(
                        RoomDraft("room1", "Room 1", "c1", "active"),
                        RoomDraft("room2", "Room 2", "c1", "active")
                    )
                } else {
                    apiRooms.map {
                        RoomDraft(it.id, it.roomName ?: "Unknown Room", "c1", it.status ?: "active")
                    }
                }
                
                if (allRooms.isNotEmpty()) {
                    selectedRoomId = allRooms.firstOrNull { it.status.equals("active", ignoreCase = true) }?._id 
                        ?: allRooms.first()._id
                }
                
                renderRoomTabs()
            } catch (e: Exception) {
                allRooms = listOf(
                    RoomDraft("room1", "Room 1", "c1", "active"),
                    RoomDraft("room2", "Room 2", "c1", "active")
                )
                selectedRoomId = allRooms.first()._id
                renderRoomTabs()
            }
        }
    }

    private fun renderRoomTabs() {
        if (_binding == null) return
        binding.layoutRoomTabs.removeAllViews()
        for (room in allRooms) {
            val tv = android.widget.TextView(requireContext())
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

            val isActive = room.status.equals("active", ignoreCase = true)

            if (room._id == selectedRoomId) {
                tv.setBackgroundResource(R.drawable.bg_button_gold)
                tv.setTextColor(android.graphics.Color.WHITE)
            } else if (!isActive) {
                tv.setBackgroundResource(R.drawable.bg_rounded_border)
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_999999))
            } else {
                tv.setBackgroundResource(R.drawable.bg_rounded_border)
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_333333))
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
        if (_binding == null) return
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val weekEnd = currentWeekStart.plusDays(6)
        binding.tvSelectedWeek.text = "Selected week: ${currentWeekStart.format(formatter)} - ${weekEnd.format(formatter)}"
        
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        binding.tvMonthYear.text = currentWeekStart.format(monthFormatter)
        
        val thisWeekStart = LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
        if (currentWeekStart.isBefore(thisWeekStart)) {
            binding.btnGenerate.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
            binding.btnGenerate.visibility = View.GONE
            binding.btnSave.visibility = View.GONE
        } else {
            binding.btnGenerate.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
            binding.btnGenerate.visibility = View.VISIBLE
            binding.btnSave.visibility = View.VISIBLE
        }

        val layouts = listOf(binding.layoutDay1, binding.layoutDay2, binding.layoutDay3, binding.layoutDay4, binding.layoutDay5, binding.layoutDay6, binding.layoutDay7)
        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
        
        val dayFormatter = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
        
        for (i in 0..6) {
            val date = currentWeekStart.plusDays(i.toLong())
            labels[i].text = date.format(dayFormatter)
            dates[i].text = date.dayOfMonth.toString()
            
            labels[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_999999))
            dates[i].setBackgroundResource(0)
            dates[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_333333))
            dates[i].typeface = android.graphics.Typeface.DEFAULT
            
            if (date == selectedDate) {
                dates[i].setBackgroundResource(R.drawable.bg_circle_selected)
                dates[i].setTextColor(android.graphics.Color.WHITE)
                dates[i].typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            
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
                val url = URL("https://date.nager.at/api/v3/PublicHolidays/${currentWeekStart.year}/VN")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                
                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val holidayDates = mutableListOf<String>()
                    val pattern = Pattern.compile("\"date\":\"(\\d{4}-\\d{2}-\\d{2})\"")
                    val matcher = pattern.matcher(response)
                    while (matcher.find()) {
                        holidayDates.add(matcher.group(1) ?: "")
                    }
                    currentWeekHolidays = holidayDates
                    
                    withContext(Dispatchers.Main) {
                        if (_binding == null) return@withContext
                        val labels = listOf(binding.tvDay1Label, binding.tvDay2Label, binding.tvDay3Label, binding.tvDay4Label, binding.tvDay5Label, binding.tvDay6Label, binding.tvDay7Label)
                        val dates = listOf(binding.tvDay1Date, binding.tvDay2Date, binding.tvDay3Date, binding.tvDay4Date, binding.tvDay5Date, binding.tvDay6Date, binding.tvDay7Date)
                        for (i in 0..6) {
                            val dateStr = currentWeekStart.plusDays(i.toLong()).toString()
                            if (holidayDates.contains(dateStr)) {
                                labels[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
                                if (currentWeekStart.plusDays(i.toLong()) != selectedDate) {
                                    dates[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
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

    private fun fetchExistingShowtimes(startDate: LocalDate, endDate: LocalDate) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.pbLoading.visibility = View.VISIBLE
                val response = RetrofitClient.managerApi.getShowtimes(startDate.toString(), endDate.toString())
                if (response.isSuccessful) {
                    val apiShowtimes = response.body()?.data ?: emptyList()
                    
                    // Only clear and replace non-auto-generated items
                    allGeneratedShowtimes.removeAll { !it.is_auto_generated }
                    
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

                            val roomRef = apiItem.roomId
                            val movieRef = apiItem.movieId

                            allGeneratedShowtimes.add(ShowtimeDraft(
                                show_date = apiItem.showDate.substring(0, 10),
                                room_id = roomRef._id ?: "",
                                room_name = roomRef.roomName ?: "Unknown",
                                movie_id = movieRef._id ?: "",
                                movie_name = movieRef.movieName ?: "Unknown",
                                cinema_id = "",
                                start_time = startLocal,
                                end_time = endLocal,
                                status = apiItem.status,
                                is_auto_generated = false
                            ))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        filterAndDisplaySchedule()
                    }
                }
            } catch (e: Exception) {
                if (_binding != null) {
                    Toast.makeText(requireContext(), "Error loading showtimes: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (_binding != null) binding.pbLoading.visibility = View.GONE
            }
        }
    }

    private fun filterAndDisplaySchedule() {
        if (_binding == null) return
        val filtered = allGeneratedShowtimes.filter {
            it.show_date == selectedDate.toString() && (selectedRoomId == null || it.room_id == selectedRoomId)
        }.sortedBy { it.start_time }
        adapter.submitList(filtered)
    }

    private fun fetchMoviesAndGenerateSchedule() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.pbLoading.visibility = View.VISIBLE
        binding.ivResultIcon.visibility = View.GONE
        binding.tvLoadingText.text = getString(R.string.auto_str_generating)
        binding.tvLoadingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_333333))
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val movieResponse = RetrofitClient.movieApi.getMovies()
                val apiMovies = movieResponse.body()?.data
                
                // Log for debugging
                // println("API Movies: ${apiMovies?.size}")

                val movies = apiMovies?.map {
                    MovieDraft(it.id ?: "", it.movieName ?: "Unknown Movie", it.duration ?: 120, it.status ?: "showing", it.tier ?: 1)
                } ?: emptyList()

                val activeMovies = movies.filter { it.status.equals("showing", ignoreCase = true) }
                if (activeMovies.isEmpty()) {
                    throw Exception("No movies found with status 'showing'")
                }

                val roomsToGenerate = if (selectedRoomId != null) {
                    allRooms.filter { it._id == selectedRoomId }
                } else {
                    allRooms
                }

                val activeRooms = roomsToGenerate.filter { it.status.equals("active", ignoreCase = true) }
                if (activeRooms.isEmpty()) {
                    throw Exception("Selected room is not active")
                }

                val config = CinemaConfigDraft(15)
                // Filter out auto-generated showtimes from other rooms to avoid conflicts during calculation
                val existingOtherRooms = allGeneratedShowtimes.filter { it.room_id != selectedRoomId }

                val schedule = withContext(Dispatchers.Default) {
                    AutoSchedulingAlgorithm.generateWeeklySchedule(
                        currentWeekStart, currentWeekStart.plusDays(6), movies, activeRooms, config, currentWeekHolidays, existingOtherRooms
                    )
                }

                if (schedule.isEmpty()) {
                    throw Exception("Algorithm generated 0 showtimes. Check movies/rooms/conflicts.")
                }

                // Remove previous drafts for this room
                allGeneratedShowtimes.removeAll { 
                    it.is_auto_generated && it.room_id == selectedRoomId
                }
                
                // Add the new drafts
                allGeneratedShowtimes.addAll(schedule)
                
                withContext(Dispatchers.Main) {
                    filterAndDisplaySchedule()
                    
                    binding.pbLoading.visibility = View.GONE
                    binding.ivResultIcon.visibility = View.VISIBLE
                    binding.ivResultIcon.setImageResource(R.drawable.baseline_check_circle_24)
                    binding.ivResultIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.auto_color_00C853))
                    binding.tvLoadingText.text = getString(R.string.auto_str_generated_successfully)
                    binding.tvLoadingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_00C853))
                    
                    delay(1500)
                    binding.layoutLoading.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.pbLoading.visibility = View.GONE
                binding.ivResultIcon.visibility = View.VISIBLE
                binding.ivResultIcon.setImageResource(R.drawable.ic_error)
                binding.ivResultIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
                binding.tvLoadingText.text = e.message ?: getString(R.string.auto_str_generation_failed)
                binding.tvLoadingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.auto_color_E53935))
                delay(2500)
                binding.layoutLoading.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
