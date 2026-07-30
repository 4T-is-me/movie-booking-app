package com.example.movie_booking_app.view.fragment

import androidx.core.content.ContextCompat

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
import com.example.movie_booking_app.data.model.entity.UserProfile
import com.example.movie_booking_app.data.model.response.UserProfileResponse
import com.example.movie_booking_app.view.activity.RewardPointsActivity
import kotlin.jvm.java

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private var progressDialog: android.app.Dialog? = null
    private var currentUserProfile: UserProfile? = null
    private var lastLoadedAvatarUrl: String? = null

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
            val intent = Intent(requireContext(), RewardPointsActivity::class.java).apply {
                putExtra("USER_PROFILE", currentUserProfile)
            }
            startActivity(intent)
        }


        binding.layoutChangeLanguage.setOnClickListener {
            val languages = arrayOf("English", "Tiếng Việt", "日本語")
            val langCodes = arrayOf("en", "vi", "ja")
            val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            val currentLang = prefs.getString("language_code", "en") ?: "en"
            val checkedItem = langCodes.indexOf(currentLang).takeIf { it >= 0 } ?: 0

            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(com.example.movie_booking_app.R.string.auto_str_change_language))
                .setSingleChoiceItems(languages, checkedItem) { dialog, which ->
                    val selectedLang = langCodes[which]
                    if (selectedLang != currentLang) {
                        prefs.edit().putString("language_code", selectedLang).apply()
                        // Restart app/activity to apply changes
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                    dialog.dismiss()
                }
                .show()
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
            val act = activity ?: return@setOnClickListener
            com.example.movie_booking_app.utils.AuthUtils.showLogoutConfirmationDialog(act)
        }
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
                    currentUserProfile = userProfile
                    binding.tvName.text = userProfile?.fullName ?: "-"
                    binding.tvPhone.text = userProfile?.phone ?: "-"
                    binding.tvEmail.text = userProfile?.email ?: "-"

                    val rank = userProfile?.membershipRank ?: "bronze"
                    val rankClean = rank.trim().lowercase()

                    val badgeColor = when (rankClean) {
                        "gold" -> ContextCompat.getColor(requireContext(), R.color.cinema_primary)
                        "silver" -> ContextCompat.getColor(requireContext(), R.color.auto_color_C0C0C0)
                        else -> ContextCompat.getColor(requireContext(), R.color.auto_color_CD7F32) // bronze
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

                    // Dynamically refresh Member Card styling
                    val memberCardBg = when (rankClean) {
                        "gold" -> R.drawable.member_card_gold
                        "silver" -> R.drawable.member_card_silver
                        else -> R.drawable.member_card_bronze
                    }
                    binding.layoutMemberCardInner.setBackgroundResource(memberCardBg)
                    binding.cardMember.strokeColor = badgeColor
                    binding.ivAvatar.strokeColor = android.content.res.ColorStateList.valueOf(badgeColor)

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
                    if (finalUrl != lastLoadedAvatarUrl) {
                        lastLoadedAvatarUrl = finalUrl
                        Glide.with(this@ProfileFragment)
                            .load(finalUrl.ifEmpty { null })
                            .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis().toString()))
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(binding.ivAvatar)
                    }
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
