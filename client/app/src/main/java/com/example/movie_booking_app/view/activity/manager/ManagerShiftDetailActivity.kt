package com.example.movie_booking_app.view.activity.manager

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.response.ShiftDetailData
import com.example.movie_booking_app.databinding.ActivityManagerShiftDetailBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.NameFormatter
import com.example.movie_booking_app.view.adapter.manager.WeekScheduleAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerShiftDetailActivity : AppCompatActivity() {

    private val viewModel: ManagerViewModel by viewModels()
    private lateinit var binding: ActivityManagerShiftDetailBinding
    private val weekScheduleAdapter by lazy { WeekScheduleAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerShiftDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val shiftId = intent.getStringExtra("shiftId") ?: return
        setupRecyclerView()
        setupObserve()
        setupListeners()

        viewModel.loadShiftDetail(shiftId)
    }

    private fun setupRecyclerView() {
        binding.rvWeekSchedule.apply {
            layoutManager = LinearLayoutManager(this@ManagerShiftDetailActivity)
            adapter = weekScheduleAdapter
            isNestedScrollingEnabled = false
        }
    }
    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setupObserve() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.shiftDetailData.collect { response ->
                    response?.data?.let { detailData ->
                        bindDataToUI(detailData)
                    }
                }
            }
        }
    }

    private fun bindDataToUI(data: ShiftDetailData) {
        val staff = data.staffInfo

        // Avatar Text
        binding.tvAvatarText.text = NameFormatter.getAvatarInitials(staff.fullName)

        // Profile Info
        binding.tvStaffName.text = staff.fullName
        binding.tvPosition.text = "${staff.position}"
        binding.tvStatusBadge.text = staff.status


        // Current Shift
        when(data.shift.lowercase()) {
            "morning" -> binding.tvShiftTime.text = "${data.shift} (${Constants.MORNING})"
            "afternoon" -> binding.tvShiftTime.text = "${data.shift} (${Constants.AFTERNOON})"
            "evening" -> binding.tvShiftTime.text = "${data.shift} (${Constants.EVENING})"
            else -> binding.tvShiftTime.text = "${data.shift} (${Constants.NIGHT})"
        }

        // Performance Metrics
        val attendance = staff.attendanceRate.toInt()
        binding.tvAttendanceRate.text = "$attendance%"
        binding.pbAttendance.progress = attendance

        // Week Schedule
        weekScheduleAdapter.submitList(data.weekSchedule)
    }
}