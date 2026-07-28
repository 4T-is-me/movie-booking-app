package com.example.movie_booking_app.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.movie_booking_app.R
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.databinding.FragmentPurchaseHistoryBinding
import com.example.movie_booking_app.view.activity.BookingDetailActivity
import com.example.movie_booking_app.view.activity.WriteReviewActivity
import com.example.movie_booking_app.view.adapter.PurchaseHistoryAdapter
import com.example.movie_booking_app.viewmodel.PurchaseHistoryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PurchaseHistoryFragment : Fragment() {

    private var _binding: FragmentPurchaseHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PurchaseHistoryViewModel by viewModels()
    private lateinit var adapter: PurchaseHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPurchaseHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadHistory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            activity?.finish()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory()
        }

        adapter = PurchaseHistoryAdapter(
            onItemClick = { item ->
                val intent = Intent(requireContext(), BookingDetailActivity::class.java).apply {
                    putExtra("BOOKING_ID", item.bookingId)
                }
                startActivity(intent)
            },
            onRateStaffClick = { item ->
                showRateStaffDialog(item.bookingId)
            },
            onRateMovieClick = { item ->
                val intent = Intent(requireContext(), WriteReviewActivity::class.java).apply {
                    putExtra("movieId", item.movieId)
                    putExtra("movieTitle", item.movie)
                    putExtra("moviePoster", item.poster)
                    putExtra("movieGenre", item.movieGenre?.joinToString(", ") ?: "")
                }
                startActivityForResult(intent, REQUEST_WRITE_REVIEW)
            }
        )
        binding.rvPurchaseHistory.adapter = adapter

        // Retry reload
        binding.btnRetry.setOnClickListener {
            viewModel.loadHistory()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        binding.shimmerContainer.visibility = View.VISIBLE
                        binding.shimmerContainer.startShimmer()
                        binding.rvPurchaseHistory.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.layoutErrorState.visibility = View.GONE
                    } else {
                        binding.shimmerContainer.stopShimmer()
                        binding.shimmerContainer.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        
                        if (viewModel.error.value == null) {
                            val list = viewModel.bookings.value
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
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bookings.collect { list ->
                    if (!viewModel.isLoading.value && viewModel.error.value == null) {
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
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        binding.layoutErrorState.visibility = View.VISIBLE
                        binding.tvErrorMsg.text = it
                        binding.rvPurchaseHistory.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rateSuccess.collect { success ->
                    if (success) {
                        Toast.makeText(requireContext(), getString(R.string.rating_submitted_success), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showRateStaffDialog(bookingId: String) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_rate_staff, null)
        dialog.setContentView(dialogView)

        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val etComment = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etComment)
        val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitRating)

        btnSubmit.setOnClickListener {
            val rating = ratingBar.rating.toInt()
            val comment = etComment.text?.toString()?.trim()
            if (rating > 0) {
                viewModel.rateStaff(bookingId, rating, comment)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_rating), Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_WRITE_REVIEW && resultCode == android.app.Activity.RESULT_OK) {
            viewModel.loadHistory()
        }
    }

    companion object {
        private const val REQUEST_WRITE_REVIEW = 101
    }
}
