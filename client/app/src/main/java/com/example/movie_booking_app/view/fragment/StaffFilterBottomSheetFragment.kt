package com.example.movie_booking_app.view.fragment

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentStaffFilterBottomSheetBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StaffFilterBottomSheetFragment(private val onApplyFilter: (startDate: String?, endDate: String?, status: String?)-> Unit
) : DialogFragment(){
    private var _binding: FragmentStaffFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    // Variable storing the ISO string sent to the API (consistent with the backend's date format)
    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var selectedStatus: String? = null
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var startCalendar: Calendar? = null
    private var endCalendar: Calendar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupStatusFilter()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }
    }
    private fun setupStatusFilter() {
        val statusViews = mapOf(
            "Active" to binding.tvStatusActive,
            "Assigned" to binding.tvStatusAssigned,
            "Completed" to binding.tvStatusCompleted,
            "NeedReplacement" to binding.tvStatusNeedReplacement
        )

        statusViews.forEach { (statusName, textView) ->
            textView.setOnClickListener {
                if (selectedStatus == statusName) {
                    selectedStatus = null
                } else {
                    selectedStatus = statusName
                }
                updateStatusUI(statusViews)
            }
        }
    }
    private fun updateStatusUI(statusViews: Map<String, TextView>) {
        statusViews.forEach { (statusName, textView) ->
            if (statusName == selectedStatus) {
                // Trạng thái được chọn: đổi background màu vàng, chữ màu tối
                textView.setBackgroundResource(R.drawable.bg_tab_container)
                textView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.color_gold)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_primary))
            } else {
                // Trạng thái không được chọn: trả về mặc định
                textView.backgroundTintList = null
                textView.setBackgroundResource(R.drawable.bg_tab_container)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
    }

    private fun setupClickListeners() {
        // Select Start Date
        binding.tvStartDate.setOnClickListener {
            showDatePicker(startCalendar) { calendar, apiStr, displayStr ->
                // Check for duplicate selection, or if the end date was already selected but the start date is later than it.
                if (endCalendar != null && calendar.after(endCalendar)) {
                    Toast.makeText(context, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
                    return@showDatePicker
                }
                startCalendar = calendar
                selectedStartDate = apiStr
                binding.tvStartDate.text = displayStr
            }
        }

        //  Select End Date
        binding.tvEndDate.setOnClickListener {
            showDatePicker(endCalendar) { calendar, apiStr, displayStr ->
                // CHECK EndDate is less than StartDate
                if (startCalendar != null && calendar.before(startCalendar)) {
                    Toast.makeText(context, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                    return@showDatePicker
                }
                endCalendar = calendar
                selectedEndDate = apiStr
                binding.tvEndDate.text = displayStr
            }
        }

        // Reset Button
        binding.btnResetFilter.setOnClickListener {
            selectedStartDate = null
            selectedEndDate = null
            selectedStatus = null
            startCalendar = null
            endCalendar = null

            binding.tvStartDate.text = "Select the start date"
            binding.tvEndDate.text = "Select the last day"

            updateStatusUI(mapOf(
                "Active" to binding.tvStatusActive,
                "Assigned" to binding.tvStatusAssigned,
                "Completed" to binding.tvStatusCompleted,
                "NeedReplacement" to binding.tvStatusNeedReplacement
            ))

            onApplyFilter(null, null, null)
            dismiss()
        }

        // Apply Button
        binding.btnApplyFilter.setOnClickListener {
            if (selectedStartDate != null && selectedEndDate == null) {
                Toast.makeText(context, "Please select end date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStartDate == null && selectedEndDate != null) {
                Toast.makeText(context, "Please select start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onApplyFilter(selectedStartDate, selectedEndDate, selectedStatus)
            dismiss()
        }


    }

    private fun showDatePicker(
        currentSelected: Calendar?,
        onDateSelected: (Calendar, String, String) -> Unit
    ) {
        val calendar = currentSelected ?: Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Use a temporary Calendar to accurately calculate the local date based on the Vietnam time zone.
                val localCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                /// Create a standardized, consistent Calendar object to send to the API
                val apiCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, month, dayOfMonth)
                }

                val apiDateStr = apiDateFormat.format(apiCal.time)
                // The generated string will always be: yyyy-MM-ddT00:00:00.000Z — fixed and no longer offset to 17:00!

                val displayDateStr = displayDateFormat.format(localCal.time)

                onDateSelected(localCal, apiDateStr, displayDateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}