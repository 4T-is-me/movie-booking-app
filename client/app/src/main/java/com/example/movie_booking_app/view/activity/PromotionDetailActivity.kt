package com.example.movie_booking_app.view.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityPromotionDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PromotionDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPromotionDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPromotionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind incoming promo data
        val title = intent.getStringExtra("promo_title") ?: getString(R.string.promo_details_title)
        val imageUrl = intent.getStringExtra("promo_image") ?: ""
        val code = intent.getStringExtra("promo_code") ?: ""
        val detail = intent.getStringExtra("promo_detail") ?: getString(R.string.promo_no_description)
        val discountType = intent.getStringExtra("promo_discount_type") ?: ""
        val discountValue = intent.extras?.getDouble("promo_discount_value", 0.0) ?: 0.0
        val minOrderValue = intent.extras?.getDouble("promo_min_order", 0.0) ?: 0.0
        val endDate = intent.getStringExtra("promo_end_date") ?: ""

        // Set UI values
        binding.tvPromoTitle.text = title
        binding.tvPromoDesc.text = detail
        binding.tvPromoCode.text = code

        // Load banner image
        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.color.card_dark)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(binding.ivPromoBanner)

        // Format discount value
        if (discountType == "percentage") {
            binding.tvDiscountValue.text = "-${discountValue.toInt()}%"
        } else {
            val formatter = NumberFormat.getInstance(Locale.GERMANY)
            binding.tvDiscountValue.text = getString(R.string.booking_price_vnd_negative, formatter.format(discountValue.toInt()))
        }

        // Format min spend
        if (minOrderValue > 0) {
            val formatter = NumberFormat.getInstance(Locale.GERMANY)
            binding.tvMinOrderValue.text = getString(R.string.promo_min_spending, formatter.format(minOrderValue.toInt()))
        } else {
            binding.tvMinOrderValue.text = getString(R.string.auto_str_no_minimum_spending_required)
        }

        // Parse and format date range
        val formattedDate = formatIsoDate(endDate)
        binding.tvPromoDateRange.text = getString(R.string.promo_expiry_date, formattedDate)

        // Set listeners
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Copy button click listener with premium visual feedback
        binding.btnCopyCode.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("promo_code", code)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, getString(R.string.promo_code_copied, code), Toast.LENGTH_SHORT).show()

            // Animate Copy Button transition state
            binding.btnCopyCode.animate().alpha(0f).setDuration(120).withEndAction {
                binding.btnCopyCode.text = getString(R.string.auto_str_copied)
                binding.btnCopyCode.setIconResource(0) // Remove share icon
                binding.btnCopyCode.setTextColor(ContextCompat.getColor(this, R.color.admin_live_indicator)) // Change color to green
                binding.btnCopyCode.animate().alpha(1f).setDuration(120).withEndAction {
                    binding.btnCopyCode.postDelayed({
                        binding.btnCopyCode.animate().alpha(0f).setDuration(120).withEndAction {
                            binding.btnCopyCode.text = getString(R.string.auto_str_copy)
                            binding.btnCopyCode.setIconResource(android.R.drawable.ic_menu_share) // Restore icon
                            binding.btnCopyCode.setTextColor(ContextCompat.getColor(this, R.color.yellow)) // Restore gold color
                            binding.btnCopyCode.animate().alpha(1f).setDuration(120).start()
                        }.start()
                    }, 2000)
                }.start()
            }.start()
        }

        // Book now button -> goes to MainActivity home page
        binding.btnBookNow.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
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
