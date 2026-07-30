package com.example.movie_booking_app.view.activity.manager

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ManagerLeaveRequestResponse
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.ActivityManagerStaffBinding
import com.example.movie_booking_app.databinding.DialogErrorCustomBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerLeaveRequestAdapter
import com.example.movie_booking_app.view.adapter.manager.ManagerStaffShiftAdapter
import com.example.movie_booking_app.view.fragment.GenerateShiftDialogFragment
import com.example.movie_booking_app.view.fragment.StaffFilterBottomSheetFragment
import com.example.movie_booking_app.view.fragment.UpdateShiftDialogFragment
import com.example.movie_booking_app.view.fragment.staff.LeaveStatusFilterDialogFragment
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.example.movie_booking_app.viewmodel.staff.LeaveRequestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerStaffActivity : AppCompatActivity() {

    private val viewModel: ManagerViewModel by viewModels()
    private val leaveRequestViewModel: LeaveRequestViewModel by viewModels()
    private lateinit var tabButtons: List<TextView>
    private lateinit var binding: ActivityManagerStaffBinding
    private var originalShiftList: List<ShiftItemResponse> = emptyList()
    private var originalLeaveRequests: List<ManagerLeaveRequestResponse> = emptyList()
    private lateinit var leaveRequestAdapter: ManagerLeaveRequestAdapter

    private var selectedStatusFilter: String? = null

    private var currentTab = "staff_list"
    private lateinit var staffShiftAdapter: ManagerStaffShiftAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerStaffBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tabButtons = listOf(
            binding.tabStaffList,
            binding.tabLeaveRequests
        )
        viewModel.loadStaffForManager()


        setUpClickListeners()
        setUpRecyclerView()
        observeShiftData()
        observeLeaveRequestData()
        observeGenerateShiftResult()
        observeUpdateShiftResult()
        setUpSearchListener()

        val targetTab = intent.getStringExtra("target_tab")
        if (targetTab == "leave_requests") {
            setActiveTab(binding.tabLeaveRequests, "leave_requests")
        } else {
            setActiveTab(binding.tabStaffList, "staff_list")
        }
    }

    private fun setUpRecyclerView() {
        staffShiftAdapter = ManagerStaffShiftAdapter(
            onItemClick = { shift ->
                val intent = Intent(this, ManagerShiftDetailActivity::class.java)
                intent.putExtra("shiftId", shift.shiftId)
                startActivity(intent)
            },
            onEditClick = { shiftItem ->
                showUpdateShiftDialog(shiftItem)
            }
        )
        leaveRequestAdapter = ManagerLeaveRequestAdapter(object : ManagerLeaveRequestAdapter.LeaveRequestListener {
            override fun onApprove(item: ManagerLeaveRequestResponse) {
                leaveRequestViewModel.approveLeaveRequest(item.leaveRequestId)
            }

            override fun onReject(item: ManagerLeaveRequestResponse) {
                showRejectDialog(item)
            }

            override fun onUndo(item: ManagerLeaveRequestResponse) {
                showConfirmUndoDialog(item)
            }
        })

        binding.rvStaffList.apply {
            layoutManager = LinearLayoutManager(this@ManagerStaffActivity)
            adapter = staffShiftAdapter
        }
    }

    private fun setUpClickListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tabStaffList.setOnClickListener {
            setActiveTab(binding.tabStaffList,  "staff_list")
        }
        binding.tabLeaveRequests.setOnClickListener {
            setActiveTab(binding.tabLeaveRequests, "leave_requests")
        }

        binding.btnAdvancedFilter?.setOnClickListener {
            if (currentTab == "staff_list") {
                showAdvancedFilterBottomSheet()
            } else {
                showLeaveStatusFilterBottomSheet()
            }
        }
        binding.btnGenerateShift.setOnClickListener {
            showGenerateShiftDialog()
        }
    }

    private fun setActiveTab(selectedTab: TextView, tabType: String) {
        currentTab = tabType
        val secondaryTextColor = ContextCompat.getColor(this, R.color.text_secondary)
        for(tab in tabButtons) {
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.setTextColor(secondaryTextColor)
            tab.setTypeface(null, Typeface.BOLD)
        }
        selectedTab.setBackgroundResource(R.drawable.bg_tab_active_gold)
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.bg_primary))
        selectedTab.setTypeface(null, Typeface.BOLD)

        if(tabType == "staff_list") {
            binding.btnGenerateShift.visibility = View.VISIBLE
            binding.rvStaffList.adapter = staffShiftAdapter
            viewModel.loadStaffShifts()
        }else {
            binding.btnGenerateShift.visibility = View.GONE
            binding.rvStaffList.adapter = leaveRequestAdapter
            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
        }
    }

    private fun showGenerateShiftDialog() {
        val staffResponse = viewModel.staffListData.value

        if (staffResponse?.success == true && staffResponse.data != null) {
            val activeStaffList = staffResponse.data.staff
            val cinemaId = staffResponse.data.cinemaId

            if (activeStaffList.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_no_active_staff), Toast.LENGTH_LONG).show()
                return
            }
            val dialog = GenerateShiftDialogFragment(
                staffList = activeStaffList,
                cinemaId = cinemaId,
                onSaveClick = { requestPayload, _, _ ->
                    viewModel.saveGeneratedShifts(requestPayload)
                }
            )
            dialog.show(supportFragmentManager, "GenerateShiftDialogFragment")
        } else {
            Toast.makeText(this, getString(R.string.toast_fetching_staff_wait), Toast.LENGTH_SHORT).show()
            viewModel.loadStaffForManager()
        }
    }

    private fun observeGenerateShiftResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.generateShiftData.collectLatest { response ->
                    if(response != null) {
                        if (response.success) {
                            (supportFragmentManager.findFragmentByTag("GenerateShiftDialogFragment") as? androidx.fragment.app.DialogFragment)?.dismiss()
                            viewModel.loadStaffShifts()
                            Toast.makeText(this@ManagerStaffActivity, getString(R.string.auto_str_success), Toast.LENGTH_SHORT).show()
                        } else {
                            showErrorDialog(response.message ?: getString(R.string.error_generate_shift_failed))
                        }
                        viewModel.clearGenerateShiftState()
                    }
                }
            }
        }
    }
    private fun setUpShiftList(shifts: List<ShiftItemResponse>) {
        val shiftItems = shifts.orEmpty()
        if (shiftItems.isEmpty()) {
            binding.rvStaffList.visibility = View.GONE
            binding.txtNoStaffData?.visibility = View.VISIBLE
        } else {
            binding.rvStaffList.visibility = View.VISIBLE
            binding.txtNoStaffData?.visibility = View.GONE
            staffShiftAdapter.updateData(shiftItems)
        }
    }

    private fun observeShiftData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shiftData.collectLatest { response ->
                    response?.data?.let { shiftList ->
                        if (currentTab == "staff_list") {
                            originalShiftList = shiftList
                            val currentQuery = binding.etSearchStaff?.text?.toString().orEmpty()
                            applySearchFilter(currentQuery)
                        }
                    }?: run {
                        originalShiftList = emptyList()
                        setUpShiftList(emptyList())
                    }
                }
            }
        }
    }

    private fun observeUpdateShiftResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateShiftResult.collectLatest { response ->
                    if (response != null) {
                        if (response.success) {
                            Toast.makeText(this@ManagerStaffActivity, response.message ?: getString(R.string.toast_update_shift_success), Toast.LENGTH_SHORT).show()
                            viewModel.loadStaffShifts()
                        } else {
                            showErrorDialog(response.message ?: getString(R.string.error_unknown_backend))
                        }
                        viewModel.clearUpdateState()
                    }
                }
            }
        }
    }

    private fun observeLeaveRequestData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.managerLeaveRequests.collectLatest { list ->
                    if (currentTab == "leave_requests") {
                        originalLeaveRequests = list.orEmpty()
                        setUpLeaveRequestList(originalLeaveRequests)

                        val currentQuery = binding.etSearchStaff?.text?.toString().orEmpty()
                        if (currentQuery.isNotEmpty()) {
                            applySearchFilter(currentQuery)
                        }
                    }
                }
            }
        }

        // Observe Approve Result
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.approveResult.collectLatest { response ->
                    if (response != null) {
                        if (response.success) {
                            Toast.makeText(this@ManagerStaffActivity, getString(R.string.toast_leave_approved), Toast.LENGTH_SHORT).show()
                            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
                        } else {
                            showErrorDialog(getString(R.string.error_approve_leave_failed))
                        }
                        leaveRequestViewModel.clearApproveState()
                    }
                }
            }
        }

        // Observe Reject Result
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.rejectResult.collectLatest { response ->
                    if (response != null) {
                        if (response.success) {
                            Toast.makeText(this@ManagerStaffActivity, getString(R.string.toast_leave_rejected), Toast.LENGTH_SHORT).show()
                            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
                        } else {
                            showErrorDialog(getString(R.string.error_reject_leave_failed))
                        }
                        leaveRequestViewModel.clearRejectState()
                    }
                }
            }
        }

        // Observe Undo Result
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.undoResult.collectLatest { response ->
                    if (response != null) {
                        if (response.success) {
                            Toast.makeText(this@ManagerStaffActivity, getString(R.string.msg_undo_success), Toast.LENGTH_SHORT).show()
                            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
                        } else {
                            showErrorDialog(getString(R.string.error_undo_failed))
                        }
                        leaveRequestViewModel.clearUndoState()
                    }
                }
            }
        }

        // Loading state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.isLoading.collectLatest { isLoading ->
                    binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // Error state displaying Dialog
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.errorMessage.collectLatest { error ->
                    if (error != null) {
                        showErrorDialog(error)
                        leaveRequestViewModel.clearError()
                    }
                }
            }
        }
    }

    private fun setUpLeaveRequestList(requests: List<ManagerLeaveRequestResponse>) {
        if (requests.isEmpty()) {
            binding.rvStaffList.visibility = View.GONE
            binding.txtNoStaffData?.text = getString(R.string.msg_no_leave_requests)
            binding.txtNoStaffData?.visibility = View.VISIBLE
        } else {
            binding.rvStaffList.visibility = View.VISIBLE
            binding.txtNoStaffData?.visibility = View.GONE
            leaveRequestAdapter.updateData(requests)
        }
    }

    private fun applySearchFilter(query: String) {
        val trimmedQuery = query.trim().lowercase()
        if (currentTab == "staff_list") {
            if (trimmedQuery.isEmpty()) {
                setUpShiftList(originalShiftList)
            } else {
                val filteredList = originalShiftList.filter { item ->
                    item.staffName?.lowercase()?.contains(trimmedQuery) == true ||
                            item.position?.lowercase()?.contains(trimmedQuery) == true ||
                            item.shift?.lowercase()?.contains(trimmedQuery) == true ||
                            item.status?.lowercase()?.contains(trimmedQuery) == true
                }
                setUpShiftList(filteredList)
            }
        } else {
            if (trimmedQuery.isEmpty()) {
                setUpLeaveRequestList(originalLeaveRequests)
            } else {
                val filteredList = originalLeaveRequests.filter { item ->
                    item.staffName.lowercase().contains(trimmedQuery) ||
                            item.staffPosition.lowercase().contains(trimmedQuery) ||
                            item.reason.lowercase().contains(trimmedQuery) ||
                            item.status.lowercase().contains(trimmedQuery)
                }
                setUpLeaveRequestList(filteredList)
            }
        }
    }

    private fun setUpSearchListener() {
        binding.etSearchStaff?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun showAdvancedFilterBottomSheet() {
        val filterFragment = StaffFilterBottomSheetFragment { startDate, endDate, status ->
            viewModel.loadStaffShifts(
                startDate = startDate,
                endDate = endDate,
                status = status
            )
        }
        filterFragment.show(supportFragmentManager, "StaffFilterBottomSheet")
    }

    private fun showUpdateShiftDialog(shiftItem: ShiftItemResponse) {
        val staffListPair = originalShiftList
            .filter { !it.staffId.isNullOrEmpty() && !it.staffName.isNullOrEmpty() }
            .map { Pair(it.staffId!!, it.staffName!!) }
            .distinctBy { it.first }

        val dialog = UpdateShiftDialogFragment(shiftItem, staffListPair) { staffId, shift, status ->
            viewModel.updateShift(shiftItem.shiftId ?: "", staffId, shift, status)
        }
        dialog.show(supportFragmentManager, "UpdateShiftDialogFragment")
    }

    private fun showLeaveStatusFilterBottomSheet() {
        val dialog = LeaveStatusFilterDialogFragment { selectedStatus ->
            selectedStatusFilter = selectedStatus
            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
        }
        dialog.show(supportFragmentManager, "LeaveStatusFilterDialogFragment")
    }

    private fun showRejectDialog(item: ManagerLeaveRequestResponse) {
        val builder = AlertDialog.Builder(this)
        val dialogBinding = com.example.movie_booking_app.databinding.DialogRejectLeaveCustomBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Cancel
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        // Confirm Reject
        dialogBinding.btnSubmitLeave.setOnClickListener {
            val reason = dialogBinding.etLeaveReason .text.toString().trim()
            if (reason.isNotBlank()) {
                leaveRequestAdapter.setLocalRejectState(item.leaveRequestId)

                // Call API to send request to server
                leaveRequestViewModel.rejectLeaveRequest(item.leaveRequestId, reason)
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.error_empty_reason_reject), Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.setLayout(width, height)
    }

    private fun showErrorDialog(backendErrorMessage: String) {
        val builder = AlertDialog.Builder(this)
        val dialogBinding = DialogErrorCustomBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.tvDialogMessage.text = backendErrorMessage
        dialogBinding.btnDialogOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showConfirmUndoDialog(item: ManagerLeaveRequestResponse) {
        val builder = AlertDialog.Builder(this)
        val dialogBinding = com.example.movie_booking_app.databinding.DialogUndoLeaveCustomBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmUndo.setOnClickListener {
            leaveRequestViewModel.undoLeaveRequest(item.leaveRequestId)
            dialog.dismiss()
        }
        dialog.show()

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.setLayout(width, height)
    }
}