package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.databinding.ItemVideoHorizontalBinding
import com.google.android.material.imageview.ShapeableImageView

class VideoHorizontalAdapter(
    private val onClick: (Movie) -> Unit
) : ListAdapter<Movie, VideoHorizontalAdapter.ViewHolder>(MovieDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemVideoHorizontalBinding, val onClick: (Movie) -> Unit) : RecyclerView.ViewHolder(binding.root) {
        private val imgVideoThumbnail: ShapeableImageView = binding.imgVideoThumbnail
        private val tvVideoName: TextView = binding.tvVideoName

        fun bind(movie: Movie) {
            tvVideoName.text = movie.movieName

            Glide.with(itemView.context)
                .load(movie.poster)
                .centerCrop()
                .placeholder(R.color.card_dark)
                .error(android.R.drawable.ic_menu_report_image)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgVideoThumbnail)

            itemView.setOnClickListener { onClick(movie) }
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
