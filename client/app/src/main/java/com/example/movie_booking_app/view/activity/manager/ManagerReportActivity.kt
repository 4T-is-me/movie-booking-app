package com.example.movie_booking_app.view.activity.manager

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ReportChartItemResponse
import com.example.movie_booking_app.data.model.response.ReportData
import com.example.movie_booking_app.data.model.response.TopMoviesResponse
import com.example.movie_booking_app.databinding.ActivityManagerReportBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerTopMoviesAdapter
import com.example.movie_booking_app.viewmodel.manager.ManagerViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class ManagerReportActivity : AppCompatActivity() {
    private val viewModel: ManagerViewModel by viewModels()
    private val topMovieManagerAdapter = ManagerTopMoviesAdapter()
    private lateinit var tabButtons: List<TextView>
    private lateinit var binding: ActivityManagerReportBinding


    private var currentReportType = "week"

    //  Formatter
    private val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val sourceMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val targetWeekFormat = SimpleDateFormat("E", Locale("en", "US")) // Mon, Tue...
    private val targetMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault()) // 09/07
    private val targetYearFormat = SimpleDateFormat("MMM", Locale("en", "US")) // Jan, Feb...

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        binding.ivBack.setOnClickListener { finish() }
        binding.btnDayReport.setOnClickListener { setActiveTab(binding.btnDayReport, "day") }
        binding.btnWeekReport.setOnClickListener { setActiveTab(binding.btnWeekReport, "week") }
        binding.btnMonthReport.setOnClickListener { setActiveTab(binding.btnMonthReport, "month") }
        binding.btnYearReport.setOnClickListener { setActiveTab(binding.btnYearReport, "year") }
    }

    private fun setActiveTab(selectedTab: TextView, type: String) {
        currentReportType = type

        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary) // Silver grey color for unselected tab
        val activeTextColor = ContextCompat.getColor(this, R.color.bg_primary) // Dark text to stand out on Gold background

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
        val greenColor = ContextCompat.getColor(this, R.color.color_green_status)
        val redColor = ContextCompat.getColor(this, R.color.iconDeduct)
        if (growthValue >= 0) {
            textView.text = "+$growthValue%"
            textView.setTextColor(greenColor)
            textView.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
        } else {
            textView.text = "$growthValue%"
            textView.setTextColor(redColor)
        }
    }

    private fun observeReportData() {
        lifecycleScope.launch {
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

    /**
     * assign Label, base on currentReportType
     */
    private fun formatChartLabel(label: String, reportType: String): String {
        return try {
            when (reportType) {
                "day" -> label // "00:00", "01:00"
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
            label // Fallback return old label
        }
    }

    private fun setUpReportTrendChart(chartList: List<ReportChartItemResponse>) {
        val ticketEntries = ArrayList<Entry>()
        val fbEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        //color
        val yellowColor = ContextCompat.getColor(this, R.color.color_gold)
        val yellowBgColor = ContextCompat.getColor(this, R.color.yellow_bg_chart)
        val labelFbColor = ContextCompat.getColor(this, R.color.blue_label_color)
        val columnColor = ContextCompat.getColor(this, R.color.darkGray_column_color)

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

    private fun setUpRecyclerView () {
        binding.rvTopMovies.apply {
            layoutManager = LinearLayoutManager(this@ManagerReportActivity)
            adapter = topMovieManagerAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setUpTopMovies (topMovies:  List<TopMoviesResponse>) {
        val movies = topMovies.orEmpty()
        if(movies.isEmpty()) {
            binding.rvTopMovies.visibility = android.view.View.GONE
            binding.txtNoMovieData.visibility = android.view.View.VISIBLE
        }else{
            binding.rvTopMovies.visibility = android.view.View.VISIBLE
            binding.txtNoMovieData.visibility = android.view.View.GONE
            topMovieManagerAdapter.submitList(movies.toMutableList())
        }
    }
}