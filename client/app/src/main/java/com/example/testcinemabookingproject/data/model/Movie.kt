package com.example.testcinemabookingproject.data.model

/**
 * DATA CLASS: Là một loại lớp đặc biệt trong Kotlin chỉ dùng để chứa dữ liệu.
 * Nó tự động tạo các hàm như equals(), toString() giúp lập trình viên nhàn hơn.
 */
data class Movie(
    val id: Int,             // Mã định danh duy nhất cho mỗi bộ phim (để không bị nhầm)
    val title: String,       // Tên bộ phim (kiểu chuỗi văn bản)
    val rating: Double,      // Điểm đánh giá (kiểu số thực, ví dụ: 4.5)
    val imageResId: Int      // ID của ảnh trong thư mục 'drawable' (trong Android, ảnh được quản lý bằng số Int)
)
