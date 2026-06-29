package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.google.android.material.imageview.ShapeableImageView

class MovieGridAdapter(
    private val isNowPlaying: Boolean,
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieGridAdapter.GridViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_grid, parent, false)
        return GridViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(getItem(position), isNowPlaying)
    }

    class GridViewHolder(view: View, private val onClick: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val imgPoster: ImageView = view.findViewById(R.id.imgPoster)
        private val tvTitle: TextView = view.findViewById(R.id.tvMovieNameVn)
        private val tvTitleEn: TextView = view.findViewById(R.id.tvMovieNameEn)
        private val tvRow1: TextView = view.findViewById(R.id.tvTextRow1)
        private val tvRow2: TextView = view.findViewById(R.id.tvTextRow2)
        private val tvRow3: TextView = view.findViewById(R.id.tvTextRow3)
        private val btnViewSchedule: Button = view.findViewById(R.id.btnViewSchedule)

        fun bind(movie: Movie, isNowPlaying: Boolean) {
            tvTitle.text = movie.movieName
            tvTitleEn.text = movie.movieName // develop model only has movieName
            
            tvRow1.text = (movie.rating?.toString() ?: "N/A")
            tvRow2.text = "${movie.duration ?: 0} min • " + (movie.censorship ?: "2D")
            tvRow3.text = com.example.movie_booking_app.utils.DateUtils.formatMovieDate(movie.releaseDate)

            Glide.with(itemView.context)
                .load(movie.poster)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPoster)

            btnViewSchedule.setOnClickListener { onClick(movie) }
            itemView.setOnClickListener { onClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
