package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.toColorInt
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.databinding.ItemSeatBinding

/**
 * Recycler Adapter for grid list of seats.
 */
class SeatAdapter(
    private val onSeatClicked: (Seat) -> Unit,
) : ListAdapter<Seat, SeatAdapter.SeatViewHolder>(SeatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val binding = ItemSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SeatViewHolder(private val binding: ItemSeatBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(seat: Seat) {
            binding.tvSeat.text = seat.id

            // Apply proper color code according to Seat state
            // Use mutate() to ensure tint doesn't leak to other shared drawable instances
            val background = binding.tvSeat.background.mutate()
            when (seat.status) {
                SeatStatus.AVAILABLE -> {
                    background.setTint("#212121".toColorInt())
                    binding.tvSeat.setTextColor("#A0A0A0".toColorInt())
                }
                SeatStatus.RESERVED -> {
                    background.setTint("#261C02".toColorInt())
                    binding.tvSeat.setTextColor("#FFC107".toColorInt())
                }
                SeatStatus.SELECTED -> {
                    background.setTint("#FFC107".toColorInt())
                    binding.tvSeat.setTextColor("#000000".toColorInt())
                }
            }

            binding.root.setOnClickListener {
                if (seat.status != SeatStatus.RESERVED) {
                    onSeatClicked(seat)
                }
            }
        }
    }

    class SeatDiffCallback : DiffUtil.ItemCallback<Seat>() {
        override fun areItemsTheSame(oldItem: Seat, newItem: Seat) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Seat, newItem: Seat) = oldItem == newItem
    }
}
