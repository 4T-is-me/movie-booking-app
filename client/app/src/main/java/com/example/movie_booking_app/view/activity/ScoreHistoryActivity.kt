package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivityScoreHistoryBinding
import com.example.movie_booking_app.view.adapter.ScoreHistoryAdapter
import com.example.movie_booking_app.viewmodel.BookingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ScoreHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScoreHistoryBinding
    private val viewModel: BookingViewModel by viewModels()
    private lateinit var historyAdapter: ScoreHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScoreHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        observeViewModel()

        viewModel.fetchScoreHistory()
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        historyAdapter = ScoreHistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@ScoreHistoryActivity)
            adapter = historyAdapter
            isNestedScrollingEnabled = false
        }
    }


    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // loading state to display the progress bar
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbLoadMore.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }

                // Retrieve and display the full list.
                launch {
                    viewModel.scoreHistoryState.collect { historyList ->
                        val historyList = historyList.orEmpty()
                        if (historyList.isNotEmpty()) {
                            binding.nestedScrollView.visibility = View.VISIBLE
                            binding.layoutEmpty.visibility = View.GONE
                            historyAdapter.updateData(historyList)
                        }else{
                            binding.nestedScrollView.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                        }
                    }
                }

                // Display error message
                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(this@ScoreHistoryActivity, it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}