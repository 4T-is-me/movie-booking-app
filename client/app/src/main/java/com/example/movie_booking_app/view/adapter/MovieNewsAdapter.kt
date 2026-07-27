package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemMovieNewsBinding
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.google.android.material.imageview.ShapeableImageView

class MovieNewsAdapter(
    private val onNewsClick: ((MovieNews) -> Unit)? = null
) : ListAdapter<MovieNews, MovieNewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val binding = ItemMovieNewsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position), onNewsClick)
    }

    class NewsViewHolder(private val binding: ItemMovieNewsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(news: MovieNews, onNewsClick: ((MovieNews) -> Unit)?) {
            binding.tvNewsTitle.text = news.title
            
            val displayDate = if (news.date.contains("T")) {
                com.example.movie_booking_app.utils.DateUtils.formatDateTime(news.date)
            } else {
                news.date
            }
            binding.tvPostMeta.text = "$displayDate • 🌐"
            binding.tvAuthorName.text = "Cinema Club ✔️"

            Glide.with(itemView.context)
                .load(news.imageUrl)
                .placeholder(R.color.card_dark)
                .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                .into(binding.imgNews)

            // Setup Tagged Movie mini-card
            if (!news.movieId.isNullOrEmpty()) {
                binding.layoutMovieTag.visibility = android.view.View.VISIBLE
                binding.tvMovieTagSubtitle.text = news.movieName ?: ""

                val fullPosterUrl = when {
                    news.moviePoster.isNullOrEmpty() -> ""
                    news.moviePoster.startsWith("http") -> news.moviePoster
                    else -> "${com.example.movie_booking_app.utils.Constants.BASE_URL.removeSuffix("/")}${news.moviePoster}"
                }

                Glide.with(itemView.context)
                    .load(fullPosterUrl.ifEmpty { null })
                    .placeholder(R.drawable.movie)
                    .error(R.drawable.movie)
                    .into(binding.ivMoviePoster)

                // Open Movie details on card click
                binding.layoutMovieTag.setOnClickListener {
                    val intent = Intent(itemView.context, com.example.movie_booking_app.view.activity.MovieDetailActivity::class.java).apply {
                        putExtra("movieId", news.movieId)
                    }
                    itemView.context.startActivity(intent)
                }
            } else {
                binding.layoutMovieTag.visibility = android.view.View.GONE
            }

            // Click listener for details redirection
            itemView.setOnClickListener {
                onNewsClick?.invoke(news)
            }
            binding.btnCommentPost.setOnClickListener {
                onNewsClick?.invoke(news)
            }

            // Simple mock Like button color toggle
            var isLiked = false
            binding.btnLikePost.setOnClickListener {
                isLiked = !isLiked
                val colorRes = if (isLiked) R.color.auto_color_F5C542 else R.color.white
                val colorVal = androidx.core.content.ContextCompat.getColor(itemView.context, colorRes)
                binding.ivLikeIcon.setColorFilter(colorVal)
                binding.tvLikeText.setTextColor(colorVal)
                
                // Toggle mock tallies
                binding.tvLikesTally.text = if (isLiked) "👍❤️ 1.9k" else "👍❤️ 1.8k"
            }
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<MovieNews>() {
        override fun areItemsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem == newItem
    }
}
