package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private val viewModel: MovieViewModel by viewModels()

    private lateinit var movieId: String
    private var selectedCinema: Cinema? = null

    private val reviewAdapter by lazy { ReviewAdapter(emptyList()) }
    private val directorAdapter by lazy { CardItemAdapter() }
    private val actorsAdapter by lazy { CardItemAdapter() }

    private var isReviewsExpanded = false
    private var isDescriptionExpanded = false
    private var currentReviewsList = emptyList<com.example.movie_booking_app.data.model.response.ReviewItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        movieId = intent.getStringExtra("movieId") ?: ""
        if (movieId.isEmpty()) {
            Toast.makeText(this, getString(R.string.auto_str_movie_id_invalid), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupViews()
        setupAdapters()
        refreshData()
        observeMovie()
    }

    private fun setupViews() {
        binding.ivBack.setOnClickListener { finish() }

        binding.tvSeeAllReviews.setOnClickListener {
            isReviewsExpanded = !isReviewsExpanded
            updateReviewsDisplay()
        }

        binding.btnBookTicket.setOnClickListener {
            val movie = viewModel.movieDetail.value?.data
            if (movie?.status == "coming-soon") {
                Toast.makeText(this, getString(R.string.auto_str_this_movie_is_coming_soon), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val cinema = selectedCinema
            if (cinema == null) {
                Toast.makeText(this, getString(R.string.auto_str_please_select_a_cinema_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, SelectSeatActivity::class.java).apply {
                putExtra("movieId", movieId)
                putExtra("cinemaId", cinema.id)
                putExtra("movieName", movie?.movieName ?: "")
                putExtra("moviePoster", movie?.poster ?: "")
                putExtra("movieGenre", movie?.movieGenre?.joinToString(", ") ?: "")
                putExtra("cinemaName", cinema.cinemaName ?: "")
                putExtra("cinemaAddress", cinema.address ?: "")
            }
            startActivity(intent)
        }

        // Setup SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener { refreshData() }
        binding.swipeRefresh.setColorSchemeResources(
            android.R.color.holo_orange_light,
            android.R.color.black
        )
    }

    private fun setupAdapters() {
        binding.rvReviews.adapter = reviewAdapter

        binding.rvDirector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvDirector.adapter = directorAdapter

        binding.rvActors.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvActors.adapter = actorsAdapter
    }

    private fun refreshData() {
        binding.swipeRefresh.isRefreshing = true
        viewModel.fetchMovieDetail(movieId)
        viewModel.fetchMovieShowtimes(movieId)
        viewModel.fetchMovieReviews(movieId)
    }

    private fun observeMovie() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    combine(
                        viewModel.movieDetail,
                        viewModel.movieShowtimes,
                        viewModel.movieReviews
                    ) { detail, showtimes, reviews ->
                        detail != null
                    }.collect { isLoaded ->
                        if (isLoaded) binding.swipeRefresh.isRefreshing = false
                    }
                }

                // Movie details
                launch {
                    viewModel.movieDetail.collect { response ->
                        response?.data?.let { movie ->
                            val descriptionText = movie.description ?: ""

                            binding.txtTitle.text = movie.movieName ?: "Unknown"
                            binding.txtDescription.text = descriptionText
                            binding.txtDescription.post {

                                if (binding.txtDescription.lineCount > 3) {
                                    binding.tvSeeMoreDescription.visibility = View.VISIBLE

                                    binding.tvSeeMoreDescription.setOnClickListener {
                                        isDescriptionExpanded = !isDescriptionExpanded
                                        if (isDescriptionExpanded) {
                                            binding.txtDescription.maxLines =
                                                Integer.MAX_VALUE
                                            binding.tvSeeMoreDescription.text = getString(R.string.auto_str_see_less)
                                        } else {
                                            binding.txtDescription.maxLines = 2
                                            binding.tvSeeMoreDescription.text = getString(R.string.auto_str_see_more)
                                        }
                                    }
                                } else {
                                    binding.tvSeeMoreDescription.visibility = View.GONE
                                }
                            }
                            binding.txtGenre.text = movie.movieGenre?.joinToString(", ") ?: ""
                            binding.txtLanguage.text = movie.language ?: ""
                            binding.txtDuration.text = formatDuration(movie.duration)
                            binding.txtReleaseDate.text = com.example.movie_booking_app.utils.DateUtils.formatMovieDate(movie.releaseDate)
                            binding.txtRating.text = movie.rating?.toString() ?: "Unrated"
                            binding.txtVote.text = "(${movie.totalReviews ?: 0})"

                            // Update rating summary section
                            val rating = movie.rating ?: 0.0
                            binding.tvAvgRating.text = String.format("%.1f", rating)
                            binding.ratingBarSummary.rating = rating.toFloat()
                            val total = movie.totalReviews ?: 0
                            binding.tvReviewCount.text = "$total review${if (total != 1) "s" else ""}"
                            binding.layoutRatingSummary.visibility = if (total > 0) View.VISIBLE else View.GONE

                            movie.director?.let { directorAdapter.submitList(it) }
                            movie.actors?.let { actorsAdapter.submitList(it) }

                            val posterUrl = movie.poster
                            val fullUrl = if (posterUrl?.startsWith("http") == true) posterUrl
                            else "${com.example.movie_booking_app.utils.Constants.BASE_URL}${posterUrl?.removePrefix("/")}"

                            Glide.with(this@MovieDetailActivity)
                                .load(fullUrl)
                                .placeholder(android.R.drawable.ic_menu_gallery)
                                .error(android.R.drawable.stat_notify_error)
                                .into(binding.imgPoster)

                             binding.btnWriteReview.visibility = View.GONE
                        }
                    }
                }

                // Showtimes & Cinema
                launch {
                    viewModel.movieShowtimes.collect { showtimes ->
                        if (showtimes.isEmpty()) {
                            binding.rvCinema.visibility = View.GONE
                            binding.tvNoCinema.visibility = View.VISIBLE
                            return@collect
                        }

                        binding.rvCinema.visibility = View.VISIBLE
                        binding.tvNoCinema.visibility = View.GONE

                        val cinemas = showtimes
                            .mapNotNull { it.cinemaId }
                            .distinctBy { it.id ?: it.cinemaName }

                        if (cinemas.isNotEmpty()) {
                            binding.rvCinema.layoutManager = LinearLayoutManager(this@MovieDetailActivity, LinearLayoutManager.VERTICAL, false)
                            binding.rvCinema.adapter = CinemaAdapter(cinemas) { cinema ->
                                selectedCinema = cinema
                            }
                        }
                    }
                }

                // Listen to review list
                launch {
                    viewModel.movieReviews.collect { reviews ->
                        currentReviewsList = reviews
                        updateReviewsDisplay()
                    }
                }

                // Error handling
                launch {
                    viewModel.detailError.collect { error ->
                        error?.let { Toast.makeText(this@MovieDetailActivity, it, Toast.LENGTH_SHORT).show() }
                    }
                }
                launch {
                    viewModel.showtimeError.collect { error ->
                        error?.let { Toast.makeText(this@MovieDetailActivity, "Showtimes: $it", Toast.LENGTH_SHORT).show() }
                    }
                }

                // Successfully submitted a review
                launch {
                    viewModel.reviewActionSuccess.collect { success ->
                        if (success) refreshData()
                    }
                }
            }
        }
    }

    private fun updateReviewsDisplay() {
        if (currentReviewsList.isEmpty()) {
            binding.tvNoReviews.visibility = View.VISIBLE
            binding.rvReviews.visibility = View.GONE
            binding.tvSeeAllReviews.visibility = View.GONE
        } else {
            binding.tvNoReviews.visibility = View.GONE
            binding.rvReviews.visibility = View.VISIBLE
            if (currentReviewsList.size > 2) {
                binding.tvSeeAllReviews.visibility = View.VISIBLE
                if (isReviewsExpanded) {
                    binding.tvSeeAllReviews.text = getString(R.string.auto_str_show_less)
                    reviewAdapter.submitList(currentReviewsList)
                } else {
                    binding.tvSeeAllReviews.text = getString(R.string.auto_str_see_all)
                    reviewAdapter.submitList(currentReviewsList.take(2))
                }
            } else {
                binding.tvSeeAllReviews.visibility = View.GONE
                reviewAdapter.submitList(currentReviewsList)
            }
        }
    }

    private fun formatDuration(minutes: Int?): String {
        if (minutes == null || minutes <= 0) return getString(com.example.movie_booking_app.R.string.duration_format_m, 0)
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> getString(com.example.movie_booking_app.R.string.duration_format_hm, h, m)
            h > 0 -> getString(com.example.movie_booking_app.R.string.duration_format_h, h)
            else -> getString(com.example.movie_booking_app.R.string.duration_format_m, m)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_REVIEW && resultCode == RESULT_OK) {
            refreshData()
        }
    }

    companion object {
        private const val REQUEST_WRITE_REVIEW = 1001
    }
}