package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.google.android.material.card.MaterialCardView

class StaffMovieAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, StaffMovieAdapter.MovieViewHolder>(MovieDiffCallback) {

    private var selectedMovieId: String? = null

    fun setSelectedMovie(movieId: String?) {
        selectedMovieId = movieId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_staff_movie_card, parent, false)
        return MovieViewHolder(view, onMovieClick)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie, movie.id == selectedMovieId)
    }

    inner class MovieViewHolder(
        view: View,
        private val onClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(view) {
        private val cardMovie: MaterialCardView = view.findViewById(R.id.cardMovie)
        private val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
        private val tvMovieTitle: TextView = view.findViewById(R.id.tvMovieTitle)
        private val tvMovieInfo: TextView = view.findViewById(R.id.tvMovieInfo)
        private val tvMovieRating: TextView = view.findViewById(R.id.tvMovieRating)
        private val tvMovieVotes: TextView = view.findViewById(R.id.tvMovieVotes)
        private val tvNowPlayingBadge: TextView = view.findViewById(R.id.tvNowPlayingBadge)

        fun bind(movie: Movie, isSelected: Boolean) {
            tvMovieTitle.text = movie.movieName
            
            // Format Info: Genre | Duration
            val genreStr = movie.movieGenre?.joinToString(", ") ?: ""
            val durationStr = if (movie.duration != null) itemView.context.getString(com.example.movie_booking_app.R.string.duration_mins, movie.duration.toString()) else "N/A"
            tvMovieInfo.text = "$genreStr | $durationStr"

            // Rating & Votes
            tvMovieRating.text = String.format("%.1f", movie.rating ?: 0.0)
            tvMovieVotes.text = "(${movie.totalReviews ?: 0} votes)"

            // Now Playing status badge visibility
            val status = movie.status ?: ""
            if (status.equals("now_playing", ignoreCase = true) || status.equals("active", ignoreCase = true)) {
                tvNowPlayingBadge.visibility = View.VISIBLE
            } else {
                tvNowPlayingBadge.visibility = View.GONE
            }

            // Selection indicator stroke
            cardMovie.strokeWidth = if (isSelected) {
                (2 * itemView.context.resources.displayMetrics.density).toInt()
            } else {
                0
            }

            Glide.with(itemView.context)
                .load(movie.poster)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPoster)

            itemView.setOnClickListener { onClick(movie) }
        }
    }

    object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
