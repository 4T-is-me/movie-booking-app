package com.example.movie_booking_app.view.fragment.admin

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.admin.DailyRevenueItem
import com.example.movie_booking_app.data.model.admin.MonthlyRevenueItem
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.databinding.FragmentAdminStatsBinding
import com.example.movie_booking_app.viewmodel.admin.AdminStatsViewModel
import com.example.movie_booking_app.viewmodel.admin.AdminStatsUiState
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class AdminStatsFragment : Fragment() {

    private var _binding: FragmentAdminStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminStatsViewModel by viewModels()

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupSwipeRefresh()
        setupToggleGroup()
        observeViewModel()
    }

    private fun setupToggleGroup() {
        binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                updateLineChart(viewModel.uiState.value)
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadDashboardData()
        }
    }

    private fun setupCharts() {
        // LineChart setup
        binding.lcDailyRevenue.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            legend.textColor = Color.WHITE
            
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    // This assumes we pass an index or timestamp. 
                    // We'll handle mapping in updateLineChart
                    return value.toString()
                }
            }

            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return currencyFormat.format(value.toDouble())
                }
            }
        }

        // HorizontalBarChart setup
        binding.bcMovieRevenue.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = Color.WHITE
            axisLeft.textColor = Color.WHITE
            axisRight.isEnabled = false
            legend.textColor = Color.WHITE

            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return currencyFormat.format(value.toDouble())
                }
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                if (state.isLoading) {
                    if (!binding.swipeRefreshLayout.isRefreshing) {
                        binding.shimmerView.visibility = View.VISIBLE
                        binding.shimmerView.startShimmer()
                        binding.contentView.visibility = View.GONE
                    }
                } else {
                    binding.swipeRefreshLayout.isRefreshing = false
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    binding.contentView.visibility = View.VISIBLE

                    state.overview?.let {
                        binding.tvMonthlyRevenue.text = currencyFormat.format(it.totalRevenue)
                        binding.tvTotalUsers.text = it.totalUsers.toString()
                        binding.tvTodayTickets.text = it.totalBookings.toString()
                        binding.tvActiveMovies.text = it.totalMovies.toString()
                    }
                    
                    updateLineChart(state)
                    updateBarChart(state.movieRevenue)
                }
            }
        }
    }

    private fun updateLineChart(state: AdminStatsUiState) {
        val isMonthly = binding.toggleGroup.checkedButtonId == R.id.btnMonthly
        
        val entries = if (isMonthly) {
            state.monthlyRevenue.map { item ->
                Entry(item.month.toFloat(), item.revenue.toFloat())
            }
        } else {
            state.dailyRevenue.mapIndexed { index, item ->
                Entry(index.toFloat(), item.revenue.toFloat())
            }
        }

        val dataSet = LineDataSet(entries, if (isMonthly) "Doanh thu tháng" else "Doanh thu ngày").apply {
            color = Color.YELLOW
            setCircleColor(Color.YELLOW)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextColor = Color.WHITE
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.YELLOW
            fillAlpha = 50
        }

        binding.lcDailyRevenue.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (isMonthly) {
                    "Tháng ${value.toInt()}"
                } else {
                    val index = value.toInt()
                    if (index >= 0 && index < state.dailyRevenue.size) {
                        val parts = state.dailyRevenue[index].date.split("-")
                        if (parts.size == 3) "${parts[2]}/${parts[1]}" else state.dailyRevenue[index].date
                    } else ""
                }
            }
        }

        binding.lcDailyRevenue.data = LineData(dataSet)
        binding.lcDailyRevenue.invalidate()
    }

    private fun updateBarChart(data: List<MovieRevenueItem>) {
        val entries = data.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.revenue.toFloat())
        }

        val dataSet = BarDataSet(entries, "Doanh thu phim").apply {
            color = Color.parseColor("#F5C518")
            valueTextColor = Color.WHITE
        }

        binding.bcMovieRevenue.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].movieTitle else ""
            }
        }

        binding.bcMovieRevenue.data = BarData(dataSet)
        binding.bcMovieRevenue.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
