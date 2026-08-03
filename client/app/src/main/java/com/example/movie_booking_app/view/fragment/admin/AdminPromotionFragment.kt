package com.example.movie_booking_app.view.fragment.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.AdminPromotion
import com.example.movie_booking_app.databinding.DialogConfirmDeletePromotionBinding
import com.example.movie_booking_app.databinding.FragmentAdminPromotionBinding
import com.example.movie_booking_app.view.activity.AdminEditPromotionActivity
import com.example.movie_booking_app.view.adapter.AdminPromotionAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminPromotionViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminPromotionFragment : Fragment() {

    private var _binding: FragmentAdminPromotionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminPromotionViewModel by viewModels()
    private lateinit var adapter: AdminPromotionAdapter
    private var allPromotions: List<AdminPromotion> = emptyList()
    private var selectedTab = TAB_ACTIVE

    companion object {
        private const val TAB_ACTIVE = "active"
        private const val TAB_INACTIVE = "inactive"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminPromotionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        setupTabs()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadPromotions()
    }

    private fun setupRecyclerView() {
        adapter = AdminPromotionAdapter(
            onEditClick = { promotion ->
                val intent = Intent(requireContext(), AdminEditPromotionActivity::class.java).apply {
                    putExtra("promotion", promotion)
                }
                startActivity(intent)
            },
            onDeleteClick = { promotion ->
                showDeleteConfirmationDialog(promotion)
            }
        )
        binding.rvAdminPromotions.layoutManager = LinearLayoutManager(context)
        binding.rvAdminPromotions.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddPromotion.setOnClickListener {
            val intent = Intent(requireContext(), AdminEditPromotionActivity::class.java)
            startActivity(intent)
        }

        binding.etSearch.doAfterTextChanged {
            filterAndSubmit()
        }
    }

    private fun setupTabs() {
        binding.tabActivePromotion.setOnClickListener { switchTab(TAB_ACTIVE) }
        binding.tabInactivePromotion.setOnClickListener { switchTab(TAB_INACTIVE) }
    }

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab

        val isActive = (tab == TAB_ACTIVE)
        updateTabStyle(binding.tabActivePromotion, isActive)
        updateTabStyle(binding.tabInactivePromotion, !isActive)

        filterAndSubmit()
    }

    private fun updateTabStyle(tabView: TextView, isActive: Boolean) {
        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_tab_active)
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
        }
    }

    private fun showDeleteConfirmationDialog(promotion: AdminPromotion) {
        val dialogBinding = DialogConfirmDeletePromotionBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        dialogBinding.tvDeleteMessage.text = "${getString(R.string.auto_str_are_you_sure_you_want_to_delet)} \"${promotion.title}\"?"

        dialogBinding.btnCancelDelete.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmDelete.setOnClickListener {
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
        val isActiveTab = (selectedTab == TAB_ACTIVE)

        val filtered = allPromotions.filter { promo ->
            // Filter by search query
            val matchesQuery = promo.title.lowercase(Locale.getDefault()).contains(query) ||
                    (promo.couponCode?.lowercase(Locale.getDefault())?.contains(query) == true)

            // Filter by tab
            val start = parseIsoDate(promo.startDate)
            val end = parseIsoDate(promo.endDate)
            val isActive = promo.isDeleted != true && (start == null || !now.before(start)) && (end == null || !now.after(end))

            val matchesTab = (isActive == isActiveTab)

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // Handle loading indicator
                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    // Handle error message
                    state.error?.let { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionSuccess.collectLatest { success ->
                    if (success) {
                        viewModel.loadPromotions()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionMessage.collectLatest { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
