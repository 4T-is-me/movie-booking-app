package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Booking
import com.example.movie_booking_app.data.model.entity.ComboDetail
import com.example.movie_booking_app.databinding.ActivitySelectFoodBinding
import com.example.movie_booking_app.view.adapter.FoodBeverageAdapter
import com.example.movie_booking_app.viewmodel.SelectFoodViewModel
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class SelectFoodActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectFoodBinding
    private val viewModel: SelectFoodViewModel by viewModels()
    private lateinit var foodAdapter: FoodBeverageAdapter
    private var isExpiredDialogShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectFoodBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val booking = intent.getSerializableExtra("BOOKING_SESSION") as? Booking
        if (booking == null) {
            finish()
            return
        }

        viewModel.initBooking(booking.showtimeId)

        setupUI()
        setupTabs()
        observeViewModel(booking)
    }

    private fun setupUI() {
        foodAdapter = FoodBeverageAdapter { item ->
            viewModel.updateQuantity(item)
        }

        binding.rvFood.apply {
            layoutManager = LinearLayoutManager(this@SelectFoodActivity)
            adapter = foodAdapter
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        val displayCategories = listOf(
            getString(R.string.fb_tab_all),
            getString(R.string.fb_tab_combo),
            getString(R.string.fb_tab_drink),
            getString(R.string.fb_tab_food)
        )
        val foodCategory = com.example.movie_booking_app.utils.Constants.FoodCategory
        val internalCategories = listOf(foodCategory.ALL, foodCategory.COMBO, foodCategory.DRINK, foodCategory.FOOD)

        displayCategories.forEach {
            binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it))
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val position = tab?.position ?: 0
                if (position in internalCategories.indices) {
                    viewModel.setCategory(internalCategories[position])
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun observeViewModel(currentBooking: Booking) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.shimmerFood.isVisible = state.isLoading
                    if (state.isLoading) binding.shimmerFood.startShimmer() else binding.shimmerFood.stopShimmer()
                    
                    binding.rvFood.isVisible = !state.isLoading
                    foodAdapter.submitList(state.filteredItems)

                    binding.tvTotalAmount.text = formatCurrency(currentBooking.ticketTotal + state.totalFBPrice)
                    binding.tvCountdown.text = state.countdownText

                    if (state.isExpired && !isExpiredDialogShown) {
                        isExpiredDialogShown = true
                        showExpiredDialog()
                    }

                    binding.btnContinue.setOnClickListener {
                        if (state.isExpired) {
                            showExpiredDialog()
                            return@setOnClickListener
                        }
                        
                        val selectedFB = viewModel.getSelectedItems()
                        val updatedBooking = currentBooking.copy(
                            combos = selectedFB.map { 
                                ComboDetail(it.id, it.name, it.selectedQuantity, it.price)
                            },
                            comboTotal = state.totalFBPrice,
                            grandTotal = currentBooking.ticketTotal + state.totalFBPrice + currentBooking.bookingFee
                        )

                        val intent = Intent(this@SelectFoodActivity, PaymentActivity::class.java).apply {
                            putExtra("BOOKING_SESSION", updatedBooking)
                        }
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showExpiredDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_expired))
            .setMessage(getString(R.string.msg_expired))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.btn_ok)) { _, _ ->
                val intent = Intent(this, SelectSeatActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun formatCurrency(amount: Int): String {
        val formatter = NumberFormat.getInstance(Locale.GERMANY)
        return "${formatter.format(amount)} VND"
    }
}
