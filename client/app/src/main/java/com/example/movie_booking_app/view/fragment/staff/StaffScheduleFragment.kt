package com.example.movie_booking_app.view.fragment.staff

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.FragmentStaffScheduleBinding
import com.example.movie_booking_app.databinding.ItemStaffScheduleShiftBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.viewmodel.staff.StaffScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StaffScheduleFragment : Fragment() {

    private var _binding: FragmentStaffScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffScheduleViewModel by viewModels()

    private lateinit var dateAdapter: DateAdapter
    private lateinit var staffShiftAdapter: StaffShiftAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        observeViewModel()
        loadData()
    }

    private fun setupRecyclerViews() {
        // Date Strip (Calendar) Adapter
        dateAdapter = DateAdapter { selectedDate ->
            val updatedDates = dateAdapter.currentList.map {
                it.copy(isSelected = it.id == selectedDate.id)
            }
            dateAdapter.submitList(updatedDates)

            // Update Today's Shift Card based on selected day from DB shifts
            updateTodayShiftCard(selectedDate.id)
        }
        binding.rvWeeklyCalendar.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }

        // Shifts List Adapter
        staffShiftAdapter = StaffShiftAdapter(
            onRequestTimeOff = { item ->
                item.shiftId?.let { id ->
                    viewModel.requestTimeOff(id, "Personal Reason")
                }
            },
            onRequestReplacement = { item ->
                // Accept replacement logic
                Toast.makeText(requireContext(), "Accept replacement requested!", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvWeeklyShifts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = staffShiftAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.shifts.collectLatest { shiftList ->
                staffShiftAdapter.submitList(shiftList)
                
                // Set default display for today's selected date in calendar strip
                val activeDate = dateAdapter.currentList.find { it.isSelected }?.id
                if (activeDate != null) {
                    updateTodayShiftCard(activeDate)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.leaveRequestStatus.collectLatest { success ->
                if (success == true) {
                    Toast.makeText(requireContext(), "Leave request submitted successfully!", Toast.LENGTH_LONG).show()
                    viewModel.resetRequestStatus()
                } else if (success == false) {
                    Toast.makeText(requireContext(), "Failed to submit leave request!", Toast.LENGTH_LONG).show()
                    viewModel.resetRequestStatus()
                }
            }
        }
    }

    private fun loadData() {
        val calendarDates = generateCurrentWeekDates()
        dateAdapter.submitList(calendarDates)

        // Fetch shifts from backend for the 7 days visible in the calendar strip
        if (calendarDates.isNotEmpty()) {
            val startDate = calendarDates.first().id
            val endDate = calendarDates.last().id
            viewModel.loadShifts(startDate, endDate)
        }
    }

    private fun updateTodayShiftCard(selectedDateId: String) {
        val shifts = viewModel.shifts.value
        val matchingShift = shifts.find {
            val cleanDate = if (it.workDate?.contains("T") == true) it.workDate.substringBefore("T") else it.workDate
            cleanDate == selectedDateId
        }

        if (matchingShift != null && !matchingShift.shift.isNullOrEmpty()) {
            binding.cardTodayShift.visibility = View.VISIBLE
            binding.tvTodayHeader.visibility = View.VISIBLE
            binding.tvShiftTitle.text = "${matchingShift.shift} Shift"
            
            // Map Shift to hours
            val hours = when (matchingShift.shift) {
                "Morning" -> "08:00 AM - 12:00 PM"
                "Afternoon" -> "12:00 PM - 05:00 PM"
                "Evening" -> "05:00 PM - 10:00 PM"
                "Night" -> "10:00 PM - 02:00 AM"
                else -> "Flexible hours"
            }
            binding.tvShiftHours.text = hours
            binding.tvShiftLocation.text = "Lumina Screen Room • Staff Member"
            binding.tvStatusBadge.text = matchingShift.status ?: "Assigned"
            binding.tvStatusBadge.setBackgroundResource(com.example.movie_booking_app.R.drawable.bg_glass_badge)
        } else {
            binding.cardTodayShift.visibility = View.GONE
            binding.tvTodayHeader.visibility = View.GONE
        }
    }

    private fun generateCurrentWeekDates(): List<ShowDate> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val dayFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val monthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
        val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)
        
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<ShowDate>()
        for (i in 0 until 7) {
            val date = calendar.time
            val id = dateFormat.format(date)
            val day = dayFormat.format(date)
            val dayOfWeek = dayOfWeekFormat.format(date)
            val month = monthFormat.format(date)
            val year = yearFormat.format(date)
            
            dates.add(ShowDate(id, day, dayOfWeek, month, year, i == 0))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return dates
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Dynamic clean adapter for staff shift list
    private inner class StaffShiftAdapter(
        private var list: List<ShiftItemResponse> = emptyList(),
        private val onRequestTimeOff: (ShiftItemResponse) -> Unit,
        private val onRequestReplacement: (ShiftItemResponse) -> Unit
    ) : RecyclerView.Adapter<StaffShiftAdapter.ViewHolder>() {

        fun submitList(newList: List<ShiftItemResponse>) {
            list = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(val binding: ItemStaffScheduleShiftBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ShiftItemResponse) {
                val workDate = item.workDate ?: ""
                val (dayNum, monthName, _) = parseDateString(workDate)
                val dayOfWeek = getDayOfWeek(workDate)
                
                binding.tvBadgeDayName.text = dayOfWeek
                binding.tvBadgeDayNumber.text = dayNum
                binding.tvBadgeMonthName.text = monthName

                if (item.shift.isNullOrEmpty()) {
                    binding.tvShiftName.text = "Day Off"
                    binding.tvShiftName.setTextColor(Color.parseColor("#94A3B8"))
                    binding.layoutShiftTime.visibility = View.GONE
                    binding.btnShiftAction.visibility = View.GONE

                    setDynamicBadge(binding.tvShiftStatus, "Off", Color.parseColor("#64748B"), Color.parseColor("#1A64748B"))
                } else {
                    binding.layoutShiftTime.visibility = View.VISIBLE
                    binding.tvShiftName.setTextColor(Color.WHITE)

                    val shiftName = "${item.shift} Shift"
                    val shiftTime = when (item.shift) {
                        "Morning" -> "08:00 AM - 12:00 PM"
                        "Afternoon" -> "12:00 PM - 05:00 PM"
                        "Evening" -> "05:00 PM - 10:00 PM"
                        "Night" -> "10:00 PM - 02:00 AM"
                        else -> "Flexible hours"
                    }

                    binding.tvShiftName.text = shiftName
                    binding.tvShiftTimeText.text = shiftTime

                    // Action buttons displaying dynamic labels based on conditions
                    when (item.status) {
                        "NeedReplacement" -> {
                            setDynamicBadge(binding.tvShiftStatus, "Need Replacement", Color.parseColor("#F43F5E"), Color.parseColor("#1AF43F5E"))
                            binding.btnShiftAction.visibility = View.VISIBLE
                            binding.btnShiftAction.text = "Accept replacement"
                            binding.btnShiftAction.setTextColor(Color.parseColor("#F5C518"))
                            binding.btnShiftAction.setOnClickListener { onRequestReplacement(item) }
                        }
                        "Completed" -> {
                            setDynamicBadge(binding.tvShiftStatus, "Completed", Color.parseColor("#F5C518"), Color.parseColor("#1AF5C518"))
                            binding.btnShiftAction.visibility = View.GONE
                        }
                        else -> {
                            // Assigned
                            setDynamicBadge(binding.tvShiftStatus, "Assigned", Color.parseColor("#F5C518"), Color.parseColor("#1AF5C518"))
                            binding.btnShiftAction.visibility = View.VISIBLE
                            binding.btnShiftAction.text = "Request time off"
                            binding.btnShiftAction.setTextColor(Color.parseColor("#FB7185"))
                            binding.btnShiftAction.setOnClickListener { onRequestTimeOff(item) }
                        }
                    }
                }
            }
            
            private fun setDynamicBadge(tv: TextView, text: String, textColor: Int, bgColor: Int) {
                tv.text = text
                tv.setTextColor(textColor)
                val shape = GradientDrawable().apply {
                    cornerRadius = 8f * tv.resources.displayMetrics.density
                    setColor(bgColor)
                }
                tv.background = shape
            }

            private fun parseDateString(dateStr: String): Triple<String, String, String> {
                val cleanDate = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
                val parts = cleanDate.split("-")
                if (parts.size != 3) return Triple("", "", "")
                val year = parts[0]
                val monthNum = parts[1]
                val dayNum = parts[2]
                
                val monthName = when(monthNum) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> "Jul"
                }
                return Triple(dayNum, monthName, year)
            }

            private fun getDayOfWeek(dateStr: String): String {
                val cleanDate = if (dateStr.contains("T")) dateStr.substringBefore("T") else dateStr
                return try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).parse(cleanDate)
                    SimpleDateFormat("EEE", Locale.ENGLISH).format(date).uppercase()
                } catch (e: Exception) {
                    "MON"
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemStaffScheduleShiftBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount(): Int = list.size
    }
}
