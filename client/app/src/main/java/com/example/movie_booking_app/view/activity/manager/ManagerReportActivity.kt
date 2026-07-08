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
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ReportChartItemResponse
import com.example.movie_booking_app.data.model.response.ReportData
import com.example.movie_booking_app.databinding.ActivityManagerReportBinding
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
    private lateinit var tabButtons: List<TextView>
    private lateinit var binding: ActivityManagerReportBinding

    // Biến lưu trạng thái tab hiện tại để format trục X chính xác
    private var currentReportType = "week"

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
    }

    private fun setUpClickListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }
        binding.btnDayReport.setOnClickListener { setActiveTab(binding.btnDayReport, "day") }
        binding.btnWeekReport.setOnClickListener { setActiveTab(binding.btnWeekReport, "week") }
        binding.btnMonthReport.setOnClickListener { setActiveTab(binding.btnMonthReport, "month") }
        binding.btnYearReport.setOnClickListener { setActiveTab(binding.btnYearReport, "year") }
    }

    private fun setActiveTab(selectedTab: TextView, type: String) {
        currentReportType = type // Cập nhật loại report đang chọn
        for (tab in tabButtons) {
            tab.setBackgroundColor(Color.TRANSPARENT)
            tab.setTextColor(Color.parseColor("#94A3B8"))
            tab.setTypeface(null, Typeface.NORMAL)
        }
        selectedTab.setBackgroundResource(R.drawable.bg_badge_gold)
        selectedTab.setTextColor(Color.parseColor("#FFFFFF"))
        selectedTab.setTypeface(null, Typeface.BOLD)
        viewModel.loadReport(type)
    }

    fun formatVND(amount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        return formatter.format(amount)
    }

    private fun displayGrowth(textView: TextView, growthValue: Int) {
        if (growthValue >= 0) {
            textView.text = "+$growthValue%"
            textView.setTextColor(Color.parseColor("#10B981"))
            textView.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.transparent)
        } else {
            textView.text = "$growthValue%"
            textView.setTextColor(Color.parseColor("#EF4444"))
        }
    }

    private fun observeReportData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportData.collectLatest { response ->
                    response?.data?.let { reportData ->
                        setUpKPICards(reportData)
                        updateTrendChart(reportData.chartData)
                        //Log.d("CheckShowtime", "Showtimes List: $reportData.chartData")
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
       // Log.d("CheckChart", "Dữ liệu dòng xu hướng: $nonNullChartList")
        if (nonNullChartList.isNotEmpty()) {
            setUpReportTrendChart(nonNullChartList)
        } else {
            binding.lineChartReportTrend.clear()
        }
    }

    private fun setUpReportTrendChart(chartList: List<ReportChartItemResponse>) {
        val ticketEntries = ArrayList<Entry>()
        val fbEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sourceMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val targetWeekFormat = SimpleDateFormat("E", Locale("en", "US"))
        val targetMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val targetYearFormat = SimpleDateFormat("MMM", Locale("en", "US"))

        chartList.forEachIndexed { index, item ->
            ticketEntries.add(Entry(index.toFloat(), item.ticketRevenue.toFloat()))
            fbEntries.add(Entry(index.toFloat(), item.fbRevenue.toFloat()))

            // Dựa hẳn vào currentReportType để định dạng chữ hiển thị dưới chân biểu đồ
            val cleanLabel = when {
                item.label.contains(":") -> item.label
                item.label.length == 10 -> {
                    try {
                        val date = sourceDateFormat.parse(item.label)
                        if (currentReportType == "week") {
                            date?.let { targetWeekFormat.format(it) } ?: item.label
                        } else {
                            date?.let { targetMonthFormat.format(it) } ?: item.label
                        }
                    } catch (e: Exception) { item.label }
                }
                item.label.length == 7 -> {
                    try {
                        val date = sourceMonthFormat.parse(item.label)
                        date?.let { targetYearFormat.format(it) } ?: item.label
                    } catch (e: Exception) { item.label }
                }
                else -> item.label
            }
            labels.add(cleanLabel)
        }

        val ticketDataSet = LineDataSet(ticketEntries, "Ticket Revenue").apply {
            color = Color.parseColor("#D4AF37")
            setCircleColor(Color.parseColor("#D4AF37"))
            lineWidth = 3f
            circleRadius = 3f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val fbDataSet = LineDataSet(fbEntries, "F&B Revenue").apply {
            color = Color.parseColor("#00BCD4")
            setCircleColor(Color.parseColor("#00BCD4"))
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
                gridColor = Color.parseColor("#2C2C2E")
                granularity = 2000000f
                isGranularityEnabled = true
            }

            axisRight.isEnabled = false
            animateX(800)
            invalidate()
        }
    }
}