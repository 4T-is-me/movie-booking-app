package com.example.movie_booking_app.view.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.databinding.ItemMovieNewsBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.utils.DateUtils
import com.example.movie_booking_app.view.activity.MovieDetailActivity

class MovieNewsAdapter(
    private val onNewsClick: ((MovieNews) -> Unit)? = null,
    private val onLikeClick: ((MovieNews) -> Unit)? = null
) : ListAdapter<MovieNews, MovieNewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemMovieNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position), onNewsClick, onLikeClick)
    }

    class NewsViewHolder(private val binding: ItemMovieNewsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: MovieNews, onNewsClick: ((MovieNews) -> Unit)?, onLikeClick: ((MovieNews) -> Unit)?) {
            binding.tvNewsTitle.text = news.title
            
            val displayDate = if (news.date.contains("T")) {
                DateUtils.formatDateTime(news.date)
            } else {
                news.date
            }
            binding.tvPostMeta.text = "$displayDate • 🌐"
            binding.tvAuthorName.text = news.authorName ?: "Cinema Club ✔️"

            // Author Avatar loading
            val avatarUrl = news.authorAvatar ?: ""
            if (avatarUrl.isNotEmpty()) {
                val fullAvatarUrl = if (avatarUrl.startsWith("http")) avatarUrl
                                    else "${Constants.BASE_URL.removeSuffix("/")}${avatarUrl}"
                Glide.with(itemView.context)
                    .load(fullAvatarUrl)
                    .placeholder(R.drawable.bg_ai_avatar)
                    .error(R.drawable.bg_ai_avatar)
                    .into(binding.ivAuthorAvatar)
            } else {
                binding.ivAuthorAvatar.setImageResource(R.drawable.bg_ai_avatar)
            }

            Glide.with(itemView.context)
                .load(news.imageUrl)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imgNews)

            // Setup Tagged Movie mini-card
            if (!news.movieId.isNullOrEmpty()) {
                binding.layoutMovieTag.visibility = View.VISIBLE
                binding.tvMovieTagSubtitle.text = news.movieName ?: ""

                val fullPosterUrl = when {
                    news.moviePoster.isNullOrEmpty() -> ""
                    news.moviePoster.startsWith("http") -> news.moviePoster
                    else -> "${Constants.BASE_URL.removeSuffix("/")}${news.moviePoster}"
                }

                Glide.with(itemView.context)
                    .load(fullPosterUrl.ifEmpty { null })
                    .placeholder(R.drawable.movie)
                    .error(R.drawable.movie)
                    .into(binding.ivMoviePoster)

                // Open Movie details on card click
                binding.layoutMovieTag.setOnClickListener {
                    val intent = Intent(itemView.context, MovieDetailActivity::class.java).apply {
                        putExtra("movieId", news.movieId)
                    }
                    itemView.context.startActivity(intent)
                }
            } else {
                binding.layoutMovieTag.visibility = View.GONE
            }

            // Real Social tallies from DB
            binding.tvLikesTally.text = "👍❤️ ${news.likesCount}"
            binding.tvCommentsTally.text = "${news.commentsCount} comments"

            // Click listener for details redirection
            itemView.setOnClickListener {
                onNewsClick?.invoke(news)
            }
            binding.btnCommentPost.setOnClickListener {
                onNewsClick?.invoke(news)
            }

            // Real Like state color mapping
            val colorRes = if (news.userLiked) R.color.auto_color_F5C542 else R.color.white
            val colorVal = ContextCompat.getColor(itemView.context, colorRes)
            binding.ivLikeIcon.setColorFilter(colorVal)
            binding.tvLikeText.setTextColor(colorVal)

            binding.btnLikePost.setOnClickListener {
                onLikeClick?.invoke(news)
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<MovieNews>() {
        override fun areItemsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem == newItem
    }
}
