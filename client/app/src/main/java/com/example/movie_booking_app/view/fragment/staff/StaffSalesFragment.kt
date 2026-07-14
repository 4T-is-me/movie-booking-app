package com.example.movie_booking_app.view.fragment.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentStaffSalesBinding
import com.example.movie_booking_app.view.adapter.SeatAdapter
import com.example.movie_booking_app.view.adapter.StaffMovieAdapter
import com.example.movie_booking_app.view.adapter.TimeAdapter
import com.example.movie_booking_app.viewmodel.staff.StaffSalesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class StaffSalesFragment : Fragment() {

    private var _binding: FragmentStaffSalesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StaffSalesViewModel by viewModels()

    private lateinit var movieAdapter: StaffMovieAdapter
    private lateinit var timeAdapter: TimeAdapter
    private lateinit var seatAdapter: SeatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffSalesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupListeners()
        observeViewModel()

        viewModel.loadMovies()
    }

    private fun setupAdapters() {
        // Movies Grid Adapter
        movieAdapter = StaffMovieAdapter { movie ->
            viewModel.selectMovie(movie)
        }
        binding.rvMoviesGrid.adapter = movieAdapter

        // Showtime Adapter
        timeAdapter = TimeAdapter { showtime ->
            viewModel.loadSeats(showtime.id)
        }
        binding.rvShowtimes.adapter = timeAdapter

        // Seat Adapter
        seatAdapter = SeatAdapter { seat ->
            viewModel.toggleSeatSelection(seat.id)
        }
        binding.rvSeatMap.adapter = seatAdapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            viewModel.selectMovie(null)
        }
    }

    private fun observeViewModel() {
        // Observe Loading State
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        // Observe Movies List
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movies.collectLatest { movies ->
                movieAdapter.submitList(movies)
            }
        }

        // Observe Selected Movie to switch views
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedMovie.collectLatest { selectedMovie ->
                if (selectedMovie != null) {
                    // Show seat selection view, hide movie grid
                    binding.rvMoviesGrid.visibility = View.GONE
                    binding.layoutSeatSelection.visibility = View.VISIBLE
                    binding.btnBack.visibility = View.VISIBLE

                    // Bind movie details
                    binding.tvSelectedMovieTitle.text = selectedMovie.movieName
                    val genreStr = selectedMovie.movieGenre?.joinToString(", ") ?: ""
                    val durationStr = if (selectedMovie.duration != null) "${selectedMovie.duration}m" else "N/A"
                    binding.tvSelectedMovieInfo.text = "$genreStr | $durationStr"
                    binding.tvSelectedMovieRating.text = String.format("%.1f", selectedMovie.rating ?: 0.0)

                    Glide.with(requireContext())
                        .load(selectedMovie.poster)
                        .placeholder(R.color.card_dark)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(binding.imgSelectedMoviePoster)
                } else {
                    // Show movie grid, hide seat selection
                    binding.rvMoviesGrid.visibility = View.VISIBLE
                    binding.layoutSeatSelection.visibility = View.GONE
                    binding.btnBack.visibility = View.GONE
                }
            }
        }

        // Observe Showtimes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showtimes.collectLatest { showtimes ->
                timeAdapter.submitList(showtimes)
                movieAdapter.setSelectedMovie(viewModel.selectedMovie.value?.id)
            }
        }

        // Observe Seats
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seats.collectLatest { seatList ->
                if (seatList.isNotEmpty()) {
                    val maxCol = seatList.maxOfOrNull { it.number } ?: 10
                    binding.rvSeatMap.layoutManager = GridLayoutManager(requireContext(), maxCol)
                    seatAdapter.submitList(seatList)

                    binding.svSeatMapContainer.visibility = View.VISIBLE
                    binding.tvScreenLabel.visibility = View.VISIBLE
                    binding.viewScreenLine.visibility = View.VISIBLE
                    binding.layoutLegends.visibility = View.VISIBLE
                    binding.layoutSummary.visibility = View.VISIBLE
                } else {
                    binding.svSeatMapContainer.visibility = View.GONE
                    binding.tvScreenLabel.visibility = View.GONE
                    binding.viewScreenLine.visibility = View.GONE
                    binding.layoutLegends.visibility = View.GONE
                    binding.layoutSummary.visibility = View.GONE
                }
            }
        }

        // Observe Selected Seats and Price to enable/disable Next button
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedSeats.collectLatest { selected ->
                if (selected.isNotEmpty()) {
                    val seatLabels = selected.joinToString(", ") { it.label }
                    binding.tvSelectedSeats.text = "Ghế đã chọn: $seatLabels"
                    binding.btnNext.isEnabled = true
                    binding.btnNext.alpha = 1.0f
                } else {
                    binding.tvSelectedSeats.text = "Ghế đã chọn: Chưa chọn"
                    binding.btnNext.isEnabled = false
                    binding.btnNext.alpha = 0.5f
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalPrice.collectLatest { price ->
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(price)
                binding.tvTotalPrice.text = "Tạm tính: $formattedPrice"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
