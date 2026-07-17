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
import com.example.movie_booking_app.utils.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class StaffFilterBottomSheetFragment(
    private val onApplyFilter: (startDate: String?, endDate: String?, status: String?) -> Unit
) : DialogFragment() {

    private var _binding: FragmentStaffFilterBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null
    private var selectedStatus: String? = null

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var startCalendar: Calendar? = null
    private var endCalendar: Calendar? = null

    private lateinit var statusViews: Map<String, TextView>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo Map quản lý các View trạng thái chuẩn theo Constants
        statusViews = mapOf(
            Constants.STATUS_SHIFT[0] to binding.tvStatusActive,
            Constants.STATUS_SHIFT[1] to binding.tvStatusAssigned,
            Constants.STATUS_SHIFT[2] to binding.tvStatusCompleted,
            Constants.STATUS_SHIFT[3] to binding.tvAbsent,
            Constants.STATUS_SHIFT[4] to binding.tvStatusNeedReplacement
        )

        setupStatusFilter()
        setupClickListeners()
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
        statusViews.forEach { (statusName, textView) ->
            textView.setOnClickListener {
                selectedStatus = if (selectedStatus == statusName) null else statusName
                updateStatusUI()
            }
        }
    }

    private fun updateStatusUI() {
        statusViews.forEach { (statusName, textView) ->
            if (statusName == selectedStatus) {
                textView.setBackgroundResource(R.drawable.bg_tab_container)
                textView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.color_gold)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.bg_primary))
            } else {
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
                if (endCalendar != null && calendar.after(endCalendar)) {
                    Toast.makeText(context, getString(R.string.error_start_after_end), Toast.LENGTH_SHORT).show()
                    return@showDatePicker
                }
                startCalendar = calendar
                selectedStartDate = apiStr
                binding.tvStartDate.text = displayStr
            }
        }

        // Select End Date
        binding.tvEndDate.setOnClickListener {
            showDatePicker(endCalendar) { calendar, apiStr, displayStr ->
                if (startCalendar != null && calendar.before(startCalendar)) {
                    Toast.makeText(context, getString(R.string.error_end_before_start), Toast.LENGTH_SHORT).show()
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

            binding.tvStartDate.text = getString(R.string.filter_placeholder_start_date)
            binding.tvEndDate.text = getString(R.string.filter_placeholder_end_date)

            updateStatusUI()

            onApplyFilter(null, null, null)
            dismiss()
        }

        // Apply Button
        binding.btnApplyFilter.setOnClickListener {
            if (selectedStartDate != null && selectedEndDate == null) {
                Toast.makeText(context, getString(R.string.error_missing_end_date), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedStartDate == null && selectedEndDate != null) {
                Toast.makeText(context, getString(R.string.error_missing_start_date), Toast.LENGTH_SHORT).show()
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
                val localCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val apiCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    clear()
                    set(year, month, dayOfMonth)
                }

                val apiDateStr = apiDateFormat.format(apiCal.time)
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