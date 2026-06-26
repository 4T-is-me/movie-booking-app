package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.showDate
import com.example.movie_booking_app.databinding.ItemDateBinding

/**
 * Adapter mapping show session dates to the horizontal date picker recycler.
 */
class dateAdapter(
    private val onDateClicked: (showDate) -> Unit
) : ListAdapter<showDate, dateAdapter.DateViewHolder>(DateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DateViewHolder(private val binding: ItemDateBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(showDate: showDate) {
            binding.tvMonth.text = showDate.month
            binding.tvDay.text = showDate.day

            // Update selection states which trigger drawable selector XML configurations
            binding.root.isSelected = showDate.isSelected
            binding.tvDay.isSelected = showDate.isSelected
            binding.tvMonth.isSelected = showDate.isSelected

            binding.root.setOnClickListener {
                onDateClicked(showDate)
            }
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<showDate>() {
        override fun areItemsTheSame(oldItem: showDate, newItem: showDate) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: showDate, newItem: showDate) = oldItem == newItem
    }
}
