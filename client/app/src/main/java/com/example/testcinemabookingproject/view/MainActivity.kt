package com.example.testcinemabookingproject.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testcinemabookingproject.databinding.ActivityMainBinding

/**
 * MAIN ACTIVITY: Bây giờ đóng vai trò là "Cái khung nhà".
 * Nhiệm vụ duy nhất: Hiển thị giao diện và để Fragment tự lắp vào.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Thổi phồng bản đồ điện (View Binding)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Ghi chú: Không cần viết code setup RecyclerView ở đây nữa.
        // Vì trong activity_main.xml, FragmentContainerView đã tự động 
        // gọi HomeFragment ra làm việc rồi.
    }
}
