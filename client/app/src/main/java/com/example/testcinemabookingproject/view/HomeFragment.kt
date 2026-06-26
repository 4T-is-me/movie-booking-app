package com.example.testcinemabookingproject.view

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.testcinemabookingproject.adapter.MovieAdapter
import com.example.testcinemabookingproject.databinding.FragmentHomeBinding
import com.example.testcinemabookingproject.data.model.Movie
import com.example.testcinemabookingproject.R

/**
 * HOME FRAGMENT: Mảnh ghép chứa danh sách phim.
 * Fragment này sẽ được "dán" vào MainActivity.
 */
class HomeFragment : Fragment() {

    // 1. Khai báo biến _binding (Cái kẹp hồ sơ nội bộ)
    private var _binding: FragmentHomeBinding? = null
    
    // 2. Tạo biến binding (Lối đi tắt để sử dụng)
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 3. Hành động: Thổi phồng giao diện XML của Fragment
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieList = listOf(
            Movie(1, "Avengers: Endgame", 4.8, R.drawable.avengers),
            Movie(2, "Inception", 4.7, R.drawable.inception),
            Movie(3, "Spider-Man: No Way Home", 4.5, R.drawable.spiderman),
            Movie(4, "The Dark Knight", 4.9, R.drawable.thedarkknight)
        )

        // 3. Chuẩn bị "Anh đầu bếp" (Adapter)
        val movieAdapter = MovieAdapter(movieList) { movie ->

            // [Hành động]: Mượn "môi trường" (Context) từ Activity để tạo Intent
            val intent = Intent(requireContext(), MovieDetailActivity::class.java)

            // putExtra: "Kẹp" thêm dữ liệu vào bức thư
            intent.putExtra(MovieDetailActivity.EXTRA_NAME, movie.title)
            intent.putExtra(MovieDetailActivity.EXTRA_RATING, movie.rating)

            // Lệnh xuất phát!
            startActivity(intent)
        }
        binding.rvMovies.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = movieAdapter
            setHasFixedSize(true)
        }

        binding.ivGoToPractice.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.mainContainer, TodoFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 5. Hành động: Dọn dẹp bộ nhớ khi mảnh ghép bị tháo ra
        _binding = null
    }
}
