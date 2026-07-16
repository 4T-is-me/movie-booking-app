package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.MemberItem
import com.example.movie_booking_app.databinding.DialogAdjustScoreBinding
import com.example.movie_booking_app.databinding.FragmentAdminUserListBinding
import com.example.movie_booking_app.view.adapter.admin.MemberAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberListFragment : Fragment() {

    private var _binding: FragmentAdminUserListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminStaffViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: MemberAdapter

    private var selectedTab = TAB_ACTIVE

    companion object {
        private const val TAB_ACTIVE = "active"
        private const val TAB_INACTIVE = "inactive"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        setupTabs()
        observeViewModel()
        
        binding.fabAdd.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMembers(binding.edtSearch.text.toString())
    }

    private fun setupRecyclerView() {
        adapter = MemberAdapter(
            onDetailClick = { member -> showMemberDetailDialog(member) },
            onAdjustScore = { member -> showAdjustScoreDialog(member) },
            onToggleStatus = { member -> 
                showConfirmToggleStatusDialog(member)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun showMemberDetailDialog(member: MemberItem) {
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_detail_member)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val tvDetailUsername = dialog.findViewById<TextView>(R.id.tvDetailUsername)
        val tvDetailFullName = dialog.findViewById<TextView>(R.id.tvDetailFullName)
        val tvDetailEmail = dialog.findViewById<TextView>(R.id.tvDetailEmail)
        val tvDetailPhone = dialog.findViewById<TextView>(R.id.tvDetailPhone)
        val tvDetailIdentityCard = dialog.findViewById<TextView>(R.id.tvDetailIdentityCard)
        val tvDetailAddress = dialog.findViewById<TextView>(R.id.tvDetailAddress)
        val tvDetailRank = dialog.findViewById<TextView>(R.id.tvDetailRank)
        val tvDetailPoints = dialog.findViewById<TextView>(R.id.tvDetailPoints)
        val tvDetailTotalSpent = dialog.findViewById<TextView>(R.id.tvDetailTotalSpent)
        val btnClose = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseDetail)

        tvDetailUsername.text = member.account?.username ?: "-"
        tvDetailFullName.text = member.fullName ?: "-"
        tvDetailEmail.text = member.account?.email ?: "-"
        tvDetailPhone.text = member.phoneNumber ?: "-"
        tvDetailIdentityCard.text = member.identityCard ?: "-"
        tvDetailAddress.text = member.address ?: "-"
        tvDetailRank.text = member.membershipRank ?: "Bronze"
        tvDetailPoints.text = String.format("%,d", member.rewardPoints ?: 0)
        tvDetailTotalSpent.text = String.format("%,.0f đ", member.totalSpent ?: 0.0)

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setupTabs() {
        binding.tabActiveMember.setOnClickListener { switchTab(TAB_ACTIVE) }
        binding.tabInactiveMember.setOnClickListener { switchTab(TAB_INACTIVE) }
    }

    private fun switchTab(tab: String) {
        if (selectedTab == tab) return
        selectedTab = tab
        
        val isActive = (tab == TAB_ACTIVE)
        updateTabStyle(binding.tabActiveMember, isActive)
        updateTabStyle(binding.tabInactiveMember, !isActive)
        
        val fullList = viewModel.uiState.value.memberList
        adapter.submitList(getFilteredMemberList(fullList))
    }

    private fun updateTabStyle(tabView: TextView, isActive: Boolean) {
        if (isActive) {
            tabView.setBackgroundResource(R.drawable.bg_tab_active)
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            tabView.background = null
            tabView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray))
        }
    }

    private fun getFilteredMemberList(fullList: List<MemberItem>): List<MemberItem> {
        return if (selectedTab == TAB_ACTIVE) {
            fullList.filter { it.account?.status != 0 }
        } else {
            fullList.filter { it.account?.status == 0 }
        }
    }

    private fun showConfirmToggleStatusDialog(member: MemberItem) {
        val isActive = member.account?.status != 0
        val title = if (isActive) "Lock Member" else "Unlock Member"
        val message = if (isActive) 
            "Are you sure you want to lock the account for \"${member.fullName}\"?" 
            else "Are you sure you want to reactivate the account for \"${member.fullName}\"?"
        val buttonText = if (isActive) "Lock" else "Unlock"
        val newStatus = if (isActive) 0 else 1

        AlertDialog.Builder(requireContext())
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

        val validate = {
            val adjStr = dialogBinding.edtAdjustment.text.toString()
            val reason = dialogBinding.edtReason.text.toString()
            val adj = adjStr.toIntOrNull() ?: 0
            val newTotal = currentPoints + adj
            
            var isValid = true
            
            // Validate Adjustment
            if (adjStr.isEmpty()) {
                dialogBinding.tilAdjustment.error = "Enter adjustment"
                isValid = false
            } else if (adj == 0) {
                dialogBinding.tilAdjustment.error = "Cannot be 0"
                isValid = false
            } else if (newTotal < 0) {
                dialogBinding.tilAdjustment.error = "New total < 0"
                isValid = false
            } else {
                dialogBinding.tilAdjustment.error = null
            }
            
            // Validate Reason
            if (reason.isEmpty()) {
                dialogBinding.tilReason.error = "Reason required"
                isValid = false
            } else {
                dialogBinding.tilReason.error = null
            }
            
            dialogBinding.btnConfirmAdjust.isEnabled = isValid

            // Update New Points Preview
            if (adjStr.isNotEmpty()) {
                dialogBinding.tvNewPoints.visibility = View.VISIBLE
                dialogBinding.tvNewPoints.text = "New Points: ${String.format("%,d", newTotal)}"
                dialogBinding.tvNewPoints.setTextColor(
                    if (newTotal < 0) android.graphics.Color.RED 
                    else ContextCompat.getColor(requireContext(), R.color.yellow)
                )
            } else {
                dialogBinding.tvNewPoints.visibility = View.GONE
            }
        }

        dialogBinding.edtAdjustment.addTextChangedListener { validate() }
        dialogBinding.edtReason.addTextChangedListener { validate() }
        
        // Initial validation
        dialogBinding.btnConfirmAdjust.isEnabled = false

        dialogBinding.btnConfirmAdjust.setOnClickListener {
            val adjStr = dialogBinding.edtAdjustment.text.toString()
            val adjustment = adjStr.toIntOrNull() ?: 0
            val reason = dialogBinding.edtReason.text.toString()

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
                    adapter.submitList(getFilteredMemberList(state.memberList))
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
