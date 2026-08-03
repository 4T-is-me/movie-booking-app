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
import com.example.movie_booking_app.databinding.ItemAdminMovieBinding

class AdminMovieAdapter(
    private val onEditClick: (Movie) -> Unit,
    private val onDeleteClick: (Movie) -> Unit,
    private val onRestoreClick: (Movie) -> Unit
) : ListAdapter<Movie, AdminMovieAdapter.AdminMovieViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminMovieViewHolder {
        val binding = ItemAdminMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdminMovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminMovieViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminMovieViewHolder(private val binding: ItemAdminMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        private val ivPoster: ImageView = binding.ivMoviePoster
        private val tvTitle: TextView = binding.tvMovieTitle
        private val tvDuration: TextView = binding.tvMovieDuration
        private val btnEdit: ImageButton = binding.btnEditMovie
        private val btnDelete: ImageButton = binding.btnDeleteMovie
        private val btnRestore: ImageButton = binding.btnRestoreMovie

        fun bind(movie: Movie) {
            tvTitle.text = movie.movieName
            tvDuration.text = itemView.context.getString(R.string.duration_mins, (movie.duration ?: 0).toString())

            Glide.with(itemView.context)
                .load(movie.poster)
                .placeholder(R.color.card_dark)
                .error(android.R.drawable.ic_menu_report_image)
                .into(ivPoster)

            val isDeleted = movie.isDeleted == true
            btnDelete.visibility = if (isDeleted) View.GONE else View.VISIBLE
            btnRestore.visibility = if (isDeleted) View.VISIBLE else View.GONE
            btnEdit.visibility = if (isDeleted) View.GONE else View.VISIBLE

            btnEdit.setOnClickListener { onEditClick(movie) }
            btnDelete.setOnClickListener { onDeleteClick(movie) }
            btnRestore.setOnClickListener { onRestoreClick(movie) }
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
