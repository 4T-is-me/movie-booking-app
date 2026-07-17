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
import com.example.movie_booking_app.data.model.response.BookingHistoryItem
import com.example.movie_booking_app.databinding.FragmentMyTicketBinding
import com.example.movie_booking_app.view.activity.BookingDetailActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.view.adapter.MyTicketAdapter
import com.example.movie_booking_app.viewmodel.MyTicketViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        adapter = MyTicketAdapter(
            onViewTicketClick = { item ->
                val intent = Intent(requireContext(), BookingDetailActivity::class.java).apply {
                    putExtra("BOOKING_ID", item.bookingId)
                }
                startActivity(intent)
            },
            onViewQrClick = { item ->
                showQrDialog(item)
            }
        )
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

    private fun showQrDialog(item: BookingHistoryItem) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_qr_code)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvMovieTitle = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvMovieTitle)
        val tvBookingCode = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvBookingCode)
        val tvDateTime = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDateTime)
        val tvCinemaRoom = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvCinemaRoom)
        val ivQrCode = dialog.findViewById<android.widget.ImageView>(com.example.movie_booking_app.R.id.ivQrCode)
        val btnClose = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnClose)

        tvMovieTitle.text = item.movie
        tvBookingCode.text = "Booking ID: ${item.bookingCode}"
        tvDateTime.text = "${item.showDate} • ${item.showtime}"
        tvCinemaRoom.text = "${item.cinemaName} • ${item.roomName}"

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        val qr = item.qrCodeData
        if (!qr.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val bitmap = withContext(Dispatchers.Default) {
                        val cleanBase64 = qr.substringAfter("base64,")
                        val imageBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
                        android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                    if (isAdded && !isDetached && _binding != null) {
                        if (bitmap != null) {
                            ivQrCode.setImageBitmap(bitmap)
                        } else {
                            ivQrCode.setImageResource(com.example.movie_booking_app.R.drawable.ic_ticket)
                        }
                        dialog.show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MyTicketFragment", "Error decoding QR Code", e)
                    if (isAdded && !isDetached && _binding != null) {
                        ivQrCode.setImageResource(com.example.movie_booking_app.R.drawable.ic_ticket)
                        android.widget.Toast.makeText(requireContext(), "Unable to load QR Code.", android.widget.Toast.LENGTH_SHORT).show()
                        dialog.show()
                    }
                }
            }
        } else {
            ivQrCode.setImageResource(com.example.movie_booking_app.R.drawable.ic_ticket)
            dialog.show()
        }
    }
}
