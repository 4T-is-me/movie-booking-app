package com.example.movie_booking_app.view.adapter

import androidx.core.content.ContextCompat

import com.example.movie_booking_app.databinding.ItemSeatPreviewBinding
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.SeatPreview

class SeatPreviewAdapter(
    private val onSeatClick: (Int) -> Unit
) : ListAdapter<SeatPreview, SeatPreviewAdapter.SeatViewHolder>(SeatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatPreviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding, onSeatClick)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SeatViewHolder(private val binding: ItemSeatPreviewBinding, private val onSeatClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val tvSeatLabel: TextView = binding.tvSeatLabel

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onSeatClick(pos)
                }
            }
        }

        fun bind(seat: SeatPreview) {
            tvSeatLabel.text = seat.seatName
            
            // Generate standard rounded corner background shape with standard type color
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f // 4dp to pixels roughly
                val colorHex = when (seat.seatType.lowercase()) {
                    "vip" -> "#FF9800"    // Orange
                    "couple" -> "#E91E63" // Pink
                    else -> "#F5C542"     // Yellow (Standard)
                }
                setColor(Color.parseColor(colorHex))
            }
            tvSeatLabel.background = shape
            tvSeatLabel.setTextColor(ContextCompat.getColor(itemView.context, R.color.cinema_bg)) // High contrast dark text color
        }
    }

    class SeatDiffCallback : DiffUtil.ItemCallback<SeatPreview>() {
        override fun areItemsTheSame(oldItem: SeatPreview, newItem: SeatPreview): Boolean = oldItem.seatName == newItem.seatName
        override fun areContentsTheSame(oldItem: SeatPreview, newItem: SeatPreview): Boolean = oldItem == newItem
    }
}
