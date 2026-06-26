package com.example.testcinemabookingproject.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testcinemabookingproject.adapter.TodoAdapter
import com.example.testcinemabookingproject.databinding.FragmentTodoBinding
import com.example.testcinemabookingproject.viewmodel.TodoViewModel

/**
 *  [VIEW/FRAGMENT]: MẶT TIỀN HIỂN THỊ DANH SÁCH TODO
 * -----------------------------------------------------------
 * - ĐÂY LÀ GÌ: Mảnh ghép giao diện hiển thị danh sách công việc.
 * - MỤC ĐÍCH: Là nơi tương tác trực tiếp với người dùng.
 * - BẰNG CÁCH NÀO: Kết nối với ViewModel để lấy dữ liệu và dùng RecyclerView để hiển thị.
 * - FLOW: Sinh ra -> Nghe ngóng ViewModel -> Có hàng thì bảo Adapter vẽ lên màn hình.
 */
class TodoFragment : Fragment() {

    // 1. [Binding]: Cái kẹp hồ sơ để điều khiển các linh kiện trong XML
    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!

    // 2. [ViewModel]: Triệu hồi bộ não chuyên lo việc lấy dữ liệu Todo
    private val viewModel: TodoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // [Hành động]: Thổi phồng file fragment_todo.xml
        _binding = FragmentTodoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * [Hành động]: Đăng ký nghe loa phát thanh (Observation).
         * Hễ danh sách trong ViewModel thay đổi, khối code này sẽ tự chạy.
         */
        viewModel.todos.observe(viewLifecycleOwner) { list ->
            // Thuê bồi bàn (Adapter) và giao danh sách cho họ
            binding.rvTodos.adapter = TodoAdapter(list)
            
            // Quy định cách xếp hàng (LayoutManager) là Hàng dọc
            binding.rvTodos.layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // [Hành động]: Dọn dẹp để tiết kiệm bộ nhớ
        _binding = null
    }
}
