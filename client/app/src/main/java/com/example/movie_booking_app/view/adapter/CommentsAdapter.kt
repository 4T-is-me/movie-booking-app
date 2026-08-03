package com.example.movie_booking_app.view.adapter

import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.CommentItemResponse
import com.example.movie_booking_app.databinding.ItemCommentBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.DateUtils
import com.google.android.material.bottomsheet.BottomSheetDialog

class CommentsAdapter(
    private val onReplyClick: (CommentItemResponse) -> Unit,
    private val onLikeClick: (CommentItemResponse, String) -> Unit
) : ListAdapter<Pair<CommentItemResponse, Int>, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding, onReplyClick, onLikeClick)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val pair = getItem(position)
        holder.bind(pair.first, pair.second)
    }

    class CommentViewHolder(
        private val binding: ItemCommentBinding,
        private val onReplyClick: (CommentItemResponse) -> Unit,
        private val onLikeClick: (CommentItemResponse, String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CommentItemResponse, level: Int) {
            // Indent comment based on level (nested reply depth)
            val density = itemView.context.resources.displayMetrics.density
            val indent = (level * 28 * density).toInt() // 28dp per level indent
            binding.layoutCommentRoot.setPaddingRelative(
                indent + (16 * density).toInt(), 
                binding.layoutCommentRoot.paddingTop, 
                (16 * density).toInt(), 
                binding.layoutCommentRoot.paddingBottom
            )

            binding.tvCommenterName.text = item.commenterName
            binding.tvCommentContent.text = item.content
            binding.tvTimeAgo.text = DateUtils.formatDateTime(item.createdAt)

            // Load Avatar
            val avatarUrl = item.commenterAvatar ?: ""
            val fullAvatarUrl = when {
                avatarUrl.isEmpty() -> ""
                avatarUrl.startsWith("http") -> avatarUrl
                else -> "${Constants.BASE_URL.removeSuffix("/")}$avatarUrl"
            }

            Glide.with(itemView.context)
                .load(fullAvatarUrl.ifEmpty { null })
                .placeholder(R.drawable.bg_ai_avatar)
                .error(R.drawable.bg_ai_avatar)
                .into(binding.ivCommenterAvatar)

            // Setup Reactions/Like UI
            val summary = item.reactionsSummary
            if (summary != null && summary.userReacted != null) {
                // User has reacted
                binding.btnLikeComment.text = summary.userReacted.uppercase()
                binding.btnLikeComment.setTextColor(ContextCompat.getColor(itemView.context, R.color.auto_color_F5C542))
            } else {
                // Not reacted
                binding.btnLikeComment.text = itemView.context.getString(R.string.auto_str_like)
                binding.btnLikeComment.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
            }

            // Reactions Display tally
            if (summary != null && summary.count > 0) {
                binding.layoutReactionsDisplay.visibility = View.VISIBLE
                
                // Build simple emoji breakdown string
                val breakdown = summary.breakdown ?: emptyMap()
                val emojis = breakdown.keys.map { type ->
                    when (type) {
                        "like" -> "👍"
                        "love" -> "❤️"
                        "haha" -> "😆"
                        "wow" -> "😲"
                        "sad" -> "😢"
                        "angry" -> "😡"
                        else -> "👍"
                    }
                }.distinct().joinToString("")

                binding.tvReactionsCount.text = "$emojis ${summary.count}"
            } else {
                binding.layoutReactionsDisplay.visibility = View.GONE
            }

            binding.btnReplyComment.setOnClickListener {
                onReplyClick(item)
            }

            binding.btnLikeComment.setOnClickListener {
                val currentReaction = summary?.userReacted ?: "like"
                onLikeClick(item, currentReaction)
            }

            // support long click to trigger simple dialog with reactions
            binding.btnLikeComment.setOnLongClickListener {
                showReactionsDialog(item)
                true
            }
        }

        private fun showReactionsDialog(item: CommentItemResponse) {
            val context = itemView.context
            val reactions = listOf(
                Pair("like", "👍"),
                Pair("love", "❤️"),
                Pair("haha", "😆"),
                Pair("wow", "😲"),
                Pair("sad", "😢"),
                Pair("angry", "😡")
            )

            val dialog = BottomSheetDialog(context)
            val sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_rate_staff, null) // repurpose dialog view or construct dynamic layout
            
            // Construct a simple linear layout dynamically for options to select reactions
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, 32, 0, 32)
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }

            reactions.forEach { reaction ->
                val btn = TextView(context).apply {
                    text = reaction.second
                    textSize = 28f
                    setPadding(24, 16, 24, 16)
                    isClickable = true
                    focusable = View.FOCUSABLE
                    val outValue = TypedValue()
                    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                    setBackgroundResource(outValue.resourceId)
                    setOnClickListener {
                        onLikeClick(item, reaction.first)
                        dialog.dismiss()
                    }
                }
                layout.addView(btn)
            }

            dialog.setContentView(layout)
            dialog.show()
        }
    }
}

class CommentDiffCallback : DiffUtil.ItemCallback<Pair<CommentItemResponse, Int>>() {
    override fun areItemsTheSame(oldItem: Pair<CommentItemResponse, Int>, newItem: Pair<CommentItemResponse, Int>): Boolean {
        return oldItem.first.id == newItem.first.id
    }

    override fun areContentsTheSame(oldItem: Pair<CommentItemResponse, Int>, newItem: Pair<CommentItemResponse, Int>): Boolean {
        return oldItem.first == newItem.first && oldItem.second == newItem.second
    }
}
