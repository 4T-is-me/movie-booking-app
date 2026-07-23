package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class GenerateShiftDialogFragment(
    private val staffList: List<ManagerStaffItem>,
    private val cinemaId: String,
    private val onSaveClick: (request: GenerateShiftRequest, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
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
                    showError(getString(R.string.error_empty_staff_count))
                    return@setOnClickListener
                }

                val baseStaffCount = staffPerShiftInput.toIntOrNull() ?: 2
                if (baseStaffCount <= 0) {
                    showError(getString(R.string.error_invalid_staff_count))
                    return@setOnClickListener
                }

                val selectedStaff = staffSelectAdapter.getSelectedStaff()
                if (selectedStaff.isEmpty()) {
                    showError(getString(R.string.error_empty_staff_selection))
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
                        showError(getString(R.string.error_weekend_insufficient_staff, baseStaffCount + 1, baseStaffCount + 1))
                    } else {
                        showError(getString(R.string.error_weekday_insufficient_staff, baseStaffCount, baseStaffCount))
                    }
                    return@setOnClickListener
                }

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

                generatedDrafts = AutoStaffSchedulingAlgorithm.generateStaffSchedule(
                    startDate = startLocal,
                    endDate = endLocal,
                    selectedStaff = selectedStaff,
                    cinemaId = cinemaId,
                    baseStaffPerShift = baseStaffCount
                )

                if (generatedDrafts.isEmpty()) {
                    showError(getString(R.string.error_failed_generation))
                    return@setOnClickListener
                }

                previewAdapter.updateData(generatedDrafts)
                binding.tvPreviewTitle.visibility = View.VISIBLE
                binding.rvPreviewShifts.visibility = View.VISIBLE
                binding.tvStaffSelectionTitle.visibility = View.GONE
                binding.rvStaffSelection.visibility = View.GONE

                binding.btnConfirmGenerate.text = getString(R.string.btn_save)
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

                binding.btnConfirmGenerate.isEnabled = false

                onSaveClick(
                    requestPayload,
                    {
                        dismiss()
                    },
                    { errorMsg ->
                        handleApiError(errorMsg)
                    }
                )
            }
        }
    }

    fun handleApiError(errorMessage: String) {
        binding.btnConfirmGenerate.isEnabled = true
        showError(errorMessage)
    }

    private fun setupDurationSpinner() {
        val options = resources.getStringArray(R.array.shift_duration_options)
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