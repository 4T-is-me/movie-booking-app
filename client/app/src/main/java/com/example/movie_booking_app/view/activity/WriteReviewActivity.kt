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

        // Nhận dữ liệu từ Intent
        movieId    = intent.getStringExtra("movieId") ?: ""
        movieTitle = intent.getStringExtra("movieTitle") ?: ""
        moviePoster = intent.getStringExtra("moviePoster") ?: ""
        movieGenre = intent.getStringExtra("movieGenre") ?: ""

        if (movieId.isEmpty()) {
            Toast.makeText(this, "Movie ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupTextWatcher()
        setupRatingBar()
        observeViewModel()
    }

    private fun setupUI() {
        // Nút back
        binding.btnBack.setOnClickListener { finish() }

        // Hiển thị thông tin phim
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

        // Nút submit
        binding.btnSubmitReview.setOnClickListener {
            val rating  = binding.ratingBarReview.rating.toInt()
            val comment = binding.etComment.text.toString().trim()

            // Validate rating
            if (rating == 0) {
                Toast.makeText(this, "Please select a rating.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate comment
            if (comment.length < 5) {
                binding.tvCommentError.text = "Comment must be at least 5 characters."
                binding.tvCommentError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (comment.length > 1000) {
                binding.tvCommentError.text = "Comment must not exceed 1000 characters."
                binding.tvCommentError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            binding.tvCommentError.visibility = View.GONE

            // Hiển thị loading, ẩn nút submit
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
                // Ẩn lỗi khi người dùng đang nhập
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
                viewModel.reviewActionSuccess.collect { success ->
                    binding.progressBar.visibility = View.GONE
                    binding.btnSubmitReview.isEnabled = true

                    if (success) {
                        Toast.makeText(
                            this@WriteReviewActivity,
                            "Review submitted successfully! 🎉",
                            Toast.LENGTH_LONG
                        ).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(
                            this@WriteReviewActivity,
                            "Failed to submit review. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}
