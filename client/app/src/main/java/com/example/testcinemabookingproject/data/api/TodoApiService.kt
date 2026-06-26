package com.example.testcinemabookingproject.data.api

import com.example.testcinemabookingproject.data.model.Todo
import retrofit2.http.GET

/**
 * 📡 [API]: ĐỊNH NGHĨA "MENU" DỊCH VỤ MẠNG
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Một Interface (bộ quy tắc) để Retrofit biết đường gọi API.
 * - MỤC ĐÍCH: Chỉ định địa chỉ (URL) và hành động (Lấy hàng) cụ thể.
 * - BẰNG CÁCH NÀO: Dùng Annotation @GET("todos") để dán nhãn địa chỉ.
 * - FLOW: ViewModel gọi hàm getTodos() -> Retrofit phóng xe đến /todos -> Mang dữ liệu về.
 */
interface TodoApiService {

    /**
     * [Hành động]: Lấy danh sách việc cần làm.
     * - suspend: "Tạm dừng" - bảo Android đừng đợi làm đơ app, hãy chạy ngầm đi.
     * - Trả về: Một danh sách (List) các vật phẩm Todo.
     */
    @GET("todos")
    suspend fun getTodos(): List<Todo>

}
