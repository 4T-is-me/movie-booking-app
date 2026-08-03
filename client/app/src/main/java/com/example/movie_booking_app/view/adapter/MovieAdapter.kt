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
import com.example.movie_booking_app.databinding.ItemMovieBinding
import com.example.movie_booking_app.utils.DateUtils

class MovieAdapter(
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieAdapter.MovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MovieViewHolder(
        private val binding: ItemMovieBinding,
        val onClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie) {
            with(binding) {
                tvMovieNameVn.text = movie.movieName
                tvMovieNameEn.text = movie.movieName // Develop model only has movieName
                tvGenre.text = movie.movieGenre?.joinToString(", ") ?: "N/A"
                tvReleaseDate.text = DateUtils.formatMovieDate(movie.releaseDate)

                Glide.with(itemView.context)
                    .load(movie.poster)
                    .centerCrop()
                    .placeholder(R.color.card_dark)
                    .error(android.R.drawable.ic_menu_report_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgPoster)

                root.setOnClickListener { onClick(movie) }
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}