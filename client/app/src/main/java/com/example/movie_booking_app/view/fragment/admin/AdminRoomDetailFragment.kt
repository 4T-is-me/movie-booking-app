package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.AdminSeatAdapter
import com.example.movie_booking_app.viewmodel.AdminRoomViewModel
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminRoomDetailFragment : Fragment() {

    private val viewModel: AdminRoomViewModel by viewModels()
    private lateinit var adapter: AdminSeatAdapter
    private var roomId: String? = null
    private var roomName: String? = null
    private var columns: Int = 8

    companion object {
        fun newInstance(roomId: String, roomName: String, columns: Int): AdminRoomDetailFragment {
            val fragment = AdminRoomDetailFragment()
            val args = Bundle()
            args.putString("ROOM_ID", roomId)
            args.putString("ROOM_NAME", roomName)
            args.putInt("COLUMNS", columns)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomId = arguments?.getString("ROOM_ID")
        roomName = arguments?.getString("ROOM_NAME")
        columns = arguments?.getInt("COLUMNS") ?: 8
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_room_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvSeatGrid = view.findViewById<RecyclerView>(R.id.rvSeatGrid)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSaveLayout)
        val tvRoomTitle = view.findViewById<TextView>(R.id.tvRoomTitle)
        val tvRoomSummary = view.findViewById<TextView>(R.id.tvRoomSummary)

        tvRoomTitle.text = roomName ?: "Seat Layout"
        tvRoomSummary.text = "Tap on seat to change type"

        adapter = AdminSeatAdapter(
            onSeatClick = { seat ->
                viewModel.toggleSeatType(seat)
            }
        )

        rvSeatGrid.layoutManager = GridLayoutManager(context, columns)
        rvSeatGrid.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.seats.collect { seats ->
                adapter.submitList(seats)
                tvRoomSummary.text = "Seat layout · ${seats.size} seats"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        roomId?.let { viewModel.loadRoomSeats(it) }

        btnSave.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}