package com.example.testcinemabookingproject.data.model

data class Seat(
    val row: String,      // Hàng ghế (A, B, C...)
    val number: Int,      // Số ghế (1, 2, 3...)
    val isBooked: Boolean = false,   // Đã có người đặt chưa?
    var isSelected: Boolean = false  // Người dùng hiện tại có đang chọn không?
) {
    // Biến id để định danh duy nhất cho ghế, ví dụ "A1"
    val id: String get() = "$row$number"
}
