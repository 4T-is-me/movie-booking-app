package com.example.movie_booking_app.view.fragment.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.entity.Staff
import com.example.movie_booking_app.databinding.FragmentAdminStaffBinding
import com.example.movie_booking_app.view.activity.AdminStaffActivity
import com.example.movie_booking_app.view.adapter.AdminStaffAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminStaffFragment : Fragment() {

    private var _binding: FragmentAdminStaffBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AdminStaffViewModel by viewModels()
    private lateinit var adapter: AdminStaffAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminStaffBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSearch()
        observeViewModel()

        binding.fabAddStaff.setOnClickListener {
            val intent = Intent(requireContext(), AdminStaffActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        adapter = AdminStaffAdapter(
            onEditClick = { staff ->
                val intent = Intent(requireContext(), AdminStaffActivity::class.java).apply {
                    putExtra("staff", staff)
                }
                startActivity(intent)
            },
            onDeleteClick = { staff ->
                showDeleteConfirmationDialog(staff)
            }
        )
        binding.rvAdminStaff.layoutManager = LinearLayoutManager(context)
        binding.rvAdminStaff.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchStaff.addTextChangedListener { text ->
            val query = text.toString().trim()
            viewModel.loadStaff(query.ifEmpty { null })
        }
    }

    private fun showDeleteConfirmationDialog(staff: Staff) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(com.example.movie_booking_app.R.layout.dialog_confirm_delete_staff)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDeleteMessage = dialog.findViewById<android.widget.TextView>(com.example.movie_booking_app.R.id.tvDeleteMessage)
        val btnCancel = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnCancelDelete)
        val btnConfirm = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.movie_booking_app.R.id.btnConfirmDelete)

        tvDeleteMessage.text = "Bạn có muốn xóa tài khoản nhân viên \"${staff.fullName}\" không?"

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            staff.id?.let { viewModel.deleteStaff(it) }
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Show loading state if needed, here we submit the list
                adapter.submitList(state.staffList)
                
                state.error?.let { err ->
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionSuccess.collectLatest { success ->
                if (success) {
                    viewModel.loadStaff()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.actionMessage.collectLatest { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
