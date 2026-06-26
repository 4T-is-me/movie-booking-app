package com.example.testcinemabookingproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.testcinemabookingproject.databinding.ItemSeatBinding
import com.example.testcinemabookingproject.data.model.Seat

/**
 * ADAPTER CHO GHẾ: Đóng vai trò "Anh thợ sơn" kiêm "Người xếp chỗ".
 * Anh thợ này nhận danh sách ghế từ "Bộ não" (ViewModel) và quyết định 
 * từng ô ghế trên màn hình sẽ hiện chữ gì, màu gì.
 */
class SeatAdapter(
    private var seats: List<Seat>,           // 1. Danh sách 40 cái ghế (Hàng hóa cần trưng bày)
    private val onSeatClick: (Seat) -> Unit  // 2. Tín hiệu báo cáo khi ghế bị bấm (Cái bộ đàm)
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    /**
     * VIEWHOLDER: "Cái hộp bảo quản View".
     * Thay vì mỗi lần cuộn màn hình phải đi tìm ID từng cái TextView (rất tốn sức),
     * cái hộp này giữ sẵn các View đó trong bộ nhớ để lôi ra dùng ngay.
     */
    class SeatViewHolder(val binding: ItemSeatBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * HÀNH ĐỘNG: "Xây xưởng đúc ghế".
     * Biến file XML (bản vẽ trên giấy) thành vật thể thực tế có thể nhìn thấy.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        // - inflate: Máy "thổi phồng" XML thành đối tượng code Kotlin.
        // - Nếu không có dòng này: Android sẽ không biết lấy cái khung nào để hiện cái ghế.
        val binding = ItemSeatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SeatViewHolder(binding)
    }

    /**
     * HÀNH ĐỘNG: "Trang trí từng cái ghế".
     * Đây là nơi logic "ghép chữ" và "sơn màu" thực sự diễn ra cho từng vị trí.
     */
    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        // - position: Số thứ tự cái ghế Android bảo mình phải vẽ (từ 0 đến 39).
        val seat = seats[position] // Thò tay vào danh sách, lấy cái ghế ở đúng số thứ tự đó ra.
        
        holder.binding.apply {
            // 1. GÁN CHỮ (Dán nhãn):
            // - Lấy chữ hàng (A) ghép với số (1) thành "A1".
            // - Nếu không có dòng này: Mọi cái ghế đều hiện chữ "A1" mặc định của XML.
            tvSeatLabel.text = "${seat.row}${seat.number}"

            // 2. CHỌN MÀU SƠN (Logic nghiệp vụ):
            // - when: Giống như một cái máy lọc.
            // - Nếu đã bị mua (isBooked) -> Lấy sơn Đỏ.
            // - Nếu khách đang chọn (isSelected) -> Lấy sơn Xanh.
            // - Nếu trống -> Lấy sơn Xám.
            val colorRes = when {
                seat.isBooked -> android.R.color.holo_red_light
                seat.isSelected -> android.R.color.holo_green_light
                else -> android.R.color.darker_gray
            }
            
            // 3. QUÉT SƠN (Thay đổi giao diện):
            // - setCardBackgroundColor: Quét màu đã chọn lên nền cái CardView.
            // - ContextCompat.getColor: Lệnh đi lấy màu từ kho hệ thống một cách an toàn.
            cardSeat.setCardBackgroundColor(ContextCompat.getColor(root.context, colorRes))

            // 4. CÀI CẢM ỨNG (Bắt sự kiện):
            // - root.setOnClickListener: Dán miếng cảm biến lên toàn bộ cái ghế.
            // - onSeatClick(seat): Hễ khách chạm vào là dùng "bộ đàm" báo về cho Activity.
            root.setOnClickListener {
                onSeatClick(seat)
            }
        }
    }

    /**
     * HÀNH ĐỘNG: "Kiểm kho".
     * Báo cho hệ thống biết tổng cộng có bao nhiêu cái ghế để Android chuẩn bị bộ nhớ.
     */
    override fun getItemCount(): Int = seats.size

    /**
     * HÀNH ĐỘNG: "Làm mới toàn bộ rạp phim".
     * Khi danh sách ghế thay đổi (có người chọn thêm ghế), ta thay danh sách cũ bằng mới.
     */
    fun updateSeats(newSeats: List<Seat>) {
        this.seats = newSeats
        // - notifyDataSetChanged: Lệnh "Xóa đi vẽ lại tất cả!". 
        // - Nếu không có dòng này: Bạn chọn ghế xong nhưng màn hình vẫn hiện màu cũ.
        notifyDataSetChanged()
    }
}
