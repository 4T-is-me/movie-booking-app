package com.example.testcinemabookingproject.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.testcinemabookingproject.data.model.Todo

/**
 * 📂 [DATA/DAO]: ÔNG QUẢN KHO (DATA ACCESS OBJECT)
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Một Interface định nghĩa các câu lệnh để làm việc với Database.
 * - MỤC ĐÍCH: Giúp Kotlin giao tiếp với Database thông qua ngôn ngữ SQL.
 * - BẰNG CÁCH NÀO: Dùng các nhãn @Dao, @Query, @Insert.
 * - FLOW: ViewModel/Repository gọi hàm -> DAO thực thi lệnh SQL -> Database trả kết quả.
 */
@Dao
interface TodoDAO {

    /**
     * [Lấy dữ liệu]: Đọc toàn bộ danh sách Todo trong máy.
     * - @Query: Viết lệnh SQL thuần.
     * - suspend: Chạy ngầm dưới nền để không làm đơ app.
     */
    @Query("SELECT * FROM todo_table")
    suspend fun getAllTodos(): List<Todo>

    /**
     * [Lưu dữ liệu]: Cất danh sách Todo vào trong kho.
     * - OnConflictStrategy.REPLACE: Nếu trùng mã ID, hãy lấy cái mới đè lên cái cũ.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertALL(todos: List<Todo>)
}
