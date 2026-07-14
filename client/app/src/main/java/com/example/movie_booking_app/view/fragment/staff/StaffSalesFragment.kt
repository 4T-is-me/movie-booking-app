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
            if (viewModel.currentStep.value > 1) {
                viewModel.prevStep()
            } else {
                viewModel.selectMovie(null)
            }
        }

        binding.btnNext.setOnClickListener {
            val step = viewModel.currentStep.value
            if (step == 1) {
                // Populate order details for step 2
                val movie = viewModel.selectedMovie.value
                val selectedShowtime = viewModel.showtimes.value.find { it.isSelected }
                val showtimeStr = selectedShowtime?.showTime ?: "N/A"
                val dateStr = selectedShowtime?.showDate ?: "N/A"
                val seatLabels = viewModel.selectedSeats.value.joinToString(", ") { it.label }
                val price = viewModel.totalPrice.value
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                val formattedPrice = formatter.format(price)

                binding.tvOrderMovieTitle.text = movie?.movieName ?: "N/A"
                binding.tvOrderDetails.text = "Suất chiếu: $showtimeStr • Ngày: $dateStr • Ghế đã chọn: $seatLabels"
                binding.tvOrderPrice.text = "Tạm tính: $formattedPrice"

                viewModel.nextStep()
            } else if (step == 2) {
                viewModel.nextStep()
            }
        }

        binding.btnVerifyMember.setOnClickListener {
            val identity = binding.edtMemberIdentity.text.toString()
            viewModel.verifyMember(identity)
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
                    binding.rvMoviesGrid.visibility = View.GONE
                    val step = viewModel.currentStep.value
                    binding.layoutSeatSelection.visibility = if (step == 1) View.VISIBLE else View.GONE
                    binding.layoutMemberVerification.visibility = if (step == 2) View.VISIBLE else View.GONE
                    binding.btnBack.visibility = View.VISIBLE

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
                    binding.rvMoviesGrid.visibility = View.VISIBLE
                    binding.layoutSeatSelection.visibility = View.GONE
                    binding.layoutMemberVerification.visibility = View.GONE
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
                    if (viewModel.currentStep.value == 1) {
                        binding.btnNext.isEnabled = true
                        binding.btnNext.alpha = 1.0f
                    }
                } else {
                    binding.tvSelectedSeats.text = "Ghế đã chọn: Chưa chọn"
                    if (viewModel.currentStep.value == 1) {
                        binding.btnNext.isEnabled = false
                        binding.btnNext.alpha = 0.5f
                    }
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

        // Observe Current Step
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentStep.collectLatest { step ->
                binding.tvStepText.text = "$step of 4"

                val isMovieSelected = viewModel.selectedMovie.value != null
                binding.btnBack.visibility = if (isMovieSelected || step > 1) View.VISIBLE else View.GONE

                updateStepIndicators(step)

                when (step) {
                    1 -> {
                        val isMovieSelected = viewModel.selectedMovie.value != null
                        binding.layoutSeatSelection.visibility = if (isMovieSelected) View.VISIBLE else View.GONE
                        binding.layoutMemberVerification.visibility = View.GONE
                        val seatsSelected = viewModel.selectedSeats.value.isNotEmpty()
                        binding.btnNext.isEnabled = seatsSelected
                        binding.btnNext.alpha = if (seatsSelected) 1.0f else 0.5f
                    }
                    2 -> {
                        binding.layoutSeatSelection.visibility = View.GONE
                        binding.layoutMemberVerification.visibility = View.VISIBLE
                        binding.btnNext.isEnabled = true
                        binding.btnNext.alpha = 1.0f
                    }
                }
            }
        }

        // Observe Member Verification Results
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.verifiedMember.collectLatest { member ->
                if (member != null) {
                    binding.cardMemberResult.visibility = View.VISIBLE
                    binding.tvMemberName.text = "Họ tên: ${member.memberName}"
                    binding.tvMemberPhone.text = "Số điện thoại: ${member.phoneNumber}"
                    binding.tvMemberScore.text = "Điểm tích luỹ: ${member.currentScore} điểm"
                    binding.cardWalkInAlert.visibility = View.GONE
                } else {
                    binding.cardMemberResult.visibility = View.GONE
                    binding.cardWalkInAlert.visibility = View.VISIBLE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.memberVerificationError.collectLatest { error ->
                if (error != null) {
                    binding.tvMemberError.text = error
                    binding.tvMemberError.visibility = View.VISIBLE
                } else {
                    binding.tvMemberError.visibility = View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isVerifyingMember.collectLatest { isVerifying ->
                binding.btnVerifyMember.isEnabled = !isVerifying
                binding.btnVerifyMember.text = if (isVerifying) "ĐANG XÁC THỰC..." else "KIỂM TRA (VERIFY)"
            }
        }
    }

    private fun updateStepIndicators(step: Int) {
        val activeDrawable = R.drawable.bg_selected_item
        val inactiveColor = android.graphics.Color.parseColor("#232A45")

        binding.viewStep1.setBackgroundResource(if (step >= 1) activeDrawable else 0)
        if (step < 1) binding.viewStep1.setBackgroundColor(inactiveColor)

        binding.viewStep2.setBackgroundResource(if (step >= 2) activeDrawable else 0)
        if (step < 2) binding.viewStep2.setBackgroundColor(inactiveColor)

        binding.viewStep3.setBackgroundResource(if (step >= 3) activeDrawable else 0)
        if (step < 3) binding.viewStep3.setBackgroundColor(inactiveColor)

        binding.viewStep4.setBackgroundResource(if (step >= 4) activeDrawable else 0)
        if (step < 4) binding.viewStep4.setBackgroundColor(inactiveColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
