package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.Showtime
import com.example.movie_booking_app.databinding.ItemTimeBinding

/**
 * Adapter mapping session times to the horizontal picker list.
 */
class TimeAdapter(
    private val onTimeClicked: (Showtime) -> Unit
) : ListAdapter<Showtime, TimeAdapter.TimeViewHolder>(TimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = ItemTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeViewHolder(private val binding: ItemTimeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(showTime: Showtime) {
            binding.tvTime.text = formatTime(showTime.startTime ?: "")
            binding.tvTime.isSelected = showTime.isSelected

            binding.tvTime.setOnClickListener {
                onTimeClicked(showTime)
            }
        }
    }

    class TimeDiffCallback : DiffUtil.ItemCallback<Showtime>() {
        override fun areItemsTheSame(oldItem: Showtime, newItem: Showtime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Showtime, newItem: Showtime) = oldItem == newItem
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
