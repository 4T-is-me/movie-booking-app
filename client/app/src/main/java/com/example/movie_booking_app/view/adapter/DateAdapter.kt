package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.ShowDate

class DateAdapter(
    private val onDateClick: (ShowDate) -> Unit
) : ListAdapter<ShowDate, DateAdapter.DateViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_show_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMonth: TextView = view.findViewById(R.id.tvMonth)
        private val tvDay: TextView = view.findViewById(R.id.tvDay)

        fun bind(date: ShowDate) {
            tvMonth.text = date.month
            tvDay.text = date.day
            
            if (date.isSelected) {
                tvMonth.setTextColor(ContextCompat.getColor(itemView.context, R.color.black))
                tvDay.setBackgroundResource(R.drawable.bg_date_selected)
                tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_primary))
                itemView.setBackgroundResource(R.drawable.bg_date_selected) // Pill background
            } else {
                tvMonth.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                tvDay.setBackgroundResource(R.drawable.bg_date_unselected)
                tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                itemView.setBackgroundResource(0)
            }

            itemView.setOnClickListener { onDateClick(date) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ShowDate>() {
        override fun areItemsTheSame(oldItem: ShowDate, newItem: ShowDate): Boolean = oldItem.day == newItem.day
        override fun areContentsTheSame(oldItem: ShowDate, newItem: ShowDate): Boolean = oldItem == newItem
    }
}
