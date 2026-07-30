package com.example.movie_booking_app.view.fragment.staff

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ShowDate
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.DialogRequestLeaveBinding
import com.example.movie_booking_app.databinding.FragmentStaffScheduleBinding
import com.example.movie_booking_app.databinding.ItemStaffScheduleShiftBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.ReplacementShiftAdapter
import com.example.movie_booking_app.viewmodel.staff.LeaveRequestViewModel
import com.example.movie_booking_app.viewmodel.staff.StaffScheduleViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class StaffScheduleFragment : Fragment() {

    private var _binding: FragmentStaffScheduleBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffScheduleViewModel by viewModels()
    private val leaveRequestViewModel: LeaveRequestViewModel by viewModels()

    private lateinit var dateAdapter: DateAdapter
    private lateinit var staffShiftAdapter: StaffShiftAdapter
    private lateinit var replacementShiftAdapter: ReplacementShiftAdapter

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

        replacementShiftAdapter = ReplacementShiftAdapter { item ->
            item.shiftId?.let { id ->
                leaveRequestViewModel.acceptReplacementShift(id)
            }?: Toast.makeText(requireContext(), "Invalid Shift ID", Toast.LENGTH_SHORT).show()
        }

        binding.rvNeedReplacementShifts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = replacementShiftAdapter
        }
        binding.rvWeeklyCalendar.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }

        // Shifts List Adapter
        staffShiftAdapter = StaffShiftAdapter(
            onRequestTimeOff = { item ->
                showRequestLeaveDialog(item)
            },
            onRequestReplacement = { item ->
                Toast.makeText(requireContext(), getString(R.string.error_cannot_accept_own_shift), Toast.LENGTH_SHORT).show()
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
                Timber.tag("StaffSchedule").d("Received shifts count: ${shiftList.size}")
                staffShiftAdapter.submitList(shiftList)

                // Set default display for today's selected date in calendar strip
                val activeDate = dateAdapter.currentList.find { it.isSelected }?.id
                Timber.tag("StaffSchedule").d("Active date on load: $activeDate")
                if (activeDate != null) {
                    updateTodayShiftCard(activeDate)
                }
            }
        }
        lifecycleScope.launch {
            leaveRequestViewModel.replacementShifts.collectLatest { replacementList ->
                if (!replacementList.isNullOrEmpty()) {
                    binding.tvNeedReplacementHeader.visibility = View.VISIBLE
                    binding.rvNeedReplacementShifts.visibility = View.VISIBLE
                    replacementShiftAdapter.submitList(replacementList)
                } else {
                    binding.tvNeedReplacementHeader.visibility = View.GONE
                    binding.rvNeedReplacementShifts.visibility = View.GONE
                }
            }
        }

        lifecycleScope.launch {
            leaveRequestViewModel.acceptShiftResult.collectLatest { response ->
                if (response != null) {
                    Toast.makeText(
                        requireContext(),
                        "Shift accepted successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    leaveRequestViewModel.clearAcceptShiftState()
                    loadData()
                }
            }
        }

        lifecycleScope.launch {
            leaveRequestViewModel.createLeaveResult.collectLatest { response ->
                if (response != null) {
                    Toast.makeText(
                        requireContext(),
                        "Leave request submitted successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    leaveRequestViewModel.clearCreateLeaveState()
                    val activeDate = dateAdapter.currentList.find { it.isSelected }?.id
                    loadData()
                }
            }
        }

        lifecycleScope.launch {
            leaveRequestViewModel.errorMessage.collectLatest { rawMessage ->
                if (rawMessage != null) {
                    val displayMessage = when (rawMessage) {
                       Constants.ERROR_DUPLICATE_SHIFT -> getString(R.string.error_duplicate_shift)
                        Constants.ERROR_UNKNOWN -> getString(R.string.error_unknown)
                        else -> {
                            try {
                                val jsonObject = org.json.JSONObject(rawMessage)
                                val errors = jsonObject.optJSONArray("errors")
                                if (errors != null && errors.length() > 0) {
                                    errors.getJSONObject(0).optString("message", rawMessage)
                                } else {
                                    jsonObject.optString("message", rawMessage)
                                }
                            } catch (e: Exception) {
                                rawMessage
                            }
                        }
                    }

                    Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_LONG).show()
                    leaveRequestViewModel.clearError()
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
            Timber.tag("StaffSchedule").d("Requesting shifts from $startDate to $endDate")
            viewModel.loadShifts(startDate, endDate)
            leaveRequestViewModel.loadReplacementShifts()
        }
    }

    private fun updateTodayShiftCard(selectedDateId: String) {
        val shifts = viewModel.shifts.value
        Timber.tag("StaffSchedule").d("Updating card for date: $selectedDateId. Total shifts: ${shifts.size}")

        // Filter out ALL duplicate dates
        val matchingShifts = shifts.filter {
            val cleanDate = if (it.workDate?.contains("T") == true) it.workDate.substringBefore("T") else it.workDate
            cleanDate == selectedDateId
        }

        // Clear all shifts from the previous day that were previously added to the container
        binding.lnTodayShiftsContainer.removeAllViews()

        val layoutInflater = LayoutInflater.from(requireContext())

        if (matchingShifts.isNotEmpty()) {
            binding.tvTodayHeader.visibility = View.VISIBLE

            // Review each shift for that day to create a separate Card
            matchingShifts.forEach { item ->
                val itemBinding = com.example.movie_booking_app.databinding.ItemTodaySubShiftBinding.inflate(
                    layoutInflater,
                    binding.lnTodayShiftsContainer,
                    false
                )
                // shift name data (Morning, Afternoon,...)
                itemBinding.tvSubShiftTitle.text = "${item.shift} Shift"
                // stauts Badge
                itemBinding.tvSubStatusBadge.text = item.status ?: "Assigned"

                // working hours data
                itemBinding.tvSubShiftHours.text = when (item.shift) {
                    Constants.SHIFTS[0] -> Constants.MORNING
                    Constants.SHIFTS[1]  -> Constants.AFTERNOON
                    Constants.SHIFTS[2]  -> Constants.EVENING
                    Constants.SHIFTS[3]  -> Constants.NIGHT
                    else -> Constants.FULLTIME
                }

                itemBinding.tvSubShiftLocation.text = "${item.position} • ${getString(R.string.auto_str_ticket_counter_staff_member)}"

                binding.lnTodayShiftsContainer.addView(itemBinding.root)
            }
        } else {
            // For days off – Issue an "Off" card featuring a matching design.
            binding.tvTodayHeader.visibility = View.VISIBLE

            val itemBinding = com.example.movie_booking_app.databinding.ItemTodaySubShiftBinding.inflate(
                layoutInflater,
                binding.lnTodayShiftsContainer,
                false
            )

            // Custom UI for holidays
            itemBinding.tvSubShiftTitle.text = "No Shift Scheduled"
            itemBinding.tvSubStatusBadge.text = "Off"
            itemBinding.layoutSubHours.visibility = View.GONE // Hide the time row
            itemBinding.tvSubShiftLocation.text = "Enjoy your day off!"

            binding.lnTodayShiftsContainer.addView(itemBinding.root)
        }
    }

    private fun showRequestLeaveDialog(shiftItem: ShiftItemResponse) {
        val context = requireContext()
        val dialog = android.app.Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val dialogBinding = DialogRequestLeaveBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(dialogBinding.root)

        //cancel
        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        //(Submit)
        dialogBinding.btnDialogSubmit.setOnClickListener {
            val reason = dialogBinding.etDialogReason.text.toString().trim()

            if (reason.isEmpty()) {
                Toast.makeText(context, getString(R.string.leave_dialog_empty_error), Toast.LENGTH_SHORT).show()
            } else {
                dialogBinding.btnDialogSubmit.isEnabled = false
                dialogBinding.btnDialogSubmit.alpha = 0.5f

                shiftItem.shiftId?.let { id ->
                    leaveRequestViewModel.createLeaveRequest(id, reason)
                }
                dialog.dismiss()
            }
        }

        dialog.window?.apply {
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.show()
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
                    binding.tvShiftName.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
                    binding.layoutShiftTime.visibility = View.GONE
                    binding.btnShiftAction.visibility = View.GONE

                    setDynamicBadge(binding.tvShiftStatus, "Off", ContextCompat.getColor(requireContext(), R.color.text_dialog_hint), ContextCompat.getColor(requireContext(), R.color.auto_color_1A64748B))
                } else {
                    binding.layoutShiftTime.visibility = View.VISIBLE
                    binding.tvShiftName.setTextColor(Color.WHITE)

                    val shiftName = "${item.shift} Shift"
                    val shiftTime = when (item.shift) {
                        Constants.SHIFTS[0] -> Constants.MORNING
                        Constants.SHIFTS[1]-> Constants.AFTERNOON
                        Constants.SHIFTS[2]-> Constants.EVENING
                        Constants.SHIFTS[3]-> Constants.NIGHT
                        else -> Constants.FULLTIME
                    }

                    binding.tvShiftName.text = shiftName
                    binding.tvShiftTimeText.text = shiftTime

                    // Action buttons displaying dynamic labels based on conditions
                    when (item.status) {
                        Constants.STATUS_SHIFT[4] -> {
                            setDynamicBadge(
                                binding.tvShiftStatus,
                                Constants.STATUS_SHIFT[4],
                                ContextCompat.getColor(context, R.color.status_rose_red),
                                ContextCompat.getColor(context, R.color.bg_badge_rose_red)
                            )
                            binding.btnShiftAction.visibility = View.VISIBLE
                            binding.btnShiftAction.text = context?.getString(R.string.action_accept_replacement)
                            binding.btnShiftAction.setTextColor(ContextCompat.getColor(context, R.color.status_gold_yellow))
                            binding.btnShiftAction.setOnClickListener { onRequestReplacement(item) }
                        }
                        Constants.STATUS_SHIFT[2] -> {
                            setDynamicBadge(
                                binding.tvShiftStatus,
                                Constants.STATUS_SHIFT[2],
                                ContextCompat.getColor(context, R.color.status_gold_yellow),
                                ContextCompat.getColor(context, R.color.bg_badge_gold_yellow)
                            )
                            binding.btnShiftAction.visibility = View.GONE
                        }
                        else -> {
                            val badgeStatus = item.status ?: Constants.STATUS_SHIFT[1]
                            setDynamicBadge(
                                binding.tvShiftStatus,
                                badgeStatus,
                                ContextCompat.getColor(context, R.color.status_gold_yellow),
                                ContextCompat.getColor(context, R.color.bg_badge_gold_yellow)
                            )
                            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(java.util.Date())
                            val cleanDate = if (item.workDate?.contains("T") == true) item.workDate.substringBefore("T") else item.workDate

                            if (badgeStatus != Constants.STATUS_SHIFT[1] || (cleanDate != null && cleanDate <= todayStr)) {
                                binding.btnShiftAction.visibility = View.GONE
                            } else {
                                binding.btnShiftAction.visibility = View.VISIBLE
                                binding.btnShiftAction.text = context?.getString(R.string.action_request_time_off)
                                binding.btnShiftAction.setTextColor(ContextCompat.getColor(context, R.color.status_light_rose))
                                binding.btnShiftAction.setOnClickListener { onRequestTimeOff(item) }
                            }
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
                    if (date != null) {
                        SimpleDateFormat("EEE", Locale.ENGLISH).format(date).uppercase()
                    } else {
                        "MON"
                    }
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
