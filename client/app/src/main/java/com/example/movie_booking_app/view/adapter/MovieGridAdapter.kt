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
import com.example.movie_booking_app.databinding.ItemMovieGridBinding

class MovieGridAdapter(
    private val isNowPlaying: Boolean,
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieGridAdapter.GridViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val binding = ItemMovieGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GridViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position), isNowPlaying)
    }

    class GridViewHolder(
        private val binding: ItemMovieGridBinding,
        private val onClick: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movie: Movie, isNowPlaying: Boolean) {
            with(binding) {
                tvMovieNameVn.text = movie.movieName
                tvMovieNameEn.text = movie.movieName // develop model only has movieName
                
                tvTextRow1.text = (movie.rating?.toString() ?: "N/A")
                tvTextRow2.text = "${movie.duration ?: 0} min • " + (movie.censorship ?: "2D")
                tvTextRow3.text = com.example.movie_booking_app.utils.DateUtils.formatMovieDate(movie.releaseDate)

                Glide.with(itemView.context)
                    .load(movie.poster)
                    .placeholder(R.color.card_dark)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgPoster)

                btnViewSchedule.setOnClickListener { onClick(movie) }
                root.setOnClickListener { onClick(movie) }
            }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
