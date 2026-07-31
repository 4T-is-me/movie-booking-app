package com.example.movie_booking_app.view.fragment.admin

import android.content.Context
import android.widget.TextView
import com.example.movie_booking_app.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.NumberFormat
import java.util.Locale

class RevenueMarkerView(
    context: Context,
    layoutResource: Int,
    private val xAxisFormatter: ValueFormatter
) : MarkerView(context, layoutResource) {

    private val tvDate: TextView = findViewById(R.id.tvMarkerDate)
    private val tvValue: TextView = findViewById(R.id.tvMarkerValue)
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun refreshContent(e: Entry, highlight: Highlight) {
        tvDate.text = xAxisFormatter.getFormattedValue(e.x)
        tvValue.text = currencyFormat.format(e.y)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 20f)
    }
}
