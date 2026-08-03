package com.example.movie_booking_app.view.fragment.manager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.NotificationItemResponse
import com.example.movie_booking_app.databinding.FragmentManagerNotificationsBinding
import com.example.movie_booking_app.view.adapter.manager.ManagerNotifAdapter
import com.example.movie_booking_app.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManagerNotificationsFragment : Fragment() {

    private var _binding: FragmentManagerNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationViewModel by activityViewModels()
    private lateinit var adapter: ManagerNotifAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManagerNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupMarkAllRead()
        setupSwipeRefresh()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotifications()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadNotifications()
        }
    }

    private fun setupRecyclerView() {
        adapter = ManagerNotifAdapter { item ->
            if (!item.isRead) {
                viewModel.markAsRead(item.id, isStaff = false)
            }
            if (item.type == "LEAVE_REQUEST_CREATED" || item.type == "SHIFT_CHANGE_ACCEPTED") {
                val fragment = ManagerStaffFragment().apply {
                    arguments = Bundle().apply {
                        putString("target_tab", "leave_requests")
                    }
                }
                (activity as? com.example.movie_booking_app.view.activity.ManagerActivity)?.replaceFragment(
                    fragment,
                    getString(R.string.auto_str_staff)
                )
            }
        }
        binding.rvManagerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvManagerNotifications.adapter = adapter
    }

    private fun setupMarkAllRead() {
        binding.ivManagerNotifMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead(isStaff = false)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
                binding.progressManagerNotif.visibility = if (isLoading && adapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collectLatest { list ->
                if (list.isEmpty()) {
                    showEmpty()
                } else {
                    showList(list)
                }
            }
        }
    }

    private fun showEmpty() {
        binding.rvManagerNotifications.visibility = View.GONE
        binding.layoutManagerNotifEmpty.visibility = View.VISIBLE
        binding.ivManagerNotifMarkAllRead.visibility = View.GONE
        adapter.submitList(emptyList())
    }

    private fun showList(list: List<NotificationItemResponse>) {
        binding.layoutManagerNotifEmpty.visibility = View.GONE
        binding.rvManagerNotifications.visibility = View.VISIBLE
        binding.ivManagerNotifMarkAllRead.visibility = View.VISIBLE

        adapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
