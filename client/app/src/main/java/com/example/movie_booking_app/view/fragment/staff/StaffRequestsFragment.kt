package com.example.movie_booking_app.view.fragment.staff

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.LeaveRequestResponse
import com.example.movie_booking_app.databinding.DialogDeleteLeaveRequestBinding
import com.example.movie_booking_app.databinding.DialogEditLeaveRequestBinding
import com.example.movie_booking_app.databinding.FragmentStaffRequestsBinding
import com.example.movie_booking_app.utils.DateUtils
import com.example.movie_booking_app.view.adapter.MyLeaveRequestsAdapter
import com.example.movie_booking_app.viewmodel.staff.LeaveRequestViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffRequestsFragment : Fragment() {

    private var _binding: FragmentStaffRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaveRequestViewModel by viewModels()
    private lateinit var leaveRequestsAdapter: MyLeaveRequestsAdapter
    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStaffRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.loadMyLeaveRequests()
    }

    private fun setupRecyclerView() {
        leaveRequestsAdapter = MyLeaveRequestsAdapter(
            onEditClick = { item ->
                showEditDialog(item)
            },
            onDeleteClick = { item ->
                item.leaveRequestId?.let { id -> showDeleteConfirmDialog(id) }
            }
        )
        val mLayoutManager = LinearLayoutManager(requireContext())
        layoutManager = mLayoutManager

        binding.rvLeaveRequests.apply {
            this.layoutManager = mLayoutManager
            this.adapter = leaveRequestsAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val visibleItemCount = mLayoutManager.childCount
                        val totalItemCount = mLayoutManager.itemCount
                        val pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition()
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            viewModel.loadNextPage()
                        }
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.leaveRequests.collectLatest { list ->
                leaveRequestsAdapter.submitList(list)
                binding.txtNoData.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                // Loading này chỉ hiển thị ở trang đầu tiên cho đỡ bị block UI khi loadmore
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                if (message != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.actionSuccessMessage.collectLatest { message ->
                if (message != null) {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    viewModel.clearSuccessMessage()
                }
            }
        }
    }

    private fun showEditDialog(item: LeaveRequestResponse) {
        val context = requireContext()
        val requestId = item.leaveRequestId ?: return

        val dialogBinding = DialogEditLeaveRequestBinding.inflate(layoutInflater)
        dialogBinding.etDialogWorkDate.setText(DateUtils.formatMovieDate(item.workDate))
        dialogBinding.etDialogShift.setText("${item.shift} Shift")
        dialogBinding.etDialogReason.setText(item.reason ?: "")
        dialogBinding.etDialogReason.setSelection(dialogBinding.etDialogReason.text.length)

        val builder = AlertDialog.Builder(context)
        builder.setView(dialogBinding.root)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnDialogCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDialogSave.setOnClickListener {
            val newReason = dialogBinding.etDialogReason.text.toString().trim()

            if (newReason.isNotEmpty()) {
                viewModel.updateLeaveReason(requestId, newReason)
                dialog.dismiss()
            } else {
                dialogBinding.etDialogReason.error = getString(R.string.error_empty_reason)
            }
        }
        dialog.show()
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.setLayout(width, height)
    }

    private fun showDeleteConfirmDialog(requestId: String) {
        val dialogBinding = DialogDeleteLeaveRequestBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialogBinding.btnDeleteDialogCancel.setOnClickListener {
            dialog.dismiss()
        }
        dialogBinding.btnDeleteDialogConfirm.setOnClickListener {
            viewModel.deleteLeaveRequest(requestId)
            dialog.dismiss()
        }

        dialog.show()
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.85).toInt()
        val height = WindowManager.LayoutParams.WRAP_CONTENT
        dialog.window?.setLayout(width, height)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}