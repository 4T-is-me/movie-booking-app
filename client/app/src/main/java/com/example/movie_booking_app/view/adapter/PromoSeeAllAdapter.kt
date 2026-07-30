package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemPromoSeeAllBinding
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Promo
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import android.content.Intent
import com.example.movie_booking_app.view.activity.PromotionDetailActivity

class PromoSeeAllAdapter : ListAdapter<Promo, PromoSeeAllAdapter.PromoViewHolder>(PromoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoViewHolder {
        val binding = ItemPromoSeeAllBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PromoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PromoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PromoViewHolder(private val binding: ItemPromoSeeAllBinding) : RecyclerView.ViewHolder(binding.root) {
        private val imgPromoBanner: ImageView = binding.imgPromoBanner
        private val tvPromoTitle: TextView = binding.tvPromoTitle
        private val tvDiscountValue: TextView = binding.tvDiscountValue
        private val tvPromoDetail: TextView = binding.tvPromoDetail
        private val tvMinOrderValue: TextView = binding.tvMinOrderValue
        private val tvCouponCode: TextView = binding.tvCouponCode
        private val btnCopyCode: com.google.android.material.button.MaterialButton = binding.btnCopyCode
        private val tvPromoDateRange: TextView = binding.tvPromoDateRange

        fun bind(promo: Promo) {
            tvPromoTitle.text = promo.title
            tvPromoDetail.text = promo.detail ?: "No details available"
            tvCouponCode.text = promo.discountCode

            // Load banner image
            Glide.with(itemView.context)
                .load(promo.imageUrl)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPromoBanner)

            // Format discount value
            val discountVal = promo.discountValue ?: 0.0
            if (promo.discountType == "percentage") {
                tvDiscountValue.text = "-${discountVal.toInt()}%"
            } else {
                val formatter = NumberFormat.getInstance(Locale.GERMANY)
                tvDiscountValue.text = "-${formatter.format(discountVal.toInt())} VND"
            }

            // Format min spend
            val minSpend = promo.minOrderValue ?: 0.0
            if (minSpend > 0) {
                val formatter = NumberFormat.getInstance(Locale.GERMANY)
                tvMinOrderValue.text = "Min Spend: ${formatter.format(minSpend.toInt())} VND"
            } else {
                tvMinOrderValue.text = itemView.context.getString(R.string.auto_str_no_minimum_spend_required)
            }

            // Parse and format date range
            val formattedDate = formatIsoDate(promo.endDate)
            tvPromoDateRange.text = "Valid until: $formattedDate"

            // Click card to open details screen
            itemView.setOnClickListener {
                val context = itemView.context
                val intent = Intent(context, PromotionDetailActivity::class.java).apply {
                    putExtra("promo_id", promo.id)
                    putExtra("promo_title", promo.title)
                    putExtra("promo_image", promo.imageUrl)
                    putExtra("promo_code", promo.discountCode)
                    putExtra("promo_detail", promo.detail)
                    putExtra("promo_discount_type", promo.discountType)
                    putExtra("promo_discount_value", promo.discountValue ?: 0.0)
                    putExtra("promo_min_order", promo.minOrderValue ?: 0.0)
                    putExtra("promo_end_date", promo.endDate)
                }
                context.startActivity(intent)
            }

            // Copy button click listener with premium feedback animation
            btnCopyCode.setOnClickListener {
                val context = itemView.context
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("promo_code", promo.discountCode)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "Code ${promo.discountCode} copied!", Toast.LENGTH_SHORT).show()

                // Animate Copy Button transition state
                btnCopyCode.animate().alpha(0f).setDuration(120).withEndAction {
                    btnCopyCode.text = itemView.context.getString(R.string.auto_str_copied)
                    btnCopyCode.setIconResource(0) // Remove share icon
                    btnCopyCode.setTextColor(ContextCompat.getColor(context, R.color.admin_live_indicator)) // Change color to green
                    btnCopyCode.animate().alpha(1f).setDuration(120).withEndAction {
                        btnCopyCode.postDelayed({
                            btnCopyCode.animate().alpha(0f).setDuration(120).withEndAction {
                                btnCopyCode.text = itemView.context.getString(R.string.auto_str_copy)
                                btnCopyCode.setIconResource(android.R.drawable.ic_menu_share) // Restore icon
                                btnCopyCode.setTextColor(ContextCompat.getColor(context, R.color.yellow)) // Restore gold color
                                btnCopyCode.animate().alpha(1f).setDuration(120).start()
                            }.start()
                        }, 2000)
                    }.start()
                }.start()
            }
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

    class PromoDiffCallback : DiffUtil.ItemCallback<Promo>() {
        override fun areItemsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Promo, newItem: Promo): Boolean = oldItem == newItem
    }
}
