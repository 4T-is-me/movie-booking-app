package com.example.movie_booking_app.view.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivityAdminPromotionBinding
import com.example.movie_booking_app.view.adapter.AdminPromotionAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminPromotionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminPromotionActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminPromotionBinding
    private val viewModel: AdminPromotionViewModel by viewModels()
    private lateinit var adapter: AdminPromotionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPromotionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminPromotionAdapter()
        binding.rvAdminPromotions.layoutManager = LinearLayoutManager(this)
        binding.rvAdminPromotions.adapter = adapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // Handle loading indicator
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Handle error message
                    state.error?.let { err ->
                        Toast.makeText(this@AdminPromotionActivity, err, Toast.LENGTH_SHORT).show()
                    }

                    // Handle list visibility and contents
                    if (!state.isLoading) {
                        if (state.promotions.isEmpty()) {
                            binding.rvAdminPromotions.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE
                        } else {
                            binding.rvAdminPromotions.visibility = View.VISIBLE
                            binding.layoutEmpty.visibility = View.GONE
                            adapter.submitList(state.promotions)
                        }
                    } else {
                        binding.rvAdminPromotions.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                    }
                }
            }
        }
    }
}
