package com.example.movie_booking_app.view.fragment.admin

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.admin.MovieRevenueItem
import com.example.movie_booking_app.databinding.FragmentAdminStatsBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.viewmodel.admin.AdminStatsUiState
import com.example.movie_booking_app.viewmodel.admin.AdminStatsViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

    companion object {
        private const val LINE_WIDTH = 1.8f
        private const val CIRCLE_RADIUS = 3.5f
        private const val CHART_ANIMATION_DURATION = 1000
        private const val GRADIENT_ALPHA = 40
    }

    private fun setupCharts() {
        // LineChart setup
        binding.lcDailyRevenue.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setDrawBorders(false)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(requireContext(), R.color.white)
                granularity = 1f
                setDrawGridLines(false)
                setAvoidFirstLastClipping(true)
            }

            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.white)
                labelCount = 5
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(requireContext(), R.color.divider_gray)
                gridLineWidth = 0.5f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when {
                            value >= 1000000 -> String.format(Locale.US, "%.1fM", value / 1000000)
                            value >= 1000 -> String.format(Locale.US, "%.0fK", value / 1000)
                            else -> value.toInt().toString()
                        }
                    }
                }
            }
            axisRight.isEnabled = false
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)
        }

        // HorizontalBarChart setup
        binding.bcMovieRevenue.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.white)
            xAxis.granularity = 1f
            xAxis.setDrawGridLines(false)
            
            axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.white)
            axisLeft.axisMinimum = 0f
            axisLeft.labelCount = 5
            axisLeft.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when {
                        value >= 1000000 -> String.format(Locale.US, "%.1fM", value / 1000000)
                        value >= 1000 -> String.format(Locale.US, "%.0fK", value / 1000)
                        else -> value.toInt().toString()
                    }
                }
            }
            axisRight.isEnabled = false
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)
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
                        binding.tvTotalUsers.text = it.totalMembers.toString()
                        binding.tvTodayTickets.text = it.totalTicketsSold.toString()
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
                Entry(item.month.toFloat(), item.totalRevenue.toFloat())
            }
        } else {
            state.dailyRevenue.mapIndexed { index, item ->
                Entry(index.toFloat(), item.totalRevenue.toFloat())
            }
        }

        val label = if (isMonthly) getString(R.string.chart_label_monthly_revenue)
                    else getString(R.string.chart_label_daily_revenue)

        val dataSet = LineDataSet(entries, label).apply {
            color = ContextCompat.getColor(requireContext(), R.color.yellow)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.yellow))
            lineWidth = 3f
            circleRadius = 5f
            setDrawCircleHole(true)
            circleHoleColor = ContextCompat.getColor(requireContext(), R.color.admin_bg)
            
            // Hiển thị giá trị trực tiếp cho chế độ Tháng để dễ theo dõi
            setDrawValues(isMonthly)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
            valueTextSize = 9f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 1000000) {
                        String.format(Locale.US, "%.1fM", value / 1000000)
                    } else if (value >= 1000) {
                        String.format(Locale.US, "%.0fK", value / 1000)
                    } else {
                        value.toInt().toString()
                    }
                }
            }

            mode = LineDataSet.Mode.CUBIC_BEZIER
            
            // Hiệu ứng Gradient
            setDrawFilled(true)
            val gradientDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    ContextCompat.getColor(requireContext(), R.color.yellow),
                    ContextCompat.getColor(requireContext(), android.R.color.transparent)
                )
            )
            fillDrawable = gradientDrawable
            fillAlpha = 80
        }

        binding.lcDailyRevenue.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (isMonthly) {
                        getString(R.string.month_prefix_short) + value.toInt().toString()
                    } else {
                        val index = value.toInt()
                        if (index >= 0 && index < state.dailyRevenue.size) {
                            val parts = state.dailyRevenue[index].date.split(Constants.Movie.SEPARATOR_DASH)
                            if (parts.size == 3) {
                                "${parts[2]}${Constants.Movie.SEPARATOR_SLASH}${parts[1]}"
                            } else {
                                state.dailyRevenue[index].date
                            }
                        } else ""
                    }
                }
            }
            labelCount = if (isMonthly) 12 else 6
        }

        binding.lcDailyRevenue.apply {
            data = LineData(dataSet)
            animateY(800)
            invalidate()
        }
    }

    private fun updateBarChart(data: List<MovieRevenueItem>) {
        // Reverse data for HorizontalBarChart so highest revenue is at the top (highest index)
        val sortedData = data.reversed()
        
        val entries = sortedData.mapIndexed { index, item ->
            BarEntry(index.toFloat(), item.totalRevenue.toFloat())
        }
        val dataSet = BarDataSet(entries, getString(R.string.chart_label_movie_revenue)).apply {
            color = ContextCompat.getColor(requireContext(), R.color.gold_selected)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
            valueTextSize = 10f
            // Format values inside the chart
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value >= 1000000) {
                        String.format(Locale.US, "%.1fM", value / 1000000)
                    } else if (value >= 1000) {
                        String.format(Locale.US, "%.0fK", value / 1000)
                    } else {
                        value.toInt().toString()
                    }
                }
            }
        }

        binding.bcMovieRevenue.apply {
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index >= 0 && index < sortedData.size) {
                        val title = sortedData[index].displayTitle
                        if (title.length > 12) title.substring(0, 10) + "..." else title
                    } else ""
                }
            }
            xAxis.granularity = 1f
            xAxis.labelCount = sortedData.size
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.divider_gray)
            
            // Adjust offsets to prevent label clipping
            setExtraOffsets(20f, 0f, 40f, 0f)
            animateY(1000)
        }

        binding.bcMovieRevenue.data = BarData(dataSet)
        binding.bcMovieRevenue.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
