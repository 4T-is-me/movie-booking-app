package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.DialogAdjustScoreBinding
import com.example.movie_booking_app.databinding.FragmentAdminUserListBinding
import com.example.movie_booking_app.view.adapter.admin.MemberAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberListFragment : Fragment() {

    private var _binding: FragmentAdminUserListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminStaffViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: MemberAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeViewModel()
        
        binding.fabAdd.visibility = View.GONE // Members usually register themselves
    }

    override fun onResume() {
        super.onResume()
        // Refresh data whenever returning to this fragment
        viewModel.loadMembers(binding.edtSearch.text.toString())
    }

    private fun setupRecyclerView() {
        adapter = MemberAdapter(
            onAdjustScore = { member -> showAdjustScoreDialog(member) },
            onToggleStatus = { member -> 
                showConfirmToggleStatusDialog(member)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun showConfirmToggleStatusDialog(member: MemberItem) {
        val isActive = member.account?.status != 0
        val title = if (isActive) "Lock Member" else "Unlock Member"
        val message = if (isActive) 
            "Are you sure you want to lock the account for \"${member.fullName}\"?" 
            else "Are you sure you want to reactivate the account for \"${member.fullName}\"?"
        val buttonText = if (isActive) "Lock" else "Unlock"
        val newStatus = if (isActive) 0 else 1

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(buttonText) { _, _ ->
                member.id?.let { viewModel.updateMemberStatus(it, newStatus) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupListeners() {
        binding.edtSearch.addTextChangedListener { text ->
            viewModel.loadMembers(text?.toString())
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMembers(binding.edtSearch.text.toString())
        }
    }

    private fun showAdjustScoreDialog(member: MemberItem) {
        val dialogBinding = DialogAdjustScoreBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        val currentPoints = member.rewardPoints ?: 0
        dialogBinding.tvCurrentPoints.text = "Current Points: ${String.format("%,d", currentPoints)}"

        // Thêm TextWatcher để tính toán điểm mới ngay khi Admin gõ
        dialogBinding.edtAdjustment.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val adj = s.toString().toIntOrNull() ?: 0
                val newTotal = currentPoints + adj
                
                dialogBinding.tvNewPoints.visibility = android.view.View.VISIBLE
                dialogBinding.tvNewPoints.text = "New Points: ${String.format("%,d", newTotal)}"
                
                // Đổi màu đỏ nếu tổng điểm < 0
                if (newTotal < 0) {
                    dialogBinding.tvNewPoints.setTextColor(android.graphics.Color.RED)
                    dialogBinding.btnConfirmAdjust.isEnabled = false
                } else {
                    dialogBinding.tvNewPoints.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.yellow))
                    dialogBinding.btnConfirmAdjust.isEnabled = true
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        dialogBinding.btnConfirmAdjust.setOnClickListener {
            val adjStr = dialogBinding.edtAdjustment.text.toString()
            val reason = dialogBinding.edtReason.text.toString()

            if (adjStr.isEmpty() || reason.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in both adjustment and reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val adjustment = adjStr.toIntOrNull() ?: 0
            member.id?.let { id ->
                viewModel.adjustMemberScore(id, currentPoints, adjustment, reason)
            }
            dialog.dismiss()
        }

        dialogBinding.btnCancelAdjust.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    adapter.submitList(state.memberList)
                    binding.swipeRefresh.isRefreshing = state.isLoading
                    state.error?.let {
                        Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scoreAdjustState.collectLatest { state ->
                    if (state.isSuccess) {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionMessage.collectLatest { message ->
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
