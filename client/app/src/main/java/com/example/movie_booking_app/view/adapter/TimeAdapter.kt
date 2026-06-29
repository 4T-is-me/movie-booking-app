package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.TimeSlot
import com.example.movie_booking_app.databinding.ItemTimeBinding

/**
 * Adapter mapping session times to the horizontal picker list.
 */
class TimeAdapter(
    private val onTimeClicked: (TimeSlot) -> Unit
) : ListAdapter<TimeSlot, TimeAdapter.TimeViewHolder>(TimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = ItemTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeViewHolder(private val binding: ItemTimeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timeSlot: TimeSlot) {
            binding.tvTime.text = formatTime(timeSlot.time)
            binding.tvTime.isSelected = timeSlot.isSelected

            binding.tvTime.setOnClickListener {
                onTimeClicked(timeSlot)
            }
        }
    }

    class TimeDiffCallback : DiffUtil.ItemCallback<TimeSlot>() {
        override fun areItemsTheSame(oldItem: TimeSlot, newItem: TimeSlot) = oldItem.time == newItem.time
        override fun areContentsTheSame(oldItem: TimeSlot, newItem: TimeSlot) = oldItem == newItem
    }
    private fun formatTime(dateTime: String): String {
        return try {
            //2026-07-25T09:00:00.000Z
            dateTime.substring(11, 16) // "09:00"
        } catch (e: Exception) {
            dateTime
        }
    }
}
