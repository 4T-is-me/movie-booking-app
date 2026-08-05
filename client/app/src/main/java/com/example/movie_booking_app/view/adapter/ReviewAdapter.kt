package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.data.model.response.ReviewItem
import com.example.movie_booking_app.databinding.ItemReviewBinding
import com.example.movie_booking_app.utils.Constants
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.content.Context
import com.example.movie_booking_app.R

class ReviewAdapter(
    @Suppress("UNUSED_PARAMETER") items: List<ReviewItem> = emptyList()
) : ListAdapter<ReviewItem, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    class ReviewViewHolder(val binding: ItemReviewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = getItem(position)
        val binding = holder.binding
        
        // Get display name prioritizing fullName -> username -> "Anonymous"
        val displayName = review.accountId?.memberInfo?.fullName
            ?: review.accountId?.username
            ?: "Anonymous"

        // Avatar: get initial letter
        val initial = if (displayName.isNotEmpty()) displayName.substring(0, 1).uppercase() else "A"
        binding.tvAvatarText.text = initial
        binding.tvReviewerName.text = displayName

        // Load avatar image if exists
        val avatarUrl = review.accountId?.memberInfo?.image
        if (!avatarUrl.isNullOrEmpty()) {
            val finalUrl = when {
                avatarUrl.startsWith("http") -> avatarUrl
                else -> "${Constants.BASE_URL.removeSuffix("/")}/${avatarUrl.removePrefix("/")}"
            }
            binding.ivAvatarImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(finalUrl)
                .into(binding.ivAvatarImage)
        } else {
            binding.ivAvatarImage.visibility = View.GONE
        }

        // Rating
        binding.tvReviewRating.text = java.lang.String.format(Locale.getDefault(), "%.1f", review.rating ?: 0.0)

        // Comment
        if (!review.comment.isNullOrEmpty()) {
            binding.tvReviewComment.text = review.comment
            binding.tvReviewComment.visibility = View.VISIBLE
        } else {
            binding.tvReviewComment.visibility = View.GONE
        }

        // Creation date
        binding.tvReviewDate.text = formatRelativeDate(holder.itemView.context, review.createdAt)
    }

    private fun formatRelativeDate(context: Context, isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val nowMillis = System.currentTimeMillis()
            val diffMillis = nowMillis - instant.toEpochMilli()
            val diffMinutes = diffMillis / 60_000
            val diffHours = diffMinutes / 60
            val diffDays = diffHours / 24
            when {
                diffMinutes < 1 -> context.getString(R.string.just_now)
                diffMinutes < 60 -> context.getString(R.string.minutes_ago_format, diffMinutes)
                diffHours < 24 -> context.getString(R.string.hours_ago_format, diffHours)
                diffDays < 30 -> context.getString(R.string.days_ago_format, diffDays)
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        .withZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                    formatter.format(instant)
                }
            }
        } catch (e: Exception) {
            isoString.take(10)
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<ReviewItem>() {
        override fun areItemsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ReviewItem, newItem: ReviewItem): Boolean = oldItem == newItem
    }
}
