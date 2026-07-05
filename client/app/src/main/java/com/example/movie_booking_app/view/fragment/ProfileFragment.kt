package com.example.movie_booking_app.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.movie_booking_app.data.local.TokenManager
import com.example.movie_booking_app.databinding.FragmentProfileBinding
import com.example.movie_booking_app.view.activity.LoginActivity
import com.example.movie_booking_app.view.activity.MainActivity
import com.example.movie_booking_app.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.activity.RewardPointsActivity
import kotlin.jvm.java

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private var progressDialog: android.app.Dialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        // Hide local redundant bottom navigation
        binding.bottomNav.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSwipeRefresh()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadUserProfile()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog?.dismiss()
        progressDialog = null
        _binding = null
    }

    private fun setupClickListeners() {
        binding.ivEditProfile.setOnClickListener {
            val intent = Intent(requireContext(), com.example.movie_booking_app.view.activity.EditProfileActivity::class.java)
            startActivity(intent)
        }
        binding.layoutRewardPoints.setOnClickListener {
            val intent = Intent(requireContext(), RewardPointsActivity::class.java)
            startActivity(intent)
        }

        binding.layoutMyTicket.setOnClickListener {
            Toast.makeText(context, "My Ticket Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.layoutChangeLanguage.setOnClickListener {
            Toast.makeText(context, "Change Language Clicked", Toast.LENGTH_SHORT).show()
        }
        binding.layoutChangePassword.setOnClickListener {
            val intent = Intent(requireContext(), com.example.movie_booking_app.view.activity.ChangePasswordActivity::class.java)
            startActivity(intent)
        }
        binding.layoutPurchaseHistory.setOnClickListener {
            val intent = Intent(requireContext(), com.example.movie_booking_app.view.activity.PurchaseHistoryActivity::class.java)
            startActivity(intent)
        }
        binding.layoutLogout.setOnClickListener {
            performLogout()
        }
    }

    private fun performLogout() {
        val act = activity ?: return
        TokenManager.clearTokens(act)
        val sharedPref = act.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("auth_token")
            remove("username")
            remove("role")
            apply()
        }
        Toast.makeText(act, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(act, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        act.finish()
    }

    private fun showLoading(isLoading: Boolean) {
        val act = activity ?: return
        if (isLoading) {
            if (binding.swipeRefresh.isRefreshing) return
            if (progressDialog == null) {
                progressDialog = android.app.Dialog(act).apply {
                    setContentView(android.widget.ProgressBar(act))
                    window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                    setCancelable(false)
                }
            }
            progressDialog?.show()
        } else {
            progressDialog?.dismiss()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { userProfile ->
                    binding.tvName.text = userProfile?.fullName ?: "-"
                    binding.tvPhone.text = userProfile?.phone ?: "-"
                    binding.tvEmail.text = userProfile?.email ?: "-"

                    val rank = userProfile?.membershipRank ?: "bronze"
                    val rankClean = rank.trim().lowercase()

                    val badgeColor = when (rankClean) {
                        "gold" -> android.graphics.Color.parseColor("#F5C542")
                        "silver" -> android.graphics.Color.parseColor("#C0C0C0")
                        else -> android.graphics.Color.parseColor("#CD7F32") // bronze
                    }

                    val badgeBgRes = when (rankClean) {
                        "gold" -> R.drawable.bg_badge_gold
                        "silver" -> R.drawable.bg_badge_silver
                        else -> R.drawable.bg_badge_bronze
                    }

                    // Apply badge background and texts
                    binding.layoutBadge.setBackgroundResource(badgeBgRes)
                    binding.tvRank.text = "${rank.replaceFirstChar { it.uppercase() }} Member"
                    binding.tvRank.setTextColor(badgeColor)

                    // Tint crown icon inside the badge
                    binding.imgBadgeCrownIcon.imageTintList = android.content.res.ColorStateList.valueOf(badgeColor)

                    val points = userProfile?.rewardPoints ?: 0
                    binding.tvPoints.text = "$points pts"

                    val spent = userProfile?.totalSpent ?: 0
                    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("vi", "VN")).apply {
                        groupingSeparator = ','
                    }
                    val spentFormatter = java.text.DecimalFormat("#,###", symbols)
                    binding.tvTotalSpent.text = "${spentFormatter.format(spent)} ₫"

                    val url = userProfile?.image
                    val finalUrl = when {
                        url.isNullOrEmpty() -> ""
                        url.startsWith("http") -> url
                        else -> "${com.example.movie_booking_app.utils.Constants.BASE_URL.removeSuffix("/")}$url"
                    }
                    Glide.with(this@ProfileFragment)
                        .load(finalUrl.ifEmpty { null })
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .into(binding.ivAvatar)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        viewModel.resetError()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    showLoading(isLoading)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.updateSuccess.collect { success ->
                    if (success) {
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                        viewModel.resetUpdateSuccess()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isUnauthorized.collect { unauthorized ->
                    if (unauthorized) {
                        val act = activity ?: return@collect
                        Toast.makeText(act, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
                        TokenManager.clearTokens(act)
                        val sharedPref = act.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            remove("auth_token")
                            remove("username")
                            remove("role")
                            apply()
                        }
                        val intent = Intent(act, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        act.finish()
                    }
                }
            }
        }
    }
}
