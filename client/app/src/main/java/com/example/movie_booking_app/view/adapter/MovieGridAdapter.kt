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
        private val imgPoster: ShapeableImageView = view.findViewById(R.id.imgPoster)
        private val tvNameVn: TextView = view.findViewById(R.id.tvMovieNameVn)
        private val tvNameEn: TextView = view.findViewById(R.id.tvMovieNameEn)
        private val tvTextRow1: TextView = view.findViewById(R.id.tvTextRow1)
        private val tvTextRow2: TextView = view.findViewById(R.id.tvTextRow2)
        private val tvTextRow3: TextView = view.findViewById(R.id.tvTextRow3)
        private val imgIconRow1: ImageView = view.findViewById(R.id.imgIconRow1)
        private val btnViewSchedule: Button = view.findViewById(R.id.btnViewSchedule)

        fun bind(movie: Movie, isNowPlaying: Boolean) {
            tvNameVn.text = movie.nameVn
            tvNameEn.text = movie.nameEn
            
            if (isNowPlaying) {
                tvTextRow1.text = movie.rating.toString()
                tvTextRow2.text = "${movie.duration} min"
                tvTextRow3.text = movie.version
            } else {
                imgIconRow1.setImageResource(android.R.drawable.ic_menu_today)
                imgIconRow1.setColorFilter(ContextCompat.getColor(itemView.context, R.color.white))
                tvTextRow1.text = movie.releaseDate
                tvTextRow2.text = "${movie.duration} min"
                tvTextRow3.text = movie.version
            }

            Glide.with(itemView.context)
                .load(movie.posterUrl)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPoster)

            btnViewSchedule.setOnClickListener { onClick(movie) }
            itemView.setOnClickListener { onClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.movieId == newItem.movieId
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
