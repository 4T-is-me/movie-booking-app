package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.databinding.ItemPurchaseHistoryBinding
import com.example.movie_booking_app.utils.Constants
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PurchaseHistoryAdapter(
    private var items: List<BookingHistoryItem> = emptyList(),
    private val onItemClick: ((BookingHistoryItem) -> Unit)? = null,
    private val onRateStaffClick: ((BookingHistoryItem) -> Unit)? = null,
    private val onRateMovieClick: ((BookingHistoryItem) -> Unit)? = null
) : RecyclerView.Adapter<PurchaseHistoryAdapter.ViewHolder>() {

    fun updateData(newItems: List<BookingHistoryItem>) {
        val diffCallback = PurchaseHistoryDiffCallback(this.items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPurchaseHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], onItemClick, onRateStaffClick, onRateMovieClick)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        private val binding: ItemPurchaseHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BookingHistoryItem,
            onItemClick: ((BookingHistoryItem) -> Unit)?,
            onRateStaffClick: ((BookingHistoryItem) -> Unit)?,
            onRateMovieClick: ((BookingHistoryItem) -> Unit)?
        ) {
            binding.tvMovieTitle.text = item.movie
            binding.tvBookingCode.text = "Code: ${item.bookingCode}"
            binding.tvShowtime.text = "${item.showDate} • ${item.showtime}"
            binding.tvCinemaRoom.text = "${item.cinemaName} • ${item.roomName}"
            
            val seatList = item.seats ?: emptyList()
            binding.tvSeats.text = "Seats: ${seatList.joinToString(", ").ifEmpty { "-" }}"
            binding.tvPaymentMethod.text = "Paid via ${item.paymentMethod ?: "-"}"

            // Format Vietnamese currency
            val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
                groupingSeparator = ','
            }
            val formatter = DecimalFormat("#,###", symbols)
            binding.tvFinalAmount.text = "${formatter.format(item.finalAmount)} ₫"

            // Load Poster Image
            val url = item.poster
            val finalUrl = when {
                url.isEmpty() -> ""
                url.startsWith("http") -> url
                else -> "${Constants.BASE_URL.removeSuffix("/")}$url"
            }

            Glide.with(itemView.context)
                .load(finalUrl.ifEmpty { null })
                .placeholder(R.drawable.movie) // fallback placeholder
                .error(R.drawable.movie)
                .into(binding.ivPoster)

            itemView.setOnClickListener {
                onItemClick?.invoke(item)
            }

            // Show Rate Staff button only if checked in and not yet rated
            if (item.checkedIn == true && item.isStaffRated == false) {
                binding.btnRateStaff.visibility = View.VISIBLE
                binding.btnRateStaff.setOnClickListener {
                    onRateStaffClick?.invoke(item)
                }
            } else {
                binding.btnRateStaff.visibility = View.GONE
            }

            // Show Rate Movie button only if not yet rated
            if (item.isMovieRated == false) {
                binding.btnRateMovie.visibility = View.VISIBLE
                binding.btnRateMovie.setOnClickListener {
                    onRateMovieClick?.invoke(item)
                }
            } else {
                binding.btnRateMovie.visibility = View.GONE
            }
        }
    }
}
class PurchaseHistoryDiffCallback(
    private val oldList: List<BookingHistoryItem>,
    private val newList: List<BookingHistoryItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].bookingCode == newList[newItemPosition].bookingCode
                && oldList[oldItemPosition].bookingId == newList[newItemPosition].bookingId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}