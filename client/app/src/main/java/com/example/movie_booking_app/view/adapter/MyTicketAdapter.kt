package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.databinding.ItemMyTicketBinding
import com.example.movie_booking_app.utils.Constants
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class MyTicketAdapter(
    private val onViewTicketClick: (BookingHistoryItem) -> Unit,
    private val onViewQrClick: (BookingHistoryItem) -> Unit,
    private val onRateStaffClick: ((BookingHistoryItem) -> Unit)? = null
) : ListAdapter<BookingHistoryItem, MyTicketAdapter.ViewHolder>(MyTicketDiffCallback()) {

    fun updateData(newItems: List<BookingHistoryItem>) {
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onViewTicketClick, onViewQrClick, onRateStaffClick)
    }

    class ViewHolder(
        private val binding: ItemMyTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BookingHistoryItem,
            onViewTicketClick: (BookingHistoryItem) -> Unit,
            onViewQrClick: (BookingHistoryItem) -> Unit,
            onRateStaffClick: ((BookingHistoryItem) -> Unit)?
        ) {
            binding.tvMovieTitle.text = item.movie
            binding.tvShowDate.text = item.showDate
            binding.tvShowTime.text = item.showtime
            binding.tvCinemaName.text = item.cinemaName
            binding.tvRoomName.text = item.roomName
            
            val seatList = item.seats ?: emptyList()
            binding.tvSeats.text = "Seats: ${seatList.joinToString(", ").ifEmpty { "-" }}"
            binding.tvBookingCode.text = "Booking ID: ${item.bookingCode}"

            // Format Vietnamese currency
            val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
                groupingSeparator = ','
            }
            val formatter = DecimalFormat("#,###", symbols)
            binding.tvTotalPaid.text = "Total Paid: ${formatter.format(item.finalAmount)} ₫"

            // Load Poster Image
            val url = item.poster
            val finalUrl = when {
                url.isEmpty() -> ""
                url.startsWith("http") -> url
                else -> "${Constants.BASE_URL.removeSuffix("/")}$url"
            }

            Glide.with(itemView.context)
                .load(finalUrl.ifEmpty { null })
                .placeholder(R.drawable.movie)
                .error(R.drawable.movie)
                .into(binding.ivPoster)

            itemView.setOnClickListener {
                onViewTicketClick(item)
            }
            binding.btnViewTicket.setOnClickListener {
                onViewTicketClick(item)
            }
            binding.btnViewQr.setOnClickListener {
                onViewQrClick(item)
            }

            // Show Rate Staff button only if checked in and not yet rated
            if (item.checkedIn == true && item.isStaffRated == false) {
                binding.btnRateStaff.visibility = android.view.View.VISIBLE
                binding.btnRateStaff.setOnClickListener {
                    onRateStaffClick?.invoke(item)
                }
            } else {
                binding.btnRateStaff.visibility = android.view.View.GONE
            }
        }
    }

    class MyTicketDiffCallback : DiffUtil.ItemCallback<BookingHistoryItem>() {
        override fun areItemsTheSame(oldItem: BookingHistoryItem, newItem: BookingHistoryItem): Boolean = oldItem.bookingId == newItem.bookingId
        override fun areContentsTheSame(oldItem: BookingHistoryItem, newItem: BookingHistoryItem): Boolean = oldItem == newItem
    }
}
