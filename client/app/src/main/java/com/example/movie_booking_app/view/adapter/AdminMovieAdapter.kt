package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie

class AdminMovieAdapter(
    private val onEditClick: (Movie) -> Unit,
    private val onDeleteClick: (Movie) -> Unit
) : ListAdapter<Movie, AdminMovieAdapter.AdminMovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminMovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_movie, parent, false)
        return AdminMovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminMovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminMovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivPoster: ImageView = view.findViewById(R.id.ivMoviePoster)
        private val tvTitle: TextView = view.findViewById(R.id.tvMovieTitle)
        private val tvDuration: TextView = view.findViewById(R.id.tvMovieDuration)
        private val btnEdit: ImageButton = view.findViewById(R.id.btnEditMovie)
        private val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteMovie)

        fun bind(movie: Movie) {
            tvTitle.text = movie.movieName
            tvDuration.text = "${movie.duration ?: 0} mins"

            Glide.with(itemView.context)
                .load(movie.poster)
                .placeholder(R.color.card_dark)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivPoster)

            val isInactive = movie.isDeleted == true
            btnDelete.visibility = if (isInactive) View.GONE else View.VISIBLE
            btnDelete.isEnabled = !isInactive

            btnEdit.setOnClickListener { onEditClick(movie) }
            btnDelete.setOnClickListener { onDeleteClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean {
            return oldItem == newItem
        }
    }
}
