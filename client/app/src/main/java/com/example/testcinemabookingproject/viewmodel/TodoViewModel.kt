package com.example.testcinemabookingproject.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.testcinemabookingproject.data.AppDatabase
import com.example.testcinemabookingproject.data.model.Todo
import com.example.testcinemabookingproject.data.repository.RetrofitClient
import com.example.testcinemabookingproject.data.repository.TodoRepository
import kotlinx.coroutines.launch

/**
 * 🧠 [VIEWMODEL]: BỘ NÃO ĐIỀU HÀNH DỮ LIỆU
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Lớp quản lý trạng thái của danh sách Todo.
 * - MỤC ĐÍCH: Giữ dữ liệu sống sót khi xoay màn hình.
 * - CẢI TIẾN: Bây giờ không gọi trực tiếp Retrofit nữa mà thông qua Repository.
 */
class TodoViewModel(application: Application) : AndroidViewModel(application) {

    // 1. Khởi tạo "Két sắt" và "Người quản kho"
    private val database = AppDatabase.getDatabase(application)
    private val todoDao = database.todoDao()
    
    // 2. Khởi tạo "Trưởng phòng vật tư" (Repository)
    // Nó cần 2 thứ: ApiService (từ mạng) và DAO (từ máy)
    private val repository = TodoRepository(RetrofitClient.apiService, todoDao)

    private val _todos = MutableLiveData<List<Todo>>()
    val todos: LiveData<List<Todo>> get() = _todos

    init {
        loadData()
    }

    /**
     * [CHIẾN THUẬT]: LẤY DỮ LIỆU THÔNG MINH
     * 1. Hiện dữ liệu cũ trong máy ra trước cho người dùng xem ngay.
     * 2. Chạy ngầm đi lấy dữ liệu mới từ mạng về cập nhật vào máy.
     * 3. Sau khi cập nhật máy xong, hiển thị lại dữ liệu mới nhất.
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Bước 1: Lấy hàng từ "Két sắt" (Local) hiện lên trước
                val localData = repository.getTodosFromLocal()
                _todos.value = localData

                // Bước 2: Đi "Nhập hàng" mới từ mạng về (Refresh)
                repository.refreshTodos()

                // Bước 3: Lấy lại hàng trong két (lúc này đã có hàng mới) để hiển thị lại
                _todos.value = repository.getTodosFromLocal()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
