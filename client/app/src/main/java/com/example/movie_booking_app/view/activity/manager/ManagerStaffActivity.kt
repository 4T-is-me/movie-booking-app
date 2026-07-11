package com.example.movie_booking_app.view.activity.manager

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.movie_booking_app.data.model.response.ShiftItemResponse
import com.example.movie_booking_app.databinding.ActivityManagerStaffBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerStaffShiftAdapter
import com.example.movie_booking_app.view.fragment.StaffFilterBottomSheetFragment
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerStaffActivity : AppCompatActivity() {

    private val viewModel: ManagerViewModel by viewModels()
    private lateinit var tabButtons: List<TextView>
    private lateinit var binding: ActivityManagerStaffBinding
    private var originalShiftList: List<ShiftItemResponse> = emptyList()

    private var currentTab = "staff_list"
    private val staffShiftAdapter = ManagerStaffShiftAdapter()
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
        setUpSearchListener()

        setActiveTab(binding.tabStaffList, "staff_list")
    }

    private fun setUpRecyclerView () {
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
            showAdvancedFilterBottomSheet()
        }
        binding.btnGenerateShift.setOnClickListener {
            Toast.makeText(this,"Feature Generate Shift coming soon", Toast.LENGTH_SHORT).show()
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
            viewModel.loadStaffShifts()
        }else {
            // Trường hợp sang tab Leave Requests (nếu có API riêng sau này)
            originalShiftList = emptyList()
            setUpShiftList(emptyList())
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

    //Function to filter the RAM list based on a query (name, location, shift)
    private fun applySearchFilter(query: String) {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.isEmpty()) {
            setUpShiftList(originalShiftList)
        }else {
            val filteredList = originalShiftList.filter { item ->
                val nameMatch = item.staffName?.lowercase()?.contains(trimmedQuery) == true
                val positionMatch = item.position?.lowercase()?.contains(trimmedQuery) == true
                val shiftMatch = item.shift?.lowercase()?.contains(trimmedQuery) == true

                nameMatch || positionMatch || shiftMatch
            }
            setUpShiftList(filteredList)
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
        val filterFragment = StaffFilterBottomSheetFragment { startDate, endDate->
            viewModel.loadStaffShifts(
                startDate = startDate,
                endDate = endDate
            )
        }
        filterFragment.show(supportFragmentManager, "StaffFilterBottomSheet")
    }

}