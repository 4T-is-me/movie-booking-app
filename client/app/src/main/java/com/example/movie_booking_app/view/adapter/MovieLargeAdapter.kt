package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.databinding.ItemMovieLargeBinding

class MovieLargeAdapter(
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieLargeAdapter.MovieLargeViewHolder>(MovieDiffCallback()) {

    override fun getItemCount(): Int {
        val size = currentList.size
        return if (size == 0) 0 else size * 1000
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieLargeViewHolder {
        val binding = ItemMovieLargeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieLargeViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: MovieLargeViewHolder, position: Int) {
        val size = currentList.size
        if (size > 0) {
            holder.bind(getItem(position % size))
        }
    }

    class MovieLargeViewHolder(
        private val binding: ItemMovieLargeBinding,
        val onClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            with(binding) {
                tvMovieNameVnLarge.text = movie.movieName
                tvMovieNameEnLarge.text = movie.movieGenre?.joinToString(", ")
                tvRatingLarge.text = "${movie.rating} (${movie.totalReviews})"
                tvVersionLarge.text = movie.censorship ?: "2D"
                
                Glide.with(itemView.context)
                    .load(movie.poster)
                    .centerCrop()
                    .placeholder(R.color.card_dark)
                    .error(android.R.drawable.ic_menu_report_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgPosterLarge)
                
                root.setOnClickListener { onClick(movie) }
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
