package com.example.testcinemabookingproject.data.repository

import com.example.testcinemabookingproject.data.api.TodoApiService
import com.example.testcinemabookingproject.data.model.Todo
import com.example.testcinemabookingproject.data.TodoDAO

/**
 * 🏢 [REPOSITORY]: QUẢN LÝ DỮ LIỆU TỔNG HỢP
 * - Đây là mắt xích quan trọng nhất trong Clean Architecture.
 * - Nó quyết định lấy dữ liệu từ đâu (Mạng hay Máy).
 */
class TodoRepository(
    private val apiService: TodoApiService,
    private val todoDao: TodoDAO
) {

    /**
     * [Hành động]: Lấy Todo từ mạng và lưu ngay vào Database.
     */
    suspend fun refreshTodos() {
        try {
            // 1. Lấy hàng từ mạng
            val response = apiService.getTodos()
            // 2. Cất hàng vào két sắt (Database)
            todoDao.insertALL(response)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * [Hành động]: Lấy dữ liệu từ két sắt (Database) trong máy.
     */
    suspend fun getTodosFromLocal(): List<Todo> {
        return todoDao.getAllTodos()
    }
}
