package com.example.testcinemabookingproject.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.testcinemabookingproject.data.model.Seat

/**
 * SEAT VIEW MODEL: "Bộ não" của màn hình chọn ghế.
 * Nhiệm vụ: Quản lý trạng thái của 40 cái ghế và xử lý logic khi người dùng bấm chọn.
 * ViewModel giúp dữ liệu không bị mất khi điện thoại xoay ngang/dọc.
 */
class SeatViewModel : ViewModel() {

    /**
     * 1. MutableLiveData (Dữ liệu nội bộ):
     * - Đây là "cái hộp" chứa danh sách ghế mà CHỈ ViewModel mới có quyền sửa đổi.
     * - Dấu gạch dưới '_' là quy ước đặt tên cho biến private trong MVVM.
     */
    private val _seats = MutableLiveData<List<Seat>>()

    /**
     * 2. LiveData (Dữ liệu công khai):
     * - Đây là "cửa sổ" để Activity nhìn vào và quan sát (Observe).
     * - Activity chỉ có quyền ĐỌC dữ liệu từ đây, không có quyền sửa.
     */
    val seats: LiveData<List<Seat>> get() = _seats

    // Khối 'init' chạy ngay khi ViewModel được tạo ra
    init {
        createTheaterSeats()
    }


    private val _selectedCount = MutableLiveData<Int>(0)
    val selectedCount: LiveData<Int> get()= _selectedCount

    private val _totalPrice= MutableLiveData<Int>(0)
    val totalPrice: LiveData<Int> get()= _totalPrice

    private val TICKET_PRICE =100000



    /**
     *
     * Hàm khởi tạo 40 cái ghế (5 hàng x 8 cột) cho rạp phim.
     */
    private fun createTheaterSeats() {
        val seatList = mutableListOf<Seat>()
        val rows = listOf("A", "B", "C", "D", "E") // 5 hàng ghế
        
        for (row in rows) {
            for (num in 1..8) { // 8 ghế mỗi hàng
                
                // Giả lập logic: Ghế A4 và A5 đã có người đặt trước (isBooked = true)
                val isReserved = (row == "A" && (num == 4 || num == 5))
                
                // Tạo một đối tượng Seat mới và thêm vào danh sách
                seatList.add(Seat(row, num, isReserved))
            }
        }
        
        // Gán danh sách vừa tạo vào LiveData để báo hiệu cho giao diện hiển thị
        _seats.value = seatList
    }

    /**
     * Logic xử lý khi người dùng click vào một cái ghế.
     * @param seat: Cái ghế vừa được bấm vào.
     */
    fun toggleSeat(seat: Seat) {
        // Lấy danh sách hiện tại ra, chuyển thành Mutable để có thể sửa
        val currentList = _seats.value?.toMutableList() ?: return
        
        // Tìm vị trí của cái ghế đó trong danh sách dựa vào ID (VD: "A1")
        val index = currentList.indexOfFirst { it.id == seat.id }




        // Kiểm tra: Nếu tìm thấy ghế VÀ ghế đó chưa bị ai đặt (isBooked = false)
        if (index != -1 && !currentList[index].isBooked) {

            /**
             * .copy(): Tạo một bản sao của ghế cũ nhưng đảo ngược trạng thái 'isSelected'.
             * Nếu đang chọn thì thành bỏ chọn, và ngược lại.
             */
            val updatedSeat = currentList[index].copy(isSelected = !currentList[index].isSelected)
            
            // Thay thế ghế cũ bằng bản sao đã cập nhật
            currentList[index] = updatedSeat

            // Cập nhật lại cái hộp LiveData. Lệnh này sẽ kích hoạt UI vẽ lại màu ghế.
            _seats.value = currentList

            val count = currentList.count{it.isSelected}
            _selectedCount.value =count
            _totalPrice.value=count*TICKET_PRICE
        }


    }



}
