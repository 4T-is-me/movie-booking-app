package com.example.movie_booking_app.view.fragment.admin

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
import com.example.movie_booking_app.databinding.DialogConfirmToggleMemberBinding
import com.example.movie_booking_app.databinding.DialogDetailMemberBinding
import com.example.movie_booking_app.databinding.FragmentAdminUserListBinding
import com.example.movie_booking_app.view.adapter.admin.MemberAdapter
import com.example.movie_booking_app.viewmodel.admin.AdminStaffViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MemberListFragment : Fragment() {

    private var _binding: FragmentAdminUserListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminStaffViewModel by viewModels()
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
            onToggleStatus = { member -> 
                showConfirmToggleStatusDialog(member)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun showMemberDetailDialog(member: MemberItem) {
        val dialogBinding = DialogDetailMemberBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.transparent))
        dialog.setCancelable(true)

        dialogBinding.tvDetailUsername.text = member.account?.username ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailFullName.text = member.fullName ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailEmail.text = member.account?.email ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailPhone.text = member.phoneNumber ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailIdentityCard.text = member.identityCard ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailAddress.text = member.address ?: getString(R.string.user_name_placeholder)
        dialogBinding.tvDetailRank.text = member.membershipRank ?: getString(R.string.admin_default_rank)
        dialogBinding.tvDetailPoints.text = String.format(Locale.getDefault(), "%,d", member.rewardPoints ?: 0)
        dialogBinding.tvDetailTotalSpent.text = String.format(Locale.getDefault(), "%,.0f ₫", member.totalSpent ?: 0.0)

        dialogBinding.btnCloseDetail.setOnClickListener { dialog.dismiss() }
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
        val dialogBinding = DialogConfirmToggleMemberBinding.inflate(layoutInflater)
        val dialog = Dialog(requireContext())
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), android.R.color.transparent))
        dialog.setCancelable(true)

        if (isActive) {
            dialogBinding.ivConfirmIcon.setImageResource(android.R.drawable.ic_menu_delete)
            dialogBinding.ivConfirmIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.yellow))
            dialogBinding.tvConfirmTitle.text = getString(R.string.admin_delete_member)
            dialogBinding.tvConfirmMessage.text = getString(R.string.admin_confirm_delete_member_msg, member.fullName ?: "")
            dialogBinding.btnConfirmToggle.text = getString(R.string.admin_delete)
        } else {
            dialogBinding.ivConfirmIcon.setImageResource(android.R.drawable.ic_menu_rotate)
            dialogBinding.ivConfirmIcon.setColorFilter(ContextCompat.getColor(requireContext(), R.color.color_green_status))
            dialogBinding.tvConfirmTitle.text = getString(R.string.admin_reactivate_member)
            dialogBinding.tvConfirmMessage.text = getString(R.string.admin_confirm_unlock_msg, member.fullName ?: "")
            dialogBinding.btnConfirmToggle.text = getString(R.string.admin_reactivate)
        }

        dialogBinding.btnCancelToggle.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnConfirmToggle.setOnClickListener {
            dialog.dismiss()
            val newStatus = if (isActive) 0 else 1
            member.id?.let { viewModel.updateMemberStatus(it, newStatus) }
        }

        dialog.show()
    }

    private fun setupListeners() {
        binding.edtSearch.addTextChangedListener { text ->
            viewModel.loadMembers(text?.toString())
        }
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadMembers(binding.edtSearch.text.toString())
        }
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
