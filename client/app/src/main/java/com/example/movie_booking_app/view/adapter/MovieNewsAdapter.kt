package com.example.movie_booking_app.view.adapter

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
import com.example.movie_booking_app.data.model.ui.MovieNews
import com.google.android.material.imageview.ShapeableImageView

class MovieNewsAdapter : ListAdapter<MovieNews, MovieNewsAdapter.NewsViewHolder>(NewsDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgPoster: ShapeableImageView = view.findViewById(R.id.imgNews)
        private val tvTitle: TextView = view.findViewById(R.id.tvNewsTitle)
        private val tvDate: TextView = view.findViewById(R.id.tvNewsDate)

        fun bind(news: MovieNews) {
            tvTitle.text = news.title
            tvDate.text = news.date
            
            Glide.with(itemView.context)
                .load(news.imageUrl)
                .placeholder(R.color.card_dark)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imgPoster)
        }
    }

    class NewsDiffCallback : DiffUtil.ItemCallback<MovieNews>() {
        override fun areItemsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: MovieNews, newItem: MovieNews): Boolean = oldItem == newItem
    }
}
