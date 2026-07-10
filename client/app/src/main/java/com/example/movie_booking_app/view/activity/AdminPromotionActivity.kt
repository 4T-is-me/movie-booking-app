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
import androidx.core.widget.doAfterTextChanged
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class AdminPromotionActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminPromotionBinding
    private val viewModel: AdminPromotionViewModel by viewModels()
    private lateinit var adapter: AdminPromotionAdapter
    private var allPromotions: List<AdminPromotion> = emptyList()

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

        binding.etSearch.doAfterTextChanged {
            filterAndSubmit()
        }

        binding.tabLayoutFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterAndSubmit()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showDeleteConfirmationDialog(promotion: AdminPromotion) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_confirm_delete_promotion)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDeleteMessage = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDeleteMessage)
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnCancelDelete)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnConfirmDelete)

        tvDeleteMessage.text = "Are you sure you want to delete the promotion \"${promotion.title}\"?"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            viewModel.deletePromotion(promotion.id)
        }

        dialog.show()
    }

    private fun parseIsoDate(dateStr: String?): Date? {
        if (dateStr.isNullOrEmpty()) return null
        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd"
        )
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                return sdf.parse(dateStr)
            } catch (e: Exception) {
                // Try next format
            }
        }
        return null
    }

    private fun updateStatistics(promotions: List<AdminPromotion>) {
        val now = Date()
        val total = promotions.size
        var active = 0
        var inactive = 0

        for (promo in promotions) {
            val start = parseIsoDate(promo.startDate)
            val end = parseIsoDate(promo.endDate)

            val isActive = promo.isDeleted != true && (start == null || !now.before(start)) && (end == null || !now.after(end))
            if (isActive) {
                active++
            } else {
                inactive++
            }
        }

        binding.tvStatTotal.text = total.toString()
        binding.tvStatActive.text = active.toString()
        binding.tvStatInactive.text = inactive.toString()
    }

    private fun filterAndSubmit() {
        val now = Date()
        val query = binding.etSearch.text.toString().trim().lowercase(Locale.getDefault())
        val selectedTab = binding.tabLayoutFilter.selectedTabPosition

        val filtered = allPromotions.filter { promo ->
            // Filter by search query
            val matchesQuery = promo.title.lowercase(Locale.getDefault()).contains(query) ||
                    (promo.couponCode?.lowercase(Locale.getDefault())?.contains(query) == true)

            // Filter by tab
            val start = parseIsoDate(promo.startDate)
            val end = parseIsoDate(promo.endDate)
            val isActive = promo.isDeleted != true && (start == null || !now.before(start)) && (end == null || !now.after(end))

            val matchesTab = when (selectedTab) {
                0 -> isActive // Active
                1 -> !isActive // Inactive
                else -> true
            }

            matchesQuery && matchesTab
        }

        if (filtered.isEmpty()) {
            binding.rvAdminPromotions.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvAdminPromotions.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            adapter.submitList(filtered)
        }
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

                    // Handle list and stats updates
                    if (!state.isLoading) {
                        allPromotions = state.promotions
                        updateStatistics(allPromotions)
                        filterAndSubmit()
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
