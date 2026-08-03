package com.example.movie_booking_app.view.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movie_booking_app.R
import com.example.movie_booking_app.view.fragment.PurchaseHistoryFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PurchaseHistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_purchase_history)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PurchaseHistoryFragment())
                .commit()
        }
    }
}
