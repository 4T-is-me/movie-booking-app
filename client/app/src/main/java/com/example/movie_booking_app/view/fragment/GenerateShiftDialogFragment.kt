package com.example.movie_booking_app.view.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.request.DateRangePayload
import com.example.movie_booking_app.data.model.request.GenerateShiftRequest
import com.example.movie_booking_app.data.model.request.NewShiftRequestItem
import com.example.movie_booking_app.data.model.request.ShiftConfigPayload
import com.example.movie_booking_app.data.model.response.ManagerStaffItem
import com.example.movie_booking_app.databinding.FragmentGenerateShiftDialogBinding
import com.example.movie_booking_app.utils.AutoStaffSchedulingAlgorithm
import com.example.movie_booking_app.utils.StaffShiftDraft
import com.example.movie_booking_app.view.adapter.manager.ManagerShiftDraftPreviewAdapter
import com.example.movie_booking_app.view.adapter.manager.ManagerStaffSelectAdapter
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Calendar
import java.util.Locale

class GenerateShiftDialogFragment(
    private val staffList: List<ManagerStaffItem>,
    private val cinemaId: String,
    private val onSaveClick: (GenerateShiftRequest) -> Unit
) : DialogFragment() {

    private var _binding: FragmentGenerateShiftDialogBinding? = null
    private val binding get() = _binding!!
    private var computedStartDateStr: String = ""
    private var computedEndDateStr: String = ""
    private lateinit var previewAdapter: ManagerShiftDraftPreviewAdapter
    private lateinit var staffSelectAdapter: ManagerStaffSelectAdapter
    private var generatedDrafts: List<StaffShiftDraft> = emptyList()
    private var isPreviewMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenerateShiftDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDurationSpinner()
        setupStaffSelectionRecyclerView()
        setupPreviewRecyclerView()
        setUpClickListener()

    }
    private fun setUpClickListener(){
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnConfirmGenerate.setOnClickListener {
            if (!isPreviewMode) {
                val staffPerShiftInput = binding.etStaffPerShift.text.toString().trim()

                if (staffPerShiftInput.isEmpty()) {
                    showError("Please enter staff count!")
                    return@setOnClickListener
                }

                val baseStaffCount = staffPerShiftInput.toIntOrNull() ?: 2
                if (baseStaffCount <= 0) {
                    showError("Staff count must be greater than 0")
                    return@setOnClickListener
                }

                val selectedStaff = staffSelectAdapter.getSelectedStaff()
                if (selectedStaff.isEmpty()) {
                    showError("Please select at least 1 staff member!")
                    return@setOnClickListener
                }

                val today = LocalDate.now()
                val startLocal = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                val endLocal = if (binding.spinnerDuration.selectedItemPosition == 1) {
                    startLocal.plusMonths(1).minusDays(1)
                } else {
                    startLocal.plusDays(6)
                }

                var hasSpecialDay = false
                var tempDate = startLocal

                while (!tempDate.isAfter(endLocal)) {
                    if (tempDate.dayOfWeek == DayOfWeek.SATURDAY || tempDate.dayOfWeek == DayOfWeek.SUNDAY) {
                        hasSpecialDay = true
                        break
                    }
                    tempDate = tempDate.plusDays(1)
                }

                val maxRequiredStaffForPeriod = if (hasSpecialDay) baseStaffCount + 1 else baseStaffCount

                if (selectedStaff.size < maxRequiredStaffForPeriod) {
                    if (hasSpecialDay) {
                        showError("Because this period includes weekends, each shift needs ${baseStaffCount + 1} staff. Please select at least ${baseStaffCount + 1} staff members!")
                    } else {
                        showError("Each shift needs $baseStaffCount staff. Please select at least $baseStaffCount staff members!")
                    }
                    return@setOnClickListener
                }

                // Hàm này sẽ check trước, nếu thiếu người gánh ca nó sẽ trả về false kèm câu thông báo lỗi chi tiết
                val validationResult = AutoStaffSchedulingAlgorithm.validateSchedulingCapability(
                    startDate = startLocal,
                    endDate = endLocal,
                    selectedStaff = selectedStaff,
                    baseStaffPerShift = baseStaffCount
                )

                if (!validationResult.first) {
                    showError(validationResult.second)
                    return@setOnClickListener
                }

                binding.tvErrorMessage.visibility = View.GONE

                computedStartDateStr = startLocal.toString()
                computedEndDateStr = endLocal.toString()

                // Gọi hàm sinh ca (lúc này thuật toán mới đã fix chặt chẽ dailyCount == 0, tối đa chỉ ra được ca thực tế, không bị 98 nữa)
                generatedDrafts = AutoStaffSchedulingAlgorithm.generateStaffSchedule(
                    startDate = startLocal,
                    endDate = endLocal,
                    selectedStaff = selectedStaff,
                    cinemaId = cinemaId,
                    baseStaffPerShift = baseStaffCount
                )

                if (generatedDrafts.isEmpty()) {
                    showError("Failed to schedule. The selected staff combination cannot fulfill the 6-day work week constraint.")
                    return@setOnClickListener
                }

                previewAdapter.updateData(generatedDrafts)
                binding.tvPreviewTitle.visibility = View.VISIBLE
                binding.rvPreviewShifts.visibility = View.VISIBLE
                binding.tvStaffSelectionTitle.visibility = View.GONE
                binding.rvStaffSelection.visibility = View.GONE

                binding.btnConfirmGenerate.text = "Save"
                isPreviewMode = true
            } else {

                if (computedStartDateStr.isEmpty() || computedEndDateStr.isEmpty()) return@setOnClickListener

                val requestPayload = GenerateShiftRequest(
                    dateRange = DateRangePayload(computedStartDateStr, computedEndDateStr),
                    config = ShiftConfigPayload(binding.etStaffPerShift.text.toString().toInt()),
                    shifts = generatedDrafts.map {
                        NewShiftRequestItem(
                            staffId = it.staffId,
                            cinemaId = it.cinemaId,
                            workDate = it.workDate,
                            shift = it.shift,
                            status = it.status,
                            isHoliday = it.isHoliday
                        )
                    }
                )
                onSaveClick(requestPayload)
                dismiss()
            }
        }
    }

    private fun setupDurationSpinner() {
        val options = arrayOf("1 Week", "1 Month")
        val adapter = ArrayAdapter(requireContext(), R.layout.item_spinner_text, options).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerDuration.adapter = adapter
    }

    private fun setupStaffSelectionRecyclerView() {
        staffSelectAdapter = ManagerStaffSelectAdapter { _ -> }
        binding.rvStaffSelection.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = staffSelectAdapter
        }
        staffSelectAdapter.submitList(staffList)
    }

    private fun setupPreviewRecyclerView() {
        previewAdapter = ManagerShiftDraftPreviewAdapter()
        binding.rvPreviewShifts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = previewAdapter
        }
    }
    private fun showError(message: String) {
        binding.tvErrorMessage.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}