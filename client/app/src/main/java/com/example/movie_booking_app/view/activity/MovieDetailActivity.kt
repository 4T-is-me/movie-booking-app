package com.example.movie_booking_app.view.activity

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.movie_booking_app.data.model.entity.Cinema
import com.example.movie_booking_app.databinding.ActivityMovieDetailBinding
import com.example.movie_booking_app.view.adapter.CardItemAdapter
import com.example.movie_booking_app.view.adapter.CinemaAdapter
import com.example.movie_booking_app.view.adapter.ReviewAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding

    private val viewModel: MovieViewModel by viewModels()

    private lateinit var movieId: String
    private var selectedCinema: Cinema? = null
    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy movieId từ Intent
        movieId = intent.getStringExtra("movieId") ?: ""

        if (movieId.isEmpty()) {
            Toast.makeText(this, "Movie ID invalid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Gọi API
        viewModel.fetchMovieDetail(movieId)
        viewModel.fetchMovieShowtimes(movieId)
        viewModel.fetchMovieReviews(movieId)

        // Setup ReviewAdapter
        reviewAdapter = ReviewAdapter(emptyList())
        binding.rvReviews.adapter = reviewAdapter

        binding.btnBookTicket.setOnClickListener {
            val movie = viewModel.movieDetail.value?.data

            if (movie?.status == "coming-soon") {
                Toast.makeText(this, "This movie is coming soon.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedCinema == null) {
                Toast.makeText(this, "Please select a cinema first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SelectSeatActivity::class.java).apply {
                putExtra("movieId", movieId)
                putExtra("cinemaId", selectedCinema!!.id)
                val movie = viewModel.movieDetail.value?.data
                putExtra("movieName", movie?.movieName ?: "")
                putExtra("moviePoster", movie?.poster ?: "")
                putExtra("movieGenre", movie?.movieGenre?.joinToString(", ") ?: "")
                putExtra("cinemaName", selectedCinema!!.cinemaName ?: "")
                putExtra("cinemaAddress", selectedCinema!!.address ?: "")
            }
            startActivity(intent)
        }

        binding.ivBack.setOnClickListener {
            finish()
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchMovieDetail(movieId)
            viewModel.fetchMovieShowtimes(movieId)
            viewModel.fetchMovieReviews(movieId)
        }
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_orange_light,
            android.R.color.black
        )

        observeMovie()
    }

    private fun observeMovie() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.movieDetail.collect { response ->
                        binding.swipeRefresh.isRefreshing = false
                        response?.data?.let { movie ->
                            val review = movie.totalReviews.toString()
                            binding.txtTitle.text = movie.movieName ?: "Unknown"
                            binding.txtDescription.text = movie.description ?: ""
                            binding.txtGenre.text =  movie.movieGenre?.joinToString(", ") ?: ""
                            binding.txtLanguage.text = movie.language ?: ""
                            binding.txtDuration.text = formatDuration(movie.duration)
                            binding.txtReleaseDate.text = com.example.movie_booking_app.utils.DateUtils.formatMovieDate(movie.releaseDate)
                            binding.txtRating.text = movie.rating?.toString() ?: "Unrated"
                            binding.txtVote.text = "(${review})"
                            // Update rating summary in reviews section
                            val rating = movie.rating ?: 0.0
                            binding.tvAvgRating.text = String.format("%.1f", rating)
                            binding.ratingBarSummary.rating = rating.toFloat()
                            val total = movie.totalReviews ?: 0
                            binding.tvReviewCount.text = "$total review${if (total != 1) "s" else ""}"
                            if (total > 0) {
                                binding.layoutRatingSummary.visibility = View.VISIBLE
                                binding.tvSeeAllReviews.visibility = View.VISIBLE
                            } else {
                                binding.layoutRatingSummary.visibility = View.GONE
                                binding.tvSeeAllReviews.visibility = View.GONE
                            }

                            val posterUrl = movie.poster
                            val fullUrl = if (posterUrl?.startsWith("http") == true) posterUrl 
                                          else "${com.example.movie_booking_app.utils.Constants.BASE_URL}${posterUrl?.removePrefix("/")}"

                            Glide.with(this@MovieDetailActivity)
                                .load(fullUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.stat_notify_error)
                                .into(binding.imgPoster)

                            binding.rvDirector.layoutManager = LinearLayoutManager(this@MovieDetailActivity, LinearLayoutManager.HORIZONTAL, false)
                            binding.rvDirector.adapter = CardItemAdapter(movie.director ?: emptyList())

                            binding.rvActors.layoutManager = LinearLayoutManager(this@MovieDetailActivity, LinearLayoutManager.HORIZONTAL, false)
                            binding.rvActors.adapter = CardItemAdapter(movie.actors ?: emptyList())

                             if (movie.hasWatched == true) {
                                 binding.btnWriteReview.visibility = View.VISIBLE
                                 binding.btnWriteReview.setOnClickListener {
                                     val intent = Intent(this@MovieDetailActivity, WriteReviewActivity::class.java).apply {
                                         putExtra("movieId", movieId)
                                         putExtra("movieTitle", movie.movieName ?: "")
                                         putExtra("moviePoster", movie.poster ?: "")
                                         putExtra("movieGenre", movie.movieGenre?.joinToString(", ") ?: "")
                                     }
                                     startActivityForResult(intent, REQUEST_WRITE_REVIEW)
                                 }
                             } else {
                                 binding.btnWriteReview.visibility = View.GONE
                             }
                        }

                    }
                }
                launch {
                    viewModel.movieShowtimes.collect { showtimes ->
                        binding.swipeRefresh.isRefreshing = false
                        // no cinema is not yet available.
                        if (showtimes.isEmpty()) {
                            binding.rvCinema.visibility = View.GONE
                            binding.tvNoCinema.visibility = View.VISIBLE
                            return@collect
                        }

                        binding.rvCinema.visibility = View.VISIBLE
                        binding.tvNoCinema.visibility = View.GONE
                        val cinemas = showtimes
                            .mapNotNull {
                                it.cinemaId
                            }
                            .distinctBy {
                                it.id ?: it.cinemaName
                            }
                        
                        if (cinemas.isNotEmpty()) {
                            binding.rvCinema.layoutManager = LinearLayoutManager(this@MovieDetailActivity, LinearLayoutManager.VERTICAL, false)
                            binding.rvCinema.adapter = CinemaAdapter(cinemas) { cinema ->
                                selectedCinema = cinema
                            }
                        }
                    }
                }

                launch {
                    viewModel.showtimeError.collect { error ->
                        error?.let {
                            binding.swipeRefresh.isRefreshing = false
                            Toast.makeText(this@MovieDetailActivity, "Showtimes: $it", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    viewModel.detailError.collect { error ->
                        error?.let {
                            binding.swipeRefresh.isRefreshing = false
                            Toast.makeText(this@MovieDetailActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    viewModel.reviewActionSuccess.collect { success ->
                        if (success) {
                            viewModel.fetchMovieDetail(movieId)
                            viewModel.fetchMovieReviews(movieId)
                        }
                    }
                }

                launch {
                    viewModel.movieReviews.collect { reviews ->
                        binding.swipeRefresh.isRefreshing = false
                        if (reviews.isEmpty()) {
                            binding.tvNoReviews.visibility = View.VISIBLE
                            binding.rvReviews.visibility = View.GONE
                        } else {
                            binding.tvNoReviews.visibility = View.GONE
                            binding.rvReviews.visibility = View.VISIBLE
                            reviewAdapter.submitList(reviews)
                        }
                    }
                }
            }
        }
    }

    fun formatDuration(minutes: Int?): String {
        if (minutes == null || minutes <= 0) return "0m"

        val h = minutes / 60
        val m = minutes % 60

        return when {
            h > 0 && m > 0 -> "${h}h${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_REVIEW && resultCode == RESULT_OK) {
            // Tải lại thông tin phim (rating mới)
            viewModel.fetchMovieDetail(movieId)
        }
    }

    companion object {
        private const val REQUEST_WRITE_REVIEW = 1001
    }
}