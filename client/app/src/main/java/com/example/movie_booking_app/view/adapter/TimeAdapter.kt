package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.ShowTime
import com.example.movie_booking_app.databinding.ItemTimeBinding

/**
 * Adapter mapping session times to the horizontal picker list.
 */
class TimeAdapter(
    private val onTimeClicked: (ShowTime) -> Unit
) : ListAdapter<ShowTime, TimeAdapter.TimeViewHolder>(TimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val binding = ItemTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TimeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TimeViewHolder(private val binding: ItemTimeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(showTime: ShowTime) {
            binding.tvTime.text = showTime.time
            binding.tvTime.isSelected = showTime.isSelected

            binding.tvTime.setOnClickListener {
                onTimeClicked(showTime)
            }
        }
    }

    class TimeDiffCallback : DiffUtil.ItemCallback<ShowTime>() {
        override fun areItemsTheSame(oldItem: ShowTime, newItem: ShowTime) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ShowTime, newItem: ShowTime) = oldItem == newItem
    }
}
