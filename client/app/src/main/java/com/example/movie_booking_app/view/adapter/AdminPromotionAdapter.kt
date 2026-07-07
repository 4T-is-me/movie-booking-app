package com.example.movie_booking_app.view.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.AdminPromotion
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AdminPromotionAdapter(
    private val onEditClick: (AdminPromotion) -> Unit,
    private val onDeleteClick: (AdminPromotion) -> Unit
) : ListAdapter<AdminPromotion, AdminPromotionAdapter.AdminPromotionViewHolder>(AdminPromotionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminPromotionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_promotion, parent, false)
        return AdminPromotionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminPromotionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminPromotionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvPromoTitle: TextView = view.findViewById(R.id.tvPromoTitle)
        private val tvPromoStatus: TextView = view.findViewById(R.id.tvPromoStatus)
        private val tvPromoDetail: TextView = view.findViewById(R.id.tvPromoDetail)
        private val tvCouponCode: TextView = view.findViewById(R.id.tvCouponCode)
        private val tvDiscountValue: TextView = view.findViewById(R.id.tvDiscountValue)
        private val tvPromoDateRange: TextView = view.findViewById(R.id.tvPromoDateRange)
        private val btnEditPromo: android.widget.ImageButton = view.findViewById(R.id.btnEditPromo)
        private val btnDeletePromo: android.widget.ImageButton = view.findViewById(R.id.btnDeletePromo)

        fun bind(promotion: AdminPromotion) {
            btnEditPromo.setOnClickListener { onEditClick(promotion) }
            btnDeletePromo.setOnClickListener { onDeleteClick(promotion) }

            tvPromoTitle.text = promotion.title
            tvPromoDetail.text = promotion.detail ?: "Không có chi tiết"
            tvCouponCode.text = promotion.couponCode ?: ""

            // Format discount value
            val type = promotion.discountType ?: "percentage"
            val value = promotion.discountValue ?: 0.0
            if (type == "percentage") {
                tvDiscountValue.text = "-${value.toInt()}%"
            } else {
                val formatter = NumberFormat.getInstance(Locale.GERMANY)
                tvDiscountValue.text = "-${formatter.format(value.toInt())} VND"
            }

            // Parse dates and format them
            val startDateStr = formatIsoDate(promotion.startDate)
            val endDateStr = formatIsoDate(promotion.endDate)
            tvPromoDateRange.text = "$startDateStr - $endDateStr"

            // Set status badge color dynamically
            val now = Date()
            val start = parseIsoDate(promotion.startDate)
            val end = parseIsoDate(promotion.endDate)

            val statusText: String
            val statusColor: Int

            if (start != null && now.before(start)) {
                statusText = "SẮP DIỄN RA"
                statusColor = ContextCompat.getColor(itemView.context, R.color.yellow)
            } else if (end != null && now.after(end)) {
                statusText = "HẾT HẠN"
                statusColor = ContextCompat.getColor(itemView.context, R.color.deduct)
            } else {
                statusText = "ĐANG ÁP DỤNG"
                statusColor = ContextCompat.getColor(itemView.context, R.color.admin_live_indicator)
            }

            tvPromoStatus.text = statusText
            tvPromoStatus.backgroundTintList = ColorStateList.valueOf(statusColor)
        }

        private fun parseIsoDate(dateStr: String?): Date? {
            if (dateStr.isNullOrEmpty()) return null
            val formats = arrayOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd"
            )
            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.US)
                    if (format.endsWith("'Z'")) {
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                    }
                    return sdf.parse(dateStr)
                } catch (e: Exception) {
                    // Try next format
                }
            }
            return null
        }

        private fun formatIsoDate(dateStr: String?): String {
            val date = parseIsoDate(dateStr) ?: return dateStr ?: ""
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            return outputFormat.format(date)
        }
    }

    class AdminPromotionDiffCallback : DiffUtil.ItemCallback<AdminPromotion>() {
        override fun areItemsTheSame(oldItem: AdminPromotion, newItem: AdminPromotion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AdminPromotion, newItem: AdminPromotion): Boolean {
            return oldItem == newItem
        }
    }
}
