package com.example.testcinemabookingproject.data.repository

import com.example.testcinemabookingproject.data.api.TodoApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * 🛠️ [REPOSITORY]: NHÀ MÁY SẢN XUẤT KẾT NỐI
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Một Object (Singleton) - thực thể duy nhất trong toàn app.
 * - MỤC ĐÍCH: Khởi tạo và giữ cấu hình mạng để dùng chung cho mọi nơi.
 * - BẰNG CÁCH NÀO: Dùng Retrofit.Builder() để lắp ráp máy móc.
 * - FLOW: Gom [URL gốc] + [Người dịch GSON] = [Bộ máy kết nối Retrofit].
 */
object RetrofitClient {

    // Địa chỉ nhà của Server (Phải có dấu / ở cuối)
    private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

    // 1. Khởi tạo bộ máy Retrofit (Dùng lazy để chỉ khi nào cần mới bật máy)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // Gắn máy thông dịch GSON
            .build() // Chốt đơn, tạo máy
    }

    // 2. Tạo ra "Cánh cửa" cụ thể để lấy Todo
    val apiService: TodoApiService by lazy {
        retrofit.create(TodoApiService::class.java)
    }
}
