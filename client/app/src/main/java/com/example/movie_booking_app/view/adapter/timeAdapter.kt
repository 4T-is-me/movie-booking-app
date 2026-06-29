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
import com.example.movie_booking_app.data.model.entity.ShowTime

class timeAdapter(
    private val onTimeClick: (ShowTime) -> Unit
) : ListAdapter<ShowTime, timeAdapter.TimeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_show_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(time: ShowTime) {
            tvTime.text = time.time
            
            if (time.isSelected) {
                tvTime.setBackgroundResource(R.drawable.bg_time_selected)
                tvTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_primary))
            } else {
                tvTime.setBackgroundResource(R.drawable.bg_time_unselected)
                tvTime.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }

            itemView.setOnClickListener { onTimeClick(time) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ShowTime>() {
        override fun areItemsTheSame(oldItem: ShowTime, newItem: ShowTime): Boolean = oldItem.time == newItem.time
        override fun areContentsTheSame(oldItem: ShowTime, newItem: ShowTime): Boolean = oldItem == newItem
    }
}
