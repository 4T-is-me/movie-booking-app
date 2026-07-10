package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.FragmentShowtimeSelectionBinding
import com.example.movie_booking_app.view.adapter.DateAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.viewmodel.SelectSeatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShowtimeSelectionFragment : Fragment() {

    private var _binding: FragmentShowtimeSelectionBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SelectSeatViewModel by activityViewModels()
    
    private lateinit var dateAdapter: DateAdapter
    private lateinit var timeAdapter: TimeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShowtimeSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        dateAdapter = DateAdapter { date ->
            viewModel.selectDate(date.id)
        }
        binding.rvDates.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }

        timeAdapter = TimeAdapter { showtime ->
            viewModel.selectTime(showtime.id)
        }
        binding.rvTimes.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = timeAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dates.collect { list ->
                    dateAdapter.submitList(list)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.times.collect { list ->
                    timeAdapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
