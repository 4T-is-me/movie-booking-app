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

import android.content.Intent
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        setupListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPromotions()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminPromotionAdapter(
            onEditClick = { promotion ->
                val intent = Intent(this, AdminEditPromotionActivity::class.java).apply {
                    putExtra("promotion", promotion)
                }
                startActivity(intent)
            },
            onDeleteClick = { promotion ->
                showDeleteConfirmationDialog(promotion)
            }
        )
        binding.rvAdminPromotions.layoutManager = LinearLayoutManager(this)
        binding.rvAdminPromotions.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddPromotion.setOnClickListener {
            val intent = Intent(this, AdminEditPromotionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showDeleteConfirmationDialog(promotion: AdminPromotion) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Xóa Khuyến Mãi")
            .setMessage("Bạn có chắc chắn muốn xóa chương trình khuyến mãi \"${promotion.title}\" không?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deletePromotion(promotion.id)
            }
            .show()
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionSuccess.collectLatest { success ->
                    if (success) {
                        viewModel.loadPromotions()
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionMessage.collectLatest { message ->
                    Toast.makeText(this@AdminPromotionActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
