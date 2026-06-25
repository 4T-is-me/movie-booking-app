package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.crossfade
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import java.text.SimpleDateFormat
import java.util.Locale

class NowPlayingAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<NowPlayingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPoster: ImageView = view.findViewById(R.id.ivPoster)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvGenres: TextView = view.findViewById(R.id.tvGenres)
        val tvRating: TextView = view.findViewById(R.id.tvRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_now_playing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = movies[position]
        holder.tvTitle.text = movie.movieName
        
        val duration = movie.duration ?: 0
        val h = duration / 60
        val m = duration % 60
        val durationStr = "${h}h${m}m"
        val genres = movie.movieGenre?.joinToString(", ") ?: ""
        holder.tvGenres.text = "$durationStr • $genres"
        
        holder.tvRating.text = "⭐ ${movie.rating ?: 0.0} (${movie.totalReviews ?: 0})"
        
        movie.poster?.let {
            holder.ivPoster.load(it) {
                crossfade(true)
            }
        }
    }

    override fun getItemCount() = movies.size
}

class ComingSoonAdapter(private val movies: List<Movie>) : RecyclerView.Adapter<ComingSoonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvComingSoonTitle)
        val tvGenres: TextView = view.findViewById(R.id.tvComingSoonGenres)
        val tvDate: TextView = view.findViewById(R.id.tvComingSoonDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_coming_soon, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = movies[position]
        holder.tvTitle.text = movie.movieName
        val genres = movie.movieGenre?.joinToString(", ") ?: ""
        holder.tvGenres.text = "📷 $genres"
        
        val dateStr = try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = movie.releaseDate?.let { parser.parse(it) }
            date?.let { formatter.format(it) } ?: movie.releaseDate
        } catch (e: Exception) {
            movie.releaseDate
        }
        
        holder.tvDate.text = "📅 $dateStr"
        
        // Find imageView dynamically inside cvComingSoonPoster because id is missing or unknown
        val ivPoster = holder.itemView.findViewById<androidx.cardview.widget.CardView>(R.id.cvComingSoonPoster).getChildAt(0) as? ImageView
        movie.poster?.let { url ->
            ivPoster?.load(url) {
                crossfade(true)
            }
        }
    }

    override fun getItemCount() = movies.size
}
