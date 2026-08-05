package com.example.movie_booking_app.view.fragment.admin

import android.content.Context
import com.example.movie_booking_app.databinding.LayoutChartMarkerBinding
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

    private val binding = LayoutChartMarkerBinding.bind(getChildAt(0))
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    override fun refreshContent(e: Entry, highlight: Highlight) {
        binding.tvMarkerDate.text = xAxisFormatter.getFormattedValue(e.x)
        binding.tvMarkerValue.text = currencyFormat.format(e.y)
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat() - 20f)
    }
}
