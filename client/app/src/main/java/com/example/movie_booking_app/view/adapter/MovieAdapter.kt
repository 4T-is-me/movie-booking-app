package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemMovieBinding
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

    class MovieViewHolder(private val binding: ItemMovieBinding, val onClick: (Movie) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        private val imgPoster: ShapeableImageView = binding.imgPoster
        private val tvNameVn: TextView = binding.tvMovieNameVn
        private val tvNameEn: TextView = binding.tvMovieNameEn
        private val tvGenre: TextView = binding.tvGenre
        private val tvReleaseDate: TextView = binding.tvReleaseDate

        fun bind(movie: Movie) {
            tvNameVn.text = movie.movieName
            tvNameEn.text = movie.movieName // Develop model only has movieName
            tvGenre.text = movie.movieGenre?.joinToString(", ") ?: "N/A"
            tvReleaseDate.text = com.example.movie_booking_app.utils.DateUtils.formatMovieDate(movie.releaseDate)

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