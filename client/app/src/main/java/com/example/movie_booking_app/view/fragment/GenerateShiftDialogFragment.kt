package com.example.movie_booking_app.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.databinding.FragmentGenerateShiftDialogBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GenerateShiftDialogFragment(
    private val onGenerateClick: (startDate: String, endDate: String) -> Unit
) : DialogFragment() {

    private var _binding: FragmentGenerateShiftDialogBinding? = null
    private val binding get() = _binding!!

    private var selectedStartDate: String? = null
    private var selectedEndDate: String? = null

    // Format "yyyy-MM-dd" to send API
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var startCalendar: Calendar? = null
    private var endCalendar: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateShiftDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // Date Picker cho Start Date (isStartDate = true)
        binding.tvStartDate.setOnClickListener {
            showDatePicker(
                isStartDate = true,
                currentSelected = startCalendar
            ) { calendar, apiStr, displayStr ->
                if (endCalendar != null && calendar.after(endCalendar)) {
                    Toast.makeText(
                        context,
                        "Start date cannot be after end date",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@showDatePicker
                }
                startCalendar = calendar
                selectedStartDate = apiStr
                binding.tvStartDate.text = displayStr
            }
        }

        // Date Picker cho End Date (isStartDate = false)
        binding.tvEndDate.setOnClickListener {
            showDatePicker(
                isStartDate = false,
                currentSelected = endCalendar
            ) { calendar, apiStr, displayStr ->
                if (startCalendar != null && calendar.before(startCalendar)) {
                    Toast.makeText(
                        context,
                        "End date cannot be before start date",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@showDatePicker
                }
                endCalendar = calendar
                selectedEndDate = apiStr
                binding.tvEndDate.text = displayStr
            }
        }

        binding.btnConfirmGenerate.setOnClickListener {
            val startDate = selectedStartDate
            val endDate = selectedEndDate

            // Check null/empty
            if (startDate.isNullOrEmpty() || endDate.isNullOrEmpty()) {
                Toast.makeText(context, "Please select both start date and end date!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check minimum 7 days standard according to UTC/Day distance
            if (startCalendar != null && endCalendar != null) {
                val startCal = (startCalendar!!.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endCal = (endCalendar!!.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val diffInMillis = endCal.timeInMillis - startCal.timeInMillis
                val diffInDays = Math.round(diffInMillis.toDouble() / (1000 * 60 * 60 * 24)).toInt() + 1

                if (diffInDays < 7) {
                    Toast.makeText(
                        context,
                        "Selected range is $diffInDays days. You must select at least 7 days!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
            }

            onGenerateClick(startDate, endDate)
            dismiss()
        }
    }

    private fun showDatePicker(
        isStartDate: Boolean,
        currentSelected: Calendar?,
        onDateSelected: (Calendar, String, String) -> Unit
    ) {
        val calendar = currentSelected ?: Calendar.getInstance()

        // get 00:00:00 today
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val localCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                // check date in the past
                if (localCal.before(today)) {
                    Toast.makeText(
                        context,
                        "Cannot select a date in the past!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@DatePickerDialog
                }

                val apiDateStr = apiDateFormat.format(localCal.time)
                val displayDateStr = displayDateFormat.format(localCal.time)

                onDateSelected(localCal, apiDateStr, displayDateStr)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        //block date in the past
        val minDateInMillis = if (!isStartDate && startCalendar != null) {
            startCalendar!!.timeInMillis
        } else {
            today.timeInMillis
        }

        datePickerDialog.datePicker.minDate = minDateInMillis
        datePickerDialog.show()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt() // 85%

            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}