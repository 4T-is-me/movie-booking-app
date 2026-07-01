package com.example.movie_booking_app.view.adapter.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import java.text.NumberFormat
import java.util.Locale

class StatsTableAdapter : RecyclerView.Adapter<StatsTableAdapter.StatsViewHolder>() {

    private var items: List<Pair<String, Double>> = emptyList()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    fun submitList(newItems: List<Pair<String, Double>>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return StatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatsViewHolder, position: Int) {
        val (label, value) = items[position]
        holder.tvLabel.text = label
        holder.tvValue.text = currencyFormat.format(value)
        
        // Custom styling for dark background
        holder.tvLabel.setTextColor(android.graphics.Color.WHITE)
        holder.tvValue.setTextColor(android.graphics.Color.parseColor("#F5C518"))
    }

    override fun getItemCount(): Int = items.size

    class StatsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(android.R.id.text1)
        val tvValue: TextView = view.findViewById(android.R.id.text2)
    }
}
