package com.example.movie_booking_app.view.activity.manager

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
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
import com.example.movie_booking_app.view.fragment.GenerateShiftSuccessDialogFragment
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

        setUpClickListeners()
        setUpRecyclerView()
        observeShiftData()
        observeLeaveRequestData()
        observeGenerateShiftResult()
        observeUpdateShiftResult()
        setUpSearchListener()

        setActiveTab(binding.tabStaffList, "staff_list")
    }

    private fun setUpRecyclerView () {
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
        // Setup Leave Request Adapter mới có tích hợp Undo
        leaveRequestAdapter = ManagerLeaveRequestAdapter(object : ManagerLeaveRequestAdapter.LeaveRequestListener {
            override fun onApprove(item: ManagerLeaveRequestResponse) {
                leaveRequestViewModel.approveLeaveRequest(item.leaveRequestId)
            }

            override fun onReject(item: ManagerLeaveRequestResponse) {
                // Task 3: Hiện dialog nhập lý do reject
                showRejectDialog(item)
            }

            override fun onUndo(item: ManagerLeaveRequestResponse) {
                // Task 3: Gọi API revert thực tế sẽ lắp ở đây
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

        //open BottomSheet
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
        val dialog = GenerateShiftDialogFragment {startDate, endDate ->
            viewModel.loadGenerateShiftData(startDate, endDate)
        }
        dialog.show(supportFragmentManager, "GenerateShiftDialogFragment")
    }

    private fun observeGenerateShiftResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.generateShiftData.collectLatest { response ->
                    if(response != null) {
                        if (response.success) {
                            val successDialog = GenerateShiftSuccessDialogFragment(response.data) {
                                viewModel.loadStaffShifts() // Reload shift list
                            }
                            successDialog.show(supportFragmentManager, "GenerateShiftSuccessDialogFragment")
                        }else {
                            Toast.makeText(this@ManagerStaffActivity, response.message, Toast.LENGTH_SHORT).show()
                        }
                        // Reset state
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
                            // Store the newly retrieved original list
                            originalShiftList = shiftList

                            // Re-filter immediately
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
                            // success
                            Toast.makeText(this@ManagerStaffActivity, response.message ?: "Update successful", Toast.LENGTH_SHORT).show()
                            viewModel.loadStaffShifts() // reload list shift
                        } else {
                            //  failed (display error )
                            showErrorDialog(response.message ?: "Unknown backend error")
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
                            Toast.makeText(this@ManagerStaffActivity, "Request approved", Toast.LENGTH_SHORT).show()
                            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
                        } else {
                            showErrorDialog("Failed to approve request")
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
                            Toast.makeText(this@ManagerStaffActivity, "Request rejected", Toast.LENGTH_SHORT).show()
                            leaveRequestViewModel.loadLeaveRequestForManager(selectedStatusFilter)
                        } else {
                            showErrorDialog("Failed to reject request")
                        }
                        leaveRequestViewModel.clearRejectState()
                    }
                }
            }
        }

        // 2. Trạng thái Loading
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                leaveRequestViewModel.isLoading.collectLatest { isLoading ->
                    binding.progressBar?.visibility = if (isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        // 3. Trạng thái Lỗi hiển thị Dialog
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
            binding.txtNoStaffData?.text = "No leave requests found."
            binding.txtNoStaffData?.visibility = View.VISIBLE
        } else {
            binding.rvStaffList.visibility = View.VISIBLE
            binding.txtNoStaffData?.visibility = View.GONE
            leaveRequestAdapter.updateData(requests)
        }
    }

    //Function to filter the RAM list based on a query (name, location, shift)
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
            // Lọc local cho Leave Requests Tab
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

     //Integrate a TextWatcher into the EditText to enable real-time keyword filtering.
    private fun setUpSearchListener() {
        binding.etSearchStaff?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applySearchFilter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    //Open the BottomSheet and receive the date range filter result from the Fragment.
    private fun showAdvancedFilterBottomSheet() {
        val filterFragment = StaffFilterBottomSheetFragment { startDate, endDate, status->
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
            .distinctBy { it.first } //get distinct Id

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
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        val input = android.widget.EditText(this)
        input.hint = "Enter reject reason"
        builder.setTitle("Reject Request")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString()
                if (reason.isNotBlank()) {
                    leaveRequestViewModel.rejectLeaveRequest(item.leaveRequestId, reason)
                } else {
                    Toast.makeText(this, "Reason cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showErrorDialog(backendErrorMessage: String) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        //inflate
        val dialogBinding = DialogErrorCustomBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()

        //delete background default
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.tvDialogMessage.text = backendErrorMessage
        dialogBinding.btnDialogOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }




}