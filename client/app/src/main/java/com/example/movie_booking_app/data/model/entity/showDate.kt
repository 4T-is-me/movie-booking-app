package com.example.movie_booking_app.data.model.entity

data class ShowDate(
    val day: String,
    val dayOfWeek: String,
    val month: String,
    val year: String,
    var isSelected: Boolean = false
) {
    /**
     * Logic tách rời: Chuyển đổi đối tượng Ngày sang chuỗi ID chuẩn cho API.
     */
    fun toApiId(): String = "$year-$month-$day"
}
