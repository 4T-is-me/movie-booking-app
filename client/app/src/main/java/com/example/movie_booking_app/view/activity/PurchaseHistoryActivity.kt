package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.databinding.ActivityPurchaseHistoryBinding
import com.example.movie_booking_app.view.adapter.PurchaseHistoryAdapter
import com.example.movie_booking_app.viewmodel.PurchaseHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PurchaseHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPurchaseHistoryBinding
    private val viewModel: PurchaseHistoryViewModel by viewModels()
    private lateinit var adapter: PurchaseHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPurchaseHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        observeViewModel()

        viewModel.loadHistory()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory()
        }

        adapter = PurchaseHistoryAdapter()
        binding.rvPurchaseHistory.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.swipeRefresh.isRefreshing = isLoading
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookings.collect { list ->
                    adapter.updateData(list)
                    if (list.isEmpty()) {
                        binding.layoutEmptyState.visibility = View.VISIBLE
                        binding.rvPurchaseHistory.visibility = View.GONE
                    } else {
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.rvPurchaseHistory.visibility = View.VISIBLE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(this@PurchaseHistoryActivity, it, Toast.LENGTH_LONG).show()
                        viewModel.resetError()
                    }
                }
            }
        }
    }
}
