package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityWriteReviewBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.viewmodel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WriteReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWriteReviewBinding
    private val viewModel: MovieViewModel by viewModels()

    private var movieId: String = ""
    private var movieTitle: String = ""
    private var moviePoster: String = ""
    private var movieGenre: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get data from Intent
        movieId    = intent.getStringExtra("movieId") ?: ""
        movieTitle = intent.getStringExtra("movieTitle") ?: ""
        moviePoster = intent.getStringExtra("moviePoster") ?: ""
        movieGenre = intent.getStringExtra("movieGenre") ?: ""

        if (movieId.isEmpty()) {
            Toast.makeText(this, getString(R.string.auto_str_movie_id_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupTextWatcher()
        setupRatingBar()
        observeViewModel()
    }

    private fun setupUI() {
        // Back button
        binding.btnBack.setOnClickListener { finish() }

        // Display movie info
        binding.tvMovieTitle.text = movieTitle.ifEmpty { "Unknown" }
        binding.tvMovieGenre.text = movieGenre.ifEmpty { "" }

        // Load poster
        val fullUrl = when {
            moviePoster.isEmpty() -> ""
            moviePoster.startsWith("http") -> moviePoster
            else -> "${Constants.BASE_URL.removeSuffix("/")}/${moviePoster.removePrefix("/")}"
        }
        Glide.with(this)
            .load(fullUrl.ifEmpty { null })
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .into(binding.ivMoviePoster)

        // Submit button
        binding.btnSubmitReview.setOnClickListener {
            val rating  = binding.ratingBarReview.rating.toInt()
            val comment = binding.etComment.text.toString().trim()

            // Validate rating
            if (rating == 0) {
                Toast.makeText(this, getString(R.string.auto_str_please_select_a_rating), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate comment
            if (comment.length < 5) {
                binding.tvCommentError.text = getString(R.string.auto_str_comment_must_be_at_least_5_cha)
                binding.tvCommentError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (comment.length > 1000) {
                binding.tvCommentError.text = getString(R.string.auto_str_comment_must_not_exceed_1000_c)
                binding.tvCommentError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.tvCommentError.visibility = View.GONE

            // Show loading, hide submit button
            binding.progressBar.visibility = View.VISIBLE
            binding.btnSubmitReview.isEnabled = false

            viewModel.submitReview(movieId, rating, comment)
        }
    }

    private fun setupTextWatcher() {
        binding.etComment.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                binding.tvCharCount.text = "$len / 1000"
                // Hide error when user is typing
                if (len >= 5) binding.tvCommentError.visibility = View.GONE
            }
        })
    }

    private fun setupRatingBar() {
        val ratingLabels = mapOf(
            1f to "😞 Terrible",
            2f to "😐 Poor",
            3f to "🙂 Average",
            4f to "😊 Good",
            5f to "🤩 Excellent"
        )
        binding.ratingBarReview.setOnRatingBarChangeListener { _, rating, _ ->
            binding.tvRatingLabel.text = ratingLabels[rating] ?: "Tap to rate"
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.reviewActionSuccess.collect { success ->
                        binding.progressBar.visibility = View.GONE
                        binding.btnSubmitReview.isEnabled = true

                        if (success) {
                            Toast.makeText(
                                this@WriteReviewActivity,
                                getString(R.string.auto_str_review_submitted_successfully),
                                Toast.LENGTH_LONG
                            ).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                }

                launch {
                    viewModel.reviewErrorMessage.collect { errorMsg ->
                        if (errorMsg != null) {
                            binding.progressBar.visibility = View.GONE
                            binding.btnSubmitReview.isEnabled = true
                            Toast.makeText(
                                this@WriteReviewActivity,
                                errorMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
