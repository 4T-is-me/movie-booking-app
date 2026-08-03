package com.example.movie_booking_app.view.fragment.staff

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentStaffRatingBinding
import com.example.movie_booking_app.view.adapter.ReviewAdapter
import com.example.movie_booking_app.viewmodel.StaffRatingViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffRatingFragment : Fragment() {

    private var _binding: FragmentStaffRatingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StaffRatingViewModel by viewModels()
    private lateinit var adapter: ReviewAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffRatingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
        viewModel.loadRatings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        adapter = ReviewAdapter()
        binding.rvRatings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRatings.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadRatings()
        }
        
        binding.swipeRefresh.setColorSchemeResources(R.color.accent_primary)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { updateUI() }
                }

                launch {
                    viewModel.ratings.collect { list ->
                        android.util.Log.d("StaffRatingFragment", "Observer: received ${list.size} ratings")
                        adapter.submitList(list)
                        updateUI()
                    }
                }

                launch {
                    viewModel.totalRating.collect { avg ->
                        binding.tvAvgRating.text = String.format(Locale.getDefault(), "%.1f", avg)
                        binding.ratingBarSummary.rating = avg.toFloat()
                    }
                }

                launch {
                    viewModel.totalCount.collect { count ->
                        binding.tvTotalCount.text = getString(R.string.total_reviews_format, count)
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        errorMsg?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateUI() {
        val isLoading = viewModel.isLoading.value
        val list = viewModel.ratings.value

        if (isLoading) {
            // If we have no data yet, show shimmer. If we have data, show pull-to-refresh spinner.
            if (list.isEmpty()) {
                binding.shimmerContainer.visibility = View.VISIBLE
                binding.shimmerContainer.startShimmer()
                binding.rvRatings.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.GONE
            } else {
                binding.shimmerContainer.stopShimmer()
                binding.shimmerContainer.visibility = View.GONE
                binding.rvRatings.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        } else {
            binding.shimmerContainer.stopShimmer()
            binding.shimmerContainer.visibility = View.GONE
            binding.swipeRefresh.isRefreshing = false

            if (list.isEmpty()) {
                binding.rvRatings.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
            } else {
                binding.rvRatings.visibility = View.VISIBLE
                binding.layoutEmptyState.visibility = View.GONE
            }
        }
    }
}
