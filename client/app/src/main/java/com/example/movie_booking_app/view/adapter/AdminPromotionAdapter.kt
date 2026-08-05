package com.example.movie_booking_app.view.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.example.movie_booking_app.databinding.ItemAdminPromotionBinding
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
        val binding = ItemAdminPromotionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminPromotionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminPromotionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminPromotionViewHolder(private val binding: ItemAdminPromotionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val tvPromoTitle: TextView = binding.tvPromoTitle
        private val tvPromoStatus: TextView = binding.tvPromoStatus
        private val tvPromoDetail: TextView = binding.tvPromoDetail
        private val tvCouponCode: TextView = binding.tvCouponCode
        private val tvDiscountValue: TextView = binding.tvDiscountValue
        private val tvPromoDateRange: TextView = binding.tvPromoDateRange
        private val btnEditPromo: ImageButton = binding.btnEditPromo
        private val btnDeletePromo: ImageButton = binding.btnDeletePromo

        fun bind(promotion: AdminPromotion) {
            btnEditPromo.setOnClickListener { onEditClick(promotion) }
            btnDeletePromo.setOnClickListener { onDeleteClick(promotion) }

            tvPromoTitle.text = promotion.title
            tvPromoDetail.text = promotion.detail ?: itemView.context.getString(R.string.admin_promo_no_details)
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

            val isActive = promotion.isDeleted != true && (start == null || !now.before(start)) && (end == null || !now.after(end))
            val statusText: String
            val statusColor: Int

            if (isActive) {
                statusText = "ACTIVE"
                statusColor = ContextCompat.getColor(itemView.context, R.color.admin_live_indicator)
            } else {
                statusText = "INACTIVE"
                statusColor = ContextCompat.getColor(itemView.context, R.color.deduct)
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
