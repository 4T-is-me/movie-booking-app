package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.ReviewItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import com.bumptech.glide.Glide
import com.example.movie_booking_app.utils.Constants

class ReviewAdapter(
    @Suppress("UNUSED_PARAMETER") items: List<ReviewItem> = emptyList()
) : ListAdapter<ReviewItem, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    inner class ReviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatarText: TextView = view.findViewById(R.id.tvAvatarText)
        val ivAvatarImage: com.google.android.material.imageview.ShapeableImageView = view.findViewById(R.id.ivAvatarImage)
        val tvName: TextView = view.findViewById(R.id.tvReviewerName)
        val tvDate: TextView = view.findViewById(R.id.tvReviewDate)
        val tvRating: TextView = view.findViewById(R.id.tvReviewRating)
        val tvComment: TextView = view.findViewById(R.id.tvReviewComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = getItem(position)

        // Get display name prioritizing fullName -> username -> "Anonymous"
        val displayName = review.accountId?.memberInfo?.fullName
            ?: review.accountId?.username
            ?: "Anonymous"

        // Avatar: get initial letter
        holder.tvAvatarText.text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        holder.tvName.text = displayName

        // Load avatar image if exists
        val avatarUrl = review.accountId?.memberInfo?.image
        if (!avatarUrl.isNullOrEmpty()) {
            val finalUrl = when {
                avatarUrl.startsWith("http") -> avatarUrl
                else -> "${Constants.BASE_URL.removeSuffix("/")}/${avatarUrl.removePrefix("/")}"
            }
            holder.ivAvatarImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(finalUrl)
                .into(holder.ivAvatarImage)
        } else {
            holder.ivAvatarImage.visibility = View.GONE
        }

        // Rating
        holder.tvRating.text = review.rating.toString()

        // Comment
        holder.tvComment.text = review.comment ?: ""

        // Creation date
        holder.tvDate.text = formatRelativeDate(review.createdAt)
    }

    private fun formatRelativeDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val nowMillis = System.currentTimeMillis()
            val diffMillis = nowMillis - instant.toEpochMilli()
            val diffMinutes = diffMillis / 60_000
            val diffHours = diffMinutes / 60
            val diffDays = diffHours / 24
            when {
                diffMinutes < 1 -> "Just now"
                diffMinutes < 60 -> "${diffMinutes}m ago"
                diffHours < 24 -> "${diffHours}h ago"
                diffDays < 30 -> "${diffDays}d ago"
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
