package com.example.movie_booking_app.view.fragment.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.FragmentAdminStaffTabsBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AdminStaffTabsFragment : Fragment() {

    private var _binding: FragmentAdminStaffTabsBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_INITIAL_TAB = "initial_tab"

        fun newInstance(initialTab: Int = 0): AdminStaffTabsFragment {
            val fragment = AdminStaffTabsFragment()
            val args = Bundle()
            args.putInt(ARG_INITIAL_TAB, initialTab)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminStaffTabsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2
            override fun createFragment(position: Int): Fragment {
                return if (position == 0) AdminStaffFragment.newInstance("Manager") else AdminStaffFragment.newInstance("Staff")
            }
        }

        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = if (position == 0) getString(R.string.admin_tab_employees) else getString(R.string.admin_tab_staff)
        }.attach()

        // Select the initial tab passed via arguments
        val initialTab = arguments?.getInt(ARG_INITIAL_TAB) ?: 0
        binding.viewPager.setCurrentItem(initialTab, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
