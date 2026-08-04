package com.example.movie_booking_app.view.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.SeatPreview
import com.example.movie_booking_app.databinding.ItemSeatPreviewBinding
import com.example.movie_booking_app.utils.Constants

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
                val colorRes = when (seat.seatType.lowercase()) {
                    Constants.SeatConstants.TYPE_VIP.lowercase() -> R.color.auto_color_FF9800
                    Constants.SeatConstants.TYPE_COUPLE.lowercase() -> R.color.auto_color_E91E63
                    else -> R.color.cinema_primary
                }
                setColor(ContextCompat.getColor(itemView.context, colorRes))
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
