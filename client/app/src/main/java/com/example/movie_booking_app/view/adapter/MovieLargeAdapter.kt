package com.example.movie_booking_app.view.adapter

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
import com.example.movie_booking_app.data.model.entity.Movie
import com.google.android.material.imageview.ShapeableImageView

class MovieLargeAdapter(
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieLargeAdapter.MovieLargeViewHolder>(MovieDiffCallback()) {

    override fun getItemCount(): Int {
        val size = currentList.size
        return if (size == 0) 0 else size * 1000
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieLargeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_large, parent, false)
        return MovieLargeViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: MovieLargeViewHolder, position: Int) {
        val size = currentList.size
        if (size > 0) {
            holder.bind(getItem(position % size))
        }
    }

    class MovieLargeViewHolder(view: View, val onClick: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val imgPoster: ShapeableImageView = view.findViewById(R.id.imgPosterLarge)
        private val tvNameVn: TextView = view.findViewById(R.id.tvMovieNameVnLarge)
        private val tvNameEn: TextView = view.findViewById(R.id.tvMovieNameEnLarge)
        private val tvRating: TextView = view.findViewById(R.id.tvRatingLarge)
        private val tvVersion: TextView = view.findViewById(R.id.tvVersionLarge)

        fun bind(movie: Movie) {
            tvNameVn.text = movie.movieName
            tvNameEn.text = movie.movieGenre?.joinToString(", ")
            tvRating.text = "${movie.rating} (${movie.totalReviews})"
            tvVersion.text = movie.censorship ?: "2D"
            
            Glide.with(itemView.context)
                .load(movie.poster)
                .centerCrop()
                .placeholder(R.color.card_dark)
                .error(android.R.drawable.ic_menu_report_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPoster)
            
            itemView.setOnClickListener { onClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
