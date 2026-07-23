package com.example.movie_booking_app.view.adapter

import com.example.movie_booking_app.databinding.ItemVideoVerticalBinding
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.google.android.material.imageview.ShapeableImageView
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

class VideoListAdapter(
    private val lifecycle: Lifecycle
) : ListAdapter<Movie, VideoListAdapter.VideoViewHolder>(MovieDiffCallback()) {

    private var activePlayerPosition: Int = -1
    private val activePlayers = mutableMapOf<Int, YouTubePlayer>()

    fun getActivePlayerPosition(): Int = activePlayerPosition

    fun pauseActivePlayer() {
        if (activePlayerPosition != -1) {
            val player = activePlayers[activePlayerPosition]
            player?.pause()
        }
    }

    fun releaseActivePlayer() {
        if (activePlayerPosition != -1) {
            val position = activePlayerPosition
            activePlayerPosition = -1
            notifyItemChanged(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoVerticalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position), position == activePlayerPosition)
    }

    override fun onViewRecycled(holder: VideoViewHolder) {
        super.onViewRecycled(holder)
        val pos = holder.bindingAdapterPosition
        if (pos == activePlayerPosition) {
            holder.releasePlayer()
            activePlayers.remove(pos)
            activePlayerPosition = -1
        } else {
            holder.releasePlayer()
        }
    }

    inner class VideoViewHolder(private val binding: ItemVideoVerticalBinding) : RecyclerView.ViewHolder(binding.root) {
        val youtubePlayerView: YouTubePlayerView = binding.youtubePlayerView
        val imgThumbnail: ShapeableImageView = binding.imgThumbnail
        val viewOverlay: View = binding.viewOverlay
        val btnPlay: View = binding.btnPlay
        val tvVideoTitle: TextView = binding.tvVideoTitle
        val tvMovieInfo: TextView = binding.tvMovieInfo
        
        private var isPlayerInitialized = false

        fun bind(movie: Movie, isPlaying: Boolean) {
            tvVideoTitle.text = movie.movieName
            tvMovieInfo.text = "${movie.language ?: "English"} • ${movie.duration ?: 120} mins"

            Glide.with(itemView.context)
                .load(movie.poster)
                .centerCrop()
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgThumbnail)

            if (isPlaying) {
                youtubePlayerView.visibility = View.VISIBLE
                imgThumbnail.visibility = View.GONE
                viewOverlay.visibility = View.GONE
                btnPlay.visibility = View.GONE

                if (!isPlayerInitialized) {
                    isPlayerInitialized = true
                    lifecycle.addObserver(youtubePlayerView)
                    
                    val packageName = itemView.context.packageName
                    val options = IFramePlayerOptions.Builder(itemView.context)
                        .controls(1)
                        .origin("https://$packageName")
                        .build()

                    youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            activePlayers[bindingAdapterPosition] = youTubePlayer
                            val videoId = getYoutubeVideoId(movie.id).trim()
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    }, options)
                }
            } else {
                releasePlayer()
                youtubePlayerView.visibility = View.GONE
                imgThumbnail.visibility = View.VISIBLE
                viewOverlay.visibility = View.VISIBLE
                btnPlay.visibility = View.VISIBLE
            }

            btnPlay.setOnClickListener {
                val currentPos = bindingAdapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    val prevPos = activePlayerPosition
                    activePlayerPosition = currentPos
                    if (prevPos != -1) {
                        notifyItemChanged(prevPos)
                    }
                    notifyItemChanged(currentPos)
                }
            }
        }

        fun releasePlayer() {
            if (isPlayerInitialized) {
                lifecycle.removeObserver(youtubePlayerView)
                youtubePlayerView.release()
                isPlayerInitialized = false
            }
        }
    }

    private fun getYoutubeVideoId(movieId: String?): String {
        return when (movieId) {
            "1" -> "dQw4w9WgXcQ " // Avengers: Endgame
            "2" -> "dQw4w9WgXcQ " // Spider-Man: No Way Home
            "3" -> "dQw4w9WgXcQ " // Jurassic World
            else -> "dQw4w9WgXcQ " // Dune
        }
    }

    class MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
        override fun areItemsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Movie, newItem: Movie): Boolean = oldItem == newItem
    }
}
