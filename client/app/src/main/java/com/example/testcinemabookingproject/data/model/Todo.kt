package com.example.testcinemabookingproject.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 📂 MODEL: ĐỊNH NGHĨA DỮ LIỆU "VIỆC CẦN LÀM"
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Một bản thiết kế (data class) khớp with dữ liệu từ JSONPlaceholder.
 * - MỤC ĐÍCH: Giúp Kotlin hiểu được cấu trúc của một món hàng lấy từ mạng về.
 * - BẰNG CÁCH NÀO: Khai báo các biến (val) có tên GIỐNG HỆT các phím (keys) trong chuỗi JSON.
 * - FLOW: Internet (JSON) -> GSON dịch -> Đối tượng Todo này -> ViewModel dùng.
 */
@Entity(tableName = "todo_table")
data class Todo(
    val userId: Int,        // ID của người chủ công việc
    
    @PrimaryKey             // Mã định danh duy nhất của công việc đó (Khóa chính)
    val id: Int,            
    
    val title: String,      // Tên/Nội dung công việc
    val completed: Boolean  // Đã xong chưa? (Đúng/Sai)
)
