package com.example.movie_booking_app.view.fragment.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.adapter.SeatPreviewAdapter
import com.example.movie_booking_app.viewmodel.AddCinemaRoomViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddCinemaRoomFragment : Fragment() {

    private val viewModel: AddCinemaRoomViewModel by viewModels()
    private lateinit var seatPreviewAdapter: SeatPreviewAdapter

    private lateinit var btnBack: View
    private lateinit var etRoomName: TextInputEditText
    private lateinit var actvRoomType: AutoCompleteTextView
    private lateinit var actvScreenType: AutoCompleteTextView
    private lateinit var actvStatus: AutoCompleteTextView
    private lateinit var etCapacity: TextInputEditText
    private lateinit var etRows: TextInputEditText
    private lateinit var etColumns: TextInputEditText
    private lateinit var tvPreviewHeader: TextView
    private lateinit var tvScreen: TextView
    private lateinit var layoutLegend: View
    private lateinit var rvSeatPreview: RecyclerView
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var pbLoading: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_cinema_room, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Views
        btnBack = view.findViewById(R.id.btnBack)
        etRoomName = view.findViewById(R.id.etRoomName)
        actvRoomType = view.findViewById(R.id.actvRoomType)
        actvScreenType = view.findViewById(R.id.actvScreenType)
        actvStatus = view.findViewById(R.id.actvStatus)
        etCapacity = view.findViewById(R.id.etCapacity)
        etRows = view.findViewById(R.id.etRows)
        etColumns = view.findViewById(R.id.etColumns)
        tvPreviewHeader = view.findViewById(R.id.tvPreviewHeader)
        tvScreen = view.findViewById(R.id.tvScreen)
        layoutLegend = view.findViewById(R.id.layoutLegend)
        rvSeatPreview = view.findViewById(R.id.rvSeatPreview)
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage)
        btnCancel = view.findViewById(R.id.btnCancel)
        btnSave = view.findViewById(R.id.btnSave)
        pbLoading = view.findViewById(R.id.pbLoading)

        // Setup drop downs (Adapters)
        setupDropdowns()

        // Setup seat preview list & adapter, enabling cycle type on tap
        seatPreviewAdapter = SeatPreviewAdapter(
            onSeatClick = { position ->
                viewModel.cycleSeatType(position)
            }
        )
        rvSeatPreview.adapter = seatPreviewAdapter

        // Set listeners
        setupFormListeners()

        // Observe view model
        observeViewModel()
    }

    private fun setupDropdowns() {
        val roomTypes = arrayOf("Standard", "VIP", "IMAX")
        val roomTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roomTypes)
        actvRoomType.setAdapter(roomTypeAdapter)
        actvRoomType.setText(roomTypes[0], false)
        viewModel.roomType.value = roomTypes[0]

        val screenTypes = arrayOf("2D", "3D", "IMAX")
        val screenTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, screenTypes)
        actvScreenType.setAdapter(screenTypeAdapter)
        actvScreenType.setText(screenTypes[0], false)
        viewModel.screenType.value = screenTypes[0]

        val statuses = arrayOf("Active", "Maintenance", "Inactive")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statuses)
        actvStatus.setAdapter(statusAdapter)
        actvStatus.setText(statuses[0], false)
        viewModel.status.value = "active" // Backend expects lowercase string representation

        // Handle selection updates
        actvRoomType.setOnItemClickListener { _, _, position, _ ->
            viewModel.roomType.value = roomTypes[position]
        }
        actvScreenType.setOnItemClickListener { _, _, position, _ ->
            viewModel.screenType.value = screenTypes[position]
        }
        actvStatus.setOnItemClickListener { _, _, position, _ ->
            val selected = statuses[position]
            viewModel.status.value = selected.lowercase()
        }
    }

    private fun setupFormListeners() {
        // Room name change
        etRoomName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.roomName.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Rows input change (automatic preview updates handled in VM init)
        etRows.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.rowsStr.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Columns input change (automatic preview updates handled in VM init)
        etColumns.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.columnsStr.value = s?.toString().orEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Save click
        btnSave.setOnClickListener {
            viewModel.saveRoom()
        }

        // Cancel clicks
        val onCancelClick = View.OnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnCancel.setOnClickListener(onCancelClick)
        btnBack.setOnClickListener(onCancelClick)
    }

    private fun observeViewModel() {
        // Observe capacity state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.capacity.collectLatest { cap ->
                etCapacity.setText("$cap seats")
            }
        }

        // Observe seat previews
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.previewSeats.collectLatest { seats ->
                if (seats.isNotEmpty()) {
                    tvPreviewHeader.visibility = View.VISIBLE
                    tvScreen.visibility = View.VISIBLE
                    layoutLegend.visibility = View.VISIBLE
                    rvSeatPreview.visibility = View.VISIBLE

                    val cols = viewModel.columnsStr.value.toIntOrNull() ?: 12
                    rvSeatPreview.layoutManager = GridLayoutManager(context, cols)
                    seatPreviewAdapter.submitList(seats)
                } else {
                    tvPreviewHeader.visibility = View.GONE
                    tvScreen.visibility = View.GONE
                    layoutLegend.visibility = View.GONE
                    rvSeatPreview.visibility = View.GONE
                }
            }
        }

        // Observe error messages
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { msg ->
                if (msg != null) {
                    tvErrorMessage.visibility = View.VISIBLE
                    tvErrorMessage.text = msg
                } else {
                    tvErrorMessage.visibility = View.GONE
                }
            }
        }

        // Observe loading state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                if (isLoading) {
                    pbLoading.visibility = View.VISIBLE
                    btnSave.text = ""
                    btnSave.isEnabled = false
                } else {
                    pbLoading.visibility = View.GONE
                    btnSave.text = "Save Room"
                    btnSave.isEnabled = true
                }
            }
        }

        // Observe success status
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isSuccess.collectLatest { isSuccess ->
                if (isSuccess) {
                    showSuccessDialog()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Success")
            .setMessage("Cinema room created successfully.")
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                parentFragmentManager.popBackStack()
            }
            .show()
    }
}
