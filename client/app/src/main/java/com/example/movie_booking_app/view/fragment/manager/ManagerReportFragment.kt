package com.example.movie_booking_app.view.fragment.manager

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ReportChartItemResponse
import com.example.movie_booking_app.data.model.response.ReportData
import com.example.movie_booking_app.data.model.response.TopMoviesResponse
import com.example.movie_booking_app.databinding.FragmentManagerReportBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerTopMoviesAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerReportFragment : Fragment() {
    private val viewModel: ManagerViewModel by viewModels()
    private val topMovieManagerAdapter = ManagerTopMoviesAdapter()
    private lateinit var tabButtons: List<TextView>
    private var _binding: FragmentManagerReportBinding? = null
    private val binding get() = _binding!!

    private var currentReportType = "week"

    private val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sourceMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val targetWeekFormat = SimpleDateFormat("E", Locale("en", "US"))
    private val targetMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val targetYearFormat = SimpleDateFormat("MMM", Locale("en", "US"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide local header
        binding.headerLayoutReport.visibility = View.GONE
        binding.headerDivider.visibility = View.GONE

        tabButtons = listOf(
            binding.btnDayReport,
            binding.btnWeekReport,
            binding.btnMonthReport,
            binding.btnYearReport
        )
        setUpClickListeners()
        observeReportData()
        setActiveTab(binding.btnWeekReport, "week")
        setUpRecyclerView()
    }

    private fun setUpClickListeners() {
        binding.btnDayReport.setOnClickListener { setActiveTab(binding.btnDayReport, "day") }
        binding.btnWeekReport.setOnClickListener { setActiveTab(binding.btnWeekReport, "week") }
        binding.btnMonthReport.setOnClickListener { setActiveTab(binding.btnMonthReport, "month") }
        binding.btnYearReport.setOnClickListener { setActiveTab(binding.btnYearReport, "year") }
    }

    private fun setActiveTab(selectedTab: TextView, type: String) {
        currentReportType = type

        val context = requireContext()
        val inactiveTextColor = ContextCompat.getColor(context, R.color.text_secondary)
        val activeTextColor = ContextCompat.getColor(context, R.color.bg_primary)

        for (tab in tabButtons) {
            tab.setBackgroundResource(android.R.color.transparent)
            tab.setTextColor(inactiveTextColor)
            tab.setTypeface(null, Typeface.NORMAL)
        }

        selectedTab.setBackgroundResource(R.drawable.bg_tab_active_gold)
        selectedTab.setTextColor(activeTextColor)
        selectedTab.setTypeface(null, Typeface.BOLD)

        viewModel.loadReport(type)
    }

    private fun formatVND(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount)
    }

    private fun displayGrowth(textView: TextView, growthValue: Int) {
        val context = requireContext()
        val greenColor = ContextCompat.getColor(context, R.color.color_green_status)
        val redColor = ContextCompat.getColor(context, R.color.iconDeduct)
        if (growthValue >= 0) {
            textView.text = "+$growthValue%"
            textView.setTextColor(greenColor)
            textView.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.transparent)
        } else {
            textView.text = "$growthValue%"
            textView.setTextColor(redColor)
        }
    }

    private fun observeReportData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportData.collectLatest { response ->
                    response?.data?.let { reportData ->
                        setUpKPICards(reportData)
                        updateTrendChart(reportData.chartData)
                        setUpTopMovies(reportData.topMovies)
                    }
                }
            }
        }
    }

    private fun setUpKPICards(data: ReportData) {
        binding.txtTicketRevenue.text = formatVND(data.ticketRevenue)
        displayGrowth(binding.txtTicketRevenueGrowth, data.growth.ticket)

        binding.txtFbRevenue.text = formatVND(data.fbRevenue)
        displayGrowth(binding.txtFbRevenueGrowth, data.growth.fb)

        binding.txtTotalTickets.text = data.totalTickets.toString()
        displayGrowth(binding.txtTotalTicketsGrowth, data.growth.totalTickets)

        binding.txtAvgTicket.text = formatVND(data.avgTicket.toLong())
        displayGrowth(binding.txtAvgTicketGrowth, data.growth.avgTicket)
    }

    private fun updateTrendChart(chartList: List<ReportChartItemResponse>?) {
        val nonNullChartList = chartList.orEmpty()
        if (nonNullChartList.isNotEmpty()) {
            setUpReportTrendChart(nonNullChartList)
        } else {
            binding.lineChartReportTrend.clear()
        }
    }

    private fun formatChartLabel(label: String, reportType: String): String {
        return try {
            when (reportType) {
                "day" -> label
                "week" -> {
                    val date = sourceDateFormat.parse(label)
                    date?.let { targetWeekFormat.format(it) } ?: label
                }
                "month" -> {
                    val date = sourceDateFormat.parse(label)
                    date?.let { targetMonthFormat.format(it) } ?: label
                }
                "year" -> {
                    val date = sourceMonthFormat.parse(label)
                    date?.let { targetYearFormat.format(it) } ?: label
                }
                else -> label
            }
        } catch (e: Exception) {
            label
        }
    }

    private fun setUpReportTrendChart(chartList: List<ReportChartItemResponse>) {
        val ticketEntries = ArrayList<Entry>()
        val fbEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        val context = requireContext()
        val yellowColor = ContextCompat.getColor(context, R.color.color_gold)
        val yellowBgColor = ContextCompat.getColor(context, R.color.yellow_bg_chart)
        val labelFbColor = ContextCompat.getColor(context, R.color.blue_label_color)
        val columnColor = ContextCompat.getColor(context, R.color.darkGray_column_color)

        chartList.forEachIndexed { index, item ->
            ticketEntries.add(Entry(index.toFloat(), item.ticketRevenue.toFloat()))
            fbEntries.add(Entry(index.toFloat(), item.fbRevenue.toFloat()))
            labels.add(formatChartLabel(item.label, currentReportType))
        }

        val ticketDataSet = LineDataSet(ticketEntries, "Ticket Revenue").apply {
            color = yellowColor
            setCircleColor(yellowBgColor)
            lineWidth = 3f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val fbDataSet = LineDataSet(fbEntries, "F&B Revenue").apply {
            color = labelFbColor
            setCircleColor(labelFbColor)
            lineWidth = 3f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val lineData = LineData(ticketDataSet, fbDataSet)

        binding.lineChartReportTrend.apply {
            data = lineData
            description.isEnabled = false
            legend.textColor = Color.WHITE

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.WHITE
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = Color.WHITE
                setDrawGridLines(true)
                gridColor = columnColor
                isGranularityEnabled = false
            }

            axisRight.isEnabled = false
            animateX(800)
            invalidate()
        }
    }

    private fun setUpRecyclerView() {
        binding.rvTopMovies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topMovieManagerAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setUpTopMovies(topMovies: List<TopMoviesResponse>) {
        val movies = topMovies.orEmpty()
        if (movies.isEmpty()) {
            binding.rvTopMovies.visibility = View.GONE
            binding.txtNoMovieData.visibility = View.VISIBLE
        } else {
            binding.rvTopMovies.visibility = View.VISIBLE
            binding.txtNoMovieData.visibility = View.GONE
            topMovieManagerAdapter.submitList(movies.toMutableList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
