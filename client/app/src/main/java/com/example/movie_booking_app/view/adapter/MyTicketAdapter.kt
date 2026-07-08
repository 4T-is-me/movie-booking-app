package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
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
    private var items: List<BookingHistoryItem> = emptyList(),
    private val onItemClick: (BookingHistoryItem) -> Unit
) : RecyclerView.Adapter<MyTicketAdapter.ViewHolder>() {

    fun updateData(newItems: List<BookingHistoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemMyTicketBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookingHistoryItem, onItemClick: (BookingHistoryItem) -> Unit) {
            binding.tvMovieTitle.text = item.movie
            binding.tvShowtime.text = "${item.showDate} • ${item.showtime}"
            binding.tvCinemaRoom.text = "${item.cinemaName} • ${item.roomName}"
            
            val seatList = item.seats ?: emptyList()
            binding.tvSeats.text = "Seats: ${seatList.joinToString(", ").ifEmpty { "-" }}"
            binding.tvBookingCode.text = "Booking ID: ${item.bookingCode}"

            // Format Vietnamese currency
            val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
                groupingSeparator = ','
            }
            val formatter = DecimalFormat("#,###", symbols)
            binding.tvTotalPaid.text = "Total Paid: ${formatter.format(item.finalAmount)} ₫"

            // Decode QR Code
            val qr = item.qrCodeData
            if (!qr.isNullOrEmpty()) {
                try {
                    val cleanBase64 = qr.substringAfter("base64,")
                    val imageBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                    val decodedImage = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.ivQrCode.setImageBitmap(decodedImage)
                    binding.ivQrCode.visibility = android.view.View.VISIBLE
                } catch (e: Exception) {
                    binding.ivQrCode.visibility = android.view.View.GONE
                }
            } else {
                binding.ivQrCode.visibility = android.view.View.GONE
            }

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
                onItemClick(item)
            }
        }
    }
}
