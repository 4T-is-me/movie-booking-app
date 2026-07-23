package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemShowDateBinding
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
        val binding = ItemShowDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateViewHolder(private val binding: ItemShowDateBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvMonth: TextView = binding.tvMonth
        private val tvDay: TextView = binding.tvDay

        fun bind(date: ShowDate) {
            tvMonth.text = date.dayOfWeek
            tvDay.text = date.day
            
            if (date.isSelected) {
                tvMonth.setTextColor(ContextCompat.getColor(itemView.context, R.color.bg_primary))
                tvDay.setBackgroundResource(R.drawable.bg_date_selected)
                tvDay.setTextColor(ContextCompat.getColor(itemView.context, R.color.bg_primary))
                itemView.setBackgroundResource(R.drawable.bg_date_selected_pill)
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
        override fun areItemsTheSame(oldItem: ShowDate, newItem: ShowDate): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShowDate, newItem: ShowDate): Boolean = oldItem == newItem
    }
}
