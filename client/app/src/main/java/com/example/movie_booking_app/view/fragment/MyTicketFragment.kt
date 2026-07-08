package com.example.movie_booking_app.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.databinding.FragmentMyTicketBinding
import com.example.movie_booking_app.view.activity.BookingDetailActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.adapter.MyTicketAdapter
import com.example.movie_booking_app.viewmodel.MyTicketViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyTicketFragment : Fragment() {

    private var _binding: FragmentMyTicketBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyTicketViewModel by viewModels()
    private lateinit var adapter: MyTicketAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadActiveTickets()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadActiveTickets()
        }

        adapter = MyTicketAdapter { item ->
            val intent = Intent(requireContext(), BookingDetailActivity::class.java).apply {
                putExtra("BOOKING_ID", item.bookingId)
            }
            startActivity(intent)
        }
        binding.rvActiveTickets.adapter = adapter

        binding.btnBookMovie.setOnClickListener {
            (activity as? MainActivity)?.navigateToSection("Home")
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadActiveTickets()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        binding.shimmerContainer.visibility = View.VISIBLE
                        binding.shimmerContainer.startShimmer()
                        binding.rvActiveTickets.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.GONE
                        binding.layoutErrorState.visibility = View.GONE
                    } else {
                        binding.shimmerContainer.stopShimmer()
                        binding.shimmerContainer.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        
                        if (viewModel.error.value == null) {
                            val list = viewModel.tickets.value
                            if (list.isEmpty()) {
                                binding.layoutEmptyState.visibility = View.VISIBLE
                                binding.rvActiveTickets.visibility = View.GONE
                            } else {
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvActiveTickets.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tickets.collect { list ->
                    if (!viewModel.isLoading.value) {
                        adapter.updateData(list)
                        if (viewModel.error.value == null) {
                            if (list.isEmpty()) {
                                binding.layoutEmptyState.visibility = View.VISIBLE
                                binding.rvActiveTickets.visibility = View.GONE
                            } else {
                                binding.layoutEmptyState.visibility = View.GONE
                                binding.rvActiveTickets.visibility = View.VISIBLE
                            }
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
                        binding.rvActiveTickets.visibility = View.GONE
                        binding.layoutEmptyState.visibility = View.GONE
                    }
                }
            }
        }
    }
}
