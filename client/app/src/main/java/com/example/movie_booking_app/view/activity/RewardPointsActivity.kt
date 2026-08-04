package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.UserProfile
import com.example.movie_booking_app.databinding.ActivityRewardPointsBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.adapter.ScoreHistoryAdapter
import com.example.movie_booking_app.viewmodel.BookingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class RewardPointsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRewardPointsBinding
    private val viewModel: BookingViewModel by viewModels()
    private lateinit var historyAdapter: ScoreHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRewardPointsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        val userProfile = intent.getSerializableExtra("USER_PROFILE") as? UserProfile
        Timber.tag("member").d(userProfile.toString())
        userProfile?.let {
            val safeId = it.accountId?.takeLast(8)?.uppercase() ?: "UNKNOWN"
            binding.tvUserInfo.text = "${it.fullName ?: "-"} • $safeId"

            val totalSpent = it.totalSpent?.toDouble() ?: 0.0
            val rewardPoints = it.rewardPoints ?: 0
            val rank = it.membershipRank ?: "bronze"

            updateCardRankStyle(totalSpent, rewardPoints, rank)
        }

        viewModel.fetchScoreHistory()

        binding.btnBack.setOnClickListener { finish() }

        binding.tvViewAll.setOnClickListener {
            val intent = Intent(this, ScoreHistoryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = ScoreHistoryAdapter()
        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@RewardPointsActivity)
            adapter = historyAdapter
            isNestedScrollingEnabled = false // Disable nested scrolling to prevent scroll conflicts
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.memberState.collect { member ->
                        member?.let {
                            binding.tvUserInfo.text = "${it.fullName} • ${it._id.takeLast(8).uppercase()}"
                            updateCardRankStyle(it.totalSpent, it.rewardPoints, it.membershipRank)
                        }
                    }
                }

                launch {
                    viewModel.scoreHistoryState.collect { historyList ->
                        if (historyList.isEmpty()) {
                            binding.rvHistory.visibility = View.GONE
                            binding.tvEmptyHistory.visibility = View.VISIBLE

                            if (binding.tvUserInfo.text.isNullOrEmpty()) {
                                updateCardRankStyle(0.0, 0, "bronze")
                            }
                        } else {
                            binding.rvHistory.visibility = View.VISIBLE
                            binding.tvEmptyHistory.visibility = View.GONE

                            val homeList = historyList.take(3)
                            historyAdapter.updateData(homeList)

                            val firstItem = historyList.firstOrNull()
                            if (firstItem != null) {
                                val member = firstItem.memberId
                                val totalSpent = member.totalSpent.toDouble()
                                val currentPoints = firstItem.remainingPoints
                                val memberRank = member.membershipRank
                                updateCardRankStyle(totalSpent, currentPoints, memberRank)
                            }
                        }
                    }
                }

                launch {
                    viewModel.errorMessage.collect { error ->
                        error?.let {
                            Toast.makeText(this@RewardPointsActivity, it, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    // Change card color based on rank
    private fun updateCardRankStyle(totalSpent: Double, currentPoints: Int, memberShipRank: String) {
        val colors: IntArray
        val rankText: String

        // Declare variables for the rank-up progress bar.
        val ratioText: String
        val nextTierLeftText: String
        val progressPercentage: Int
        val progressLabelText: String

        val totalSpentLong = totalSpent.toLong()

        when (memberShipRank.lowercase().trim()) {
            "gold" -> {
                rankText = getString(R.string.reward_rank_gold)
                colors = intArrayOf(
                    ContextCompat.getColor(this, R.color.rank_gold_start),
                    ContextCompat.getColor(this, R.color.rank_gold_middle),
                    ContextCompat.getColor(this, R.color.rank_gold_end)
                )

                progressPercentage = 100
                ratioText = getString(R.string.reward_max_rank)
                progressLabelText = getString(R.string.reward_max_tier_msg)
                nextTierLeftText = getString(R.string.reward_thanks_gold)
            }
            "silver" -> {
                rankText = getString(R.string.reward_rank_silver)
                colors = intArrayOf(
                    ContextCompat.getColor(this, R.color.rank_silver_start),
                    ContextCompat.getColor(this, R.color.rank_silver_middle),
                    ContextCompat.getColor(this, R.color.rank_silver_end)
                )

                progressPercentage = ((totalSpent / Constants.RANK_GOLD) * 100).toInt()

                ratioText = getString(R.string.reward_ratio_format, totalSpentLong, Constants.RANK_GOLD.toLong())
                val moneyLeft = Constants.RANK_GOLD - totalSpentLong

                progressLabelText = getString(R.string.reward_progress_to_gold)
                nextTierLeftText = getString(R.string.reward_more_to_gold, moneyLeft)
            }
            else -> { // Default to Bronze
                rankText = getString(R.string.reward_rank_bronze)
                colors = intArrayOf(
                    ContextCompat.getColor(this, R.color.rank_bronze_start),
                    ContextCompat.getColor(this, R.color.rank_bronze_middle),
                    ContextCompat.getColor(this, R.color.rank_bronze_end)
                )

                progressPercentage = ((totalSpent / Constants.RANK_SILVER) * 100).toInt()

                ratioText = getString(R.string.reward_ratio_format, totalSpentLong, Constants.RANK_SILVER.toLong())
                val moneyLeft = Constants.RANK_SILVER - totalSpentLong

                progressLabelText = getString(R.string.reward_progress_to_silver)
                nextTierLeftText = getString(R.string.reward_more_to_silver, moneyLeft)
            }
        }

        // Create a Gradient Drawable with a 135-degree diagonal sweep from a dynamic color array.
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        gradientDrawable.cornerRadius = dpToPx(20)

        // Directly overwrite the old XML code with the newly created gradient color.
        binding.layoutMemberCard.background = gradientDrawable
        binding.tvBadgeText.text = rankText
        binding.tvPointsNum.text = String.format("%,d", currentPoints)

        // Update ProgressBar value & percentage
        binding.pbTierProgress.max = 100
        binding.pbTierProgress.progress = progressPercentage
        binding.tvProgressRatio.text = ratioText

        binding.tvProgressLabel.text = progressLabelText
        binding.tvNextTierHint.text = nextTierLeftText
    }

    // A helper function that converts dp values to pixels to ensure accurate corner rounding across all phone screens.
    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }
}