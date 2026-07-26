package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentGeneratePayrollDialogBinding
import java.util.Calendar

class GeneratePayrollDialogFragment(
    private val onGenerate: (month: Int, year: Int) -> Unit
) : DialogFragment() {

    private var _binding: FragmentGeneratePayrollDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratePayrollDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMonthSpinner()
        setupYearSpinner()

        binding.btnCancelGenerate.setOnClickListener { dismiss() }

        binding.btnGenerate.setOnClickListener {
            val monthPos = binding.spinnerMonth.selectedItemPosition
            val yearText = binding.spinnerYear.selectedItem?.toString()

            if (monthPos < 0 || yearText == null) {
                Toast.makeText(requireContext(), getString(R.string.msg_please_select_month_year), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent spam click
            binding.btnGenerate.isEnabled = false
            binding.btnCancelGenerate.isEnabled = false

            val month = monthPos + 1  // 1-based
            val year = yearText.toInt()
            onGenerate(month, year)
            dismiss()
        }
    }

    private fun setupMonthSpinner() {
        val months = listOf(
            getString(R.string.month_january),
            getString(R.string.month_february),
            getString(R.string.month_march),
            getString(R.string.month_april),
            getString(R.string.month_may),
            getString(R.string.month_june),
            getString(R.string.month_july),
            getString(R.string.month_august),
            getString(R.string.month_september),
            getString(R.string.month_october),
            getString(R.string.month_november),
            getString(R.string.month_december)
        )
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_text,
            months
        ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown) }

        binding.spinnerMonth.adapter = adapter

        // Default: current month
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-based
        binding.spinnerMonth.setSelection(currentMonth)
    }

    private fun setupYearSpinner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 2..currentYear + 1).map { it.toString() }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_spinner_text,
            years
        ).apply { setDropDownViewResource(R.layout.item_spinner_dropdown) }

        binding.spinnerYear.adapter = adapter
        // Default: current year
        val currentIndex = years.indexOf(currentYear.toString())
        if (currentIndex >= 0) binding.spinnerYear.setSelection(currentIndex)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}