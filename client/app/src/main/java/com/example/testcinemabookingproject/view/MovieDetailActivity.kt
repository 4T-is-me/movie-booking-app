package com.example.testcinemabookingproject.view

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testcinemabookingproject.databinding.ActivityMovieDetailBinding

/**
 * MOVIE DETAIL ACTIVITY: Màn hình hiện ra khi bạn click vào một bộ phim.
 */
class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieName = intent.getStringExtra(EXTRA_NAME)
        val movieRating = intent.getDoubleExtra(EXTRA_RATING, 0.0)

        // Đổ dữ liệu vào giao diện XML
        binding.tvDetailTitle.text = movieName
        binding.tvDetailRating.text = "Đánh giá: $movieRating ⭐"

        // --- BƯỚC NỐI DÂY: Gắn logic cho nút Đặt vé ---
        binding.btnBookTicket.setOnClickListener {
            val intent = Intent(this, SeatSelectionActivity::class.java)
            startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_NAME = "MOVIE_NAME"
        const val EXTRA_RATING = "MOVIE_RATING"
    }
}
