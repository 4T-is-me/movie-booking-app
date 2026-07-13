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
    private var currentReviewDialog: com.google.android.material.bottomsheet.BottomSheetDialog? = null

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

        observeMovie()
    }

    private fun observeMovie() {

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.movieDetail.collect { response ->
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
                                     showWriteReviewDialog()
                                 }
                             } else {
                                 binding.btnWriteReview.visibility = View.GONE
                             }
                        }

                    }
                }
                launch {
                    viewModel.movieShowtimes.collect { showtimes ->
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
                            Toast.makeText(this@MovieDetailActivity, "Showtimes: $it", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    viewModel.detailError.collect { error ->
                        error?.let {
                            Toast.makeText(this@MovieDetailActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                launch {
                    viewModel.reviewActionSuccess.collect { success ->
                        if (success) {
                            Toast.makeText(this@MovieDetailActivity, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                            currentReviewDialog?.dismiss()
                            currentReviewDialog = null
                        } else {
                            Toast.makeText(this@MovieDetailActivity, "Failed to submit review.", Toast.LENGTH_SHORT).show()
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

    private fun showWriteReviewDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val dialogBinding = com.example.movie_booking_app.databinding.DialogWriteReviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnSubmitReview.setOnClickListener {
            val rating = dialogBinding.ratingBarReview.rating.toInt()
            val comment = dialogBinding.etComment.text.toString().trim()

            if (comment.length < 5 || comment.length > 1000) {
                dialogBinding.layoutComment.error = "Comment must be between 5 and 1000 characters."
                return@setOnClickListener
            }
            dialogBinding.layoutComment.error = null

            viewModel.submitReview(movieId, rating, comment)
            currentReviewDialog = dialog
        }

        dialog.show()
    }

}