package com.example.movie_booking_app.view.fragment.staff

import android.os.Bundle
import timber.log.Timber
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.data.model.response.NotificationItemResponse
import com.example.movie_booking_app.databinding.FragmentStaffNotificationsBinding
import com.example.movie_booking_app.view.adapter.StaffNotifAdapter
import com.example.movie_booking_app.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StaffNotificationsFragment : Fragment() {

    private var _binding: FragmentStaffNotificationsBinding? = null
    private val binding get() = _binding!!

    // Thay đổi từ viewModels() -> activityViewModels() để dùng chung ViewModel với StaffActivity
    private val viewModel: NotificationViewModel by activityViewModels()
    private lateinit var adapter: StaffNotifAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStaffNotificationsBinding.inflate(inflater, container, false)
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
        viewModel.fetchStaffNotifications()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.fetchStaffNotifications()
        }
    }

    private fun setupRecyclerView() {
        adapter = StaffNotifAdapter { item ->
            if (!item.isRead) {
                viewModel.markAsRead(item.id)
            }
        }
        binding.rvStaffNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.rvStaffNotifications.adapter = adapter
    }

    private fun setupMarkAllRead() {
        binding.ivStaffNotifMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.swipeRefreshLayout.isRefreshing = isLoading
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.mergedNotifications.collectLatest { list ->
                Timber.tag("test").d(list.toString())
                if (list.isEmpty()) {
                    showEmpty()
                } else {
                    showList(list)
                }
            }
        }
    }

    private fun showEmpty() {
        binding.rvStaffNotifications.visibility = View.GONE
        binding.layoutStaffNotifEmpty.visibility = View.VISIBLE
        binding.ivStaffNotifMarkAllRead.visibility = View.GONE
        adapter.submitList(emptyList())
    }

    private fun showList(list: List<NotificationItemResponse>) {
        binding.layoutStaffNotifEmpty.visibility = View.GONE
        binding.rvStaffNotifications.visibility = View.VISIBLE
        binding.ivStaffNotifMarkAllRead.visibility = View.VISIBLE

        adapter.submitList(list)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}