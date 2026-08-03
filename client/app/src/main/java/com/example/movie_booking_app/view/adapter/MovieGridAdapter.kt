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
import com.example.movie_booking_app.utils.DateUtils

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

        val tvTitle = binding.tvMovieNameVn
        val tvTitleEn = binding.tvMovieNameEn
        val tvRow1 = binding.tvTextRow1
        val tvRow2 = binding.tvTextRow2
        val tvRow3 = binding.tvTextRow3
        val imgPoster = binding.imgPoster
        val btnViewSchedule = binding.btnViewSchedule
        val root = binding.root

        fun bind(movie: Movie, isNowPlaying: Boolean) {
                tvTitle.text = movie.movieName
                tvTitleEn.text = movie.movieName // develop model only has movieName
                
                tvRow1.text = (movie.rating?.toString() ?: "N/A")
                val durationStr = itemView.context.getString(R.string.duration_mins, (movie.duration ?: 0).toString())
                tvRow2.text = "$durationStr • " + (movie.censorship ?: "2D")
                tvRow3.text = DateUtils.formatMovieDate(movie.releaseDate)

                Glide.with(itemView.context)
                    .load(movie.poster)
                    .placeholder(R.color.card_dark)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(imgPoster)

                btnViewSchedule.setOnClickListener { onClick(movie) }
                root.setOnClickListener { onClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
