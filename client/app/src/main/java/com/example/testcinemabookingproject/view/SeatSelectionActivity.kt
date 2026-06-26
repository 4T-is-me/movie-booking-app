package com.example.testcinemabookingproject.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.testcinemabookingproject.adapter.SeatAdapter
import com.example.testcinemabookingproject.databinding.ActivitySeatSelectionBinding
import com.example.testcinemabookingproject.viewmodel.SeatViewModel

/**
 * SEAT SELECTION ACTIVITY: "Phòng điều hành" của rạp phim.
 */
class SeatSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeatSelectionBinding
    private val viewModel: SeatViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySeatSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val seatAdapter = SeatAdapter(listOf()) { seat ->
            viewModel.toggleSeat(seat)
        }

        binding.rvSeats.apply {
            layoutManager = GridLayoutManager(this@SeatSelectionActivity, 8)
            adapter = seatAdapter
        }

        viewModel.seats.observe(this) { updatedSeats ->
            seatAdapter.updateSeats(updatedSeats)
        }
        
        viewModel.selectedCount.observe(this) { count ->
            binding.tvSelectedCount.text = "Đã chọn: $count ghế"
        }

        viewModel.totalPrice.observe(this) { price ->
            binding.tvTotalPrice.text = "Tổng: ${price}đ"
        }

        binding.btnConfirm.setOnClickListener {
            if (viewModel.selectedCount.value ?: 0 > 0) {
                val intent = Intent(this, BookingSuccessActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ghế!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
