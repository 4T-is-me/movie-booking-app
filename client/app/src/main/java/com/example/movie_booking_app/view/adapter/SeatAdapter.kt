package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Seat
import com.example.movie_booking_app.data.model.entity.SeatStatus
import com.example.movie_booking_app.data.model.entity.SeatType

class SeatAdapter(
    private val onSeatClick: (Seat) -> Unit
) : ListAdapter<Seat, RecyclerView.ViewHolder>(DiffCallback) {

    private val VIEW_TYPE_NORMAL = 0
    private val VIEW_TYPE_COUPLE = 1

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).type == SeatType.COUPLE) VIEW_TYPE_COUPLE else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_COUPLE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat_couple, parent, false)
            CoupleSeatViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_seat, parent, false)
            NormalSeatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val seat = getItem(position)
        if (holder is CoupleSeatViewHolder) holder.bind(seat)
        else if (holder is NormalSeatViewHolder) holder.bind(seat)
    }

    inner class NormalSeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSeat: TextView = view.findViewById(R.id.tvSeat)
        fun bind(seat: Seat) {
            tvSeat.text = seat.label
            tvSeat.setBackgroundResource(seat.getBackgroundResource())
            val isReserved = seat.displayStatus == SeatStatus.RESERVED || seat.displayStatus == SeatStatus.HOLDING
            itemView.isEnabled = !isReserved
            itemView.alpha = if (isReserved) 0.5f else 1.0f
            itemView.setOnClickListener { onSeatClick(seat) }
        }
    }

    inner class CoupleSeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSeatLeft: TextView = view.findViewById(R.id.tvSeatLeft)
        private val tvSeatRight: TextView = view.findViewById(R.id.tvSeatRight)
        
        fun bind(seat: Seat) {
            val background = seat.getBackgroundResource()
            
            // Sequential numbering: 1->H1,H2; 2->H3,H4...
            val leftNumber = seat.number * 2 - 1
            val rightNumber = seat.number * 2
            
            tvSeatLeft.text = "${seat.row}${leftNumber}"
            tvSeatRight.text = "${seat.row}${rightNumber}"
            
            tvSeatLeft.setBackgroundResource(background)
            tvSeatRight.setBackgroundResource(background)
            
            val isReserved = seat.displayStatus == SeatStatus.RESERVED || seat.displayStatus == SeatStatus.HOLDING
            itemView.isEnabled = !isReserved
            itemView.alpha = if (isReserved) 0.5f else 1.0f
            
            // Maintain a subtle "pushed back" gap (8dp) for the couple row
            val params = itemView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = (8 * itemView.context.resources.displayMetrics.density).toInt()
            itemView.layoutParams = params

            itemView.setOnClickListener { onSeatClick(seat) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Seat>() {
        override fun areItemsTheSame(oldItem: Seat, newItem: Seat): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Seat, newItem: Seat): Boolean = 
            oldItem == newItem && oldItem.displayStatus == newItem.displayStatus
    }
}
