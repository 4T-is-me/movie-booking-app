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
import com.example.movie_booking_app.databinding.ActivityRewardPointsBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.adapter.ScoreHistoryAdapter
import com.example.movie_booking_app.viewmodel.BookingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
                            //assign name + id with last 8 characters
                            binding.tvUserInfo.text = "${it.fullName} • ${it._id.takeLast(8).uppercase()}"
                        }
                    }
                }

                // list score history
                launch {
                    viewModel.scoreHistoryState.collect { historyList ->
                        val homeList = historyList.take(3)
                        historyAdapter.updateData(homeList)

                        val firstItem = historyList.firstOrNull()
                        if (firstItem != null) {
                            val member = firstItem.memberId
                            // get totalSpent => double => async
                            val totalSpent = member.totalSpent.toDouble()
                            // Standard remaining points balance after the final transaction
                            val currentPoints = firstItem.remainingPoints
                            val memberRank = member.membershipRank
                            //Call the card color-changing function and run a automated progress bar based on real data.
                            updateCardRankStyle(totalSpent, currentPoints, memberRank)
                        }
                    }
                }
                // error
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

    //Change card color based on rank
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
                rankText = "GOLD MEMBER"
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#2E2401"), // start
                    android.graphics.Color.parseColor("#141103"), // middle
                    android.graphics.Color.parseColor("#141103")//end
                )

                progressPercentage = 100
                ratioText = "MAX RANK"
                progressLabelText = "You are at maximum tier"
                nextTierLeftText = "Thank you for being our Gold Member!"
            }
            "silver" -> {
                rankText = "SILVER MEMBER"
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#2C3E50"),
                    android.graphics.Color.parseColor("#1A252F"),
                    android.graphics.Color.parseColor("#0F0F11")
                )

                progressPercentage = ((totalSpent / Constants.RANK_GOLD) * 100).toInt()

                ratioText = String.format("%,d / %,d", totalSpentLong, Constants.RANK_GOLD.toLong())
                val moneyLeft = Constants.RANK_GOLD - totalSpentLong

                progressLabelText = "Progress to Gold Member"
                nextTierLeftText = String.format("%,dđ more to reach Gold Member", moneyLeft)
            }
            else -> { // Default to Bronze
                rankText = "BRONZE MEMBER"
                colors = intArrayOf(
                    android.graphics.Color.parseColor("#0F0F11"),
                    android.graphics.Color.parseColor("#150D05"),
                    android.graphics.Color.parseColor("#2A1A0A")

                )

                progressPercentage = ((totalSpent / Constants.RANK_SILVER) * 100).toInt()

                ratioText = String.format("%,d / %,d", totalSpentLong, Constants.RANK_SILVER.toLong())
                val moneyLeft = Constants.RANK_SILVER - totalSpentLong

                progressLabelText = "Progress to Silver Member"
                nextTierLeftText = String.format("%,dđ more to reach Silver Member", moneyLeft)
            }
        }

        // Create a Gradient Drawable with a 135-degree diagonal sweep from a dynamic color array.
        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, colors)
        gradientDrawable.cornerRadius = dpToPx(20)

        //Directly overwrite the old XML code with the newly created gradient color.
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
    // A helper function that converts dp values ​​to pixels to ensure accurate corner rounding across all phone screens.
    private fun dpToPx(dp: Int): Float {
        return dp * resources.displayMetrics.density
    }
}