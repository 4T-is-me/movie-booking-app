package com.example.testcinemabookingproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.testcinemabookingproject.data.model.Todo
import com.example.testcinemabookingproject.databinding.ItemTodoBinding

/**
 * : NGƯỜI BỒI BÀN ĐIỀU PHỐI DANH SÁCH
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Lớp trung gian giữa Dữ liệu (List) và Giao diện (RecyclerView).
 * - MỤC ĐÍCH: Biến đống dữ liệu thô thành các ô hiển thị đẹp đẽ trên màn hình.
 * - BẰNG CÁCH NÀO: Nhân bản file XML và dán dữ liệu vào từng ô.
 * - FLOW: Nhận List -> Tạo khuôn (ViewHolder) -> Dán dữ liệu (Bind) -> Hiện lên màn hình.
 */
class TodoAdapter(private val todos: List<Todo>) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    /**
     * [Cái Hộp]: Giữ các linh kiện (View) của 1 dòng để tái sử dụng.
     */
    class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * [Hành động]: Tạo ra cái khuôn bánh (Thổi phồng XML).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        // Biến file item_todo.xml thành một đối tượng code Kotlin
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    /**
     * [Hành động]: Đổ bột vào khuôn (Dán dữ liệu thật vào giao diện).
     */
    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = todos[position] // Lấy món đồ thứ n trong danh sách
        holder.binding.apply {
            // Gán chữ tiêu đề
            tvTodoTitle.text = item.title
            // Gán trạng thái tích xanh (đúng/sai)
            cbCompleted.isChecked = item.completed
        }
    }

    /**
     * [Hành động]: Đếm xem có tổng cộng bao nhiêu món hàng.
     */
    override fun getItemCount(): Int = todos.size
}
