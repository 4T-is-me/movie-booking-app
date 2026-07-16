package com.example.movie_booking_app.view.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.DialogUpdateShiftBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.DateUtils

class UpdateShiftDialogFragment(
    private val currentShift: ShiftItemResponse,
    private val staffList: List<Pair<String, String>>, // List of Pair(StaffId, StaffName)
    private val onSaveClick: (staffId: String, shift: String, status: String) -> Unit
) : DialogFragment(){
    private var _binding: DialogUpdateShiftBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogUpdateShiftBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, viewTheme: Bundle?) {
        super.onViewCreated(view, viewTheme)
        binding.tvWorkDate.text = DateUtils.formatMovieDate(currentShift.workDate)

        val staffNames = staffList.map { it.second }
        val staffAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, staffNames)
        staffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStaff.adapter = staffAdapter

        // Set Spinner to the correct current employee of the shift
        val currentStaffIndex = staffList.indexOfFirst { it.second == currentShift.staffName }
        if (currentStaffIndex != -1) binding.spinnerStaff.setSelection(currentStaffIndex)

        // 3. Setup Spinner Shift
        val shifts = Constants.SHIFTS
        val shiftAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, shifts)
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerShift.adapter = shiftAdapter
        val currentShiftIndex = shifts.indexOf(currentShift.shift)
        if (currentShiftIndex != -1) binding.spinnerShift.setSelection(currentShiftIndex)

        // 4. Setup Spinner Status
        val statuses = Constants.STATUS_SHIFT
        val statusAdapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, statuses)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerStatus.adapter = statusAdapter
        val currentStatusIndex = statuses.indexOf(currentShift.status)
        if (currentStatusIndex != -1) binding.spinnerStatus.setSelection(currentStatusIndex)

        //cancel button
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // save button
        binding.btnSave.setOnClickListener {
            if (staffList.isNotEmpty() && binding.spinnerStaff.selectedItemPosition != -1) {
                val selectedStaffId = staffList[binding.spinnerStaff.selectedItemPosition].first
                val selectedShift = binding.spinnerShift.selectedItem.toString()
                val selectedStatus = binding.spinnerStatus.selectedItem.toString()

                onSaveClick(selectedStaffId, selectedShift, selectedStatus)
                dismiss()
            }
        }
    }
    override fun onStart() {
        super.onStart()
        //Make the outer border transparent to display the rounded-corner drawable.
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.9).toInt() // 90%

            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}