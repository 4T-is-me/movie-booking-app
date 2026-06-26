package com.example.testcinemabookingproject.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.testcinemabookingproject.databinding.ItemMovieBinding
import com.example.testcinemabookingproject.data.model.Movie

/**
 * ADAPTER: Đóng vai trò cầu nối. Nó lấy danh sách dữ liệu (movies) 
 * và "đổ" vào từng ô giao diện (item_movie.xml).
 */
class MovieAdapter(
    private val movies: List<Movie>,        // Danh sách các bộ phim cần hiển thị
    private val onItemClick: (Movie) -> Unit // Một hành động (Lambda) sẽ chạy khi ta bấm vào phim
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    /**
     * VIEWHOLDER: "Cái hộp" giữ các View. Thay vì mỗi lần cuộn lại phải tìm ID, 
     * nó giữ sẵn tvTitle, ivPoster... trong bộ nhớ để cuộn cho mượt.
     */
    class MovieViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)

    // Bước 1: Tạo ra "Cái hộp" (ViewHolder)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        // 'layoutInflater' là máy thổi phồng file XML thành code Kotlin có thể dùng được
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    // Bước 2: "Dán" dữ liệu vào "Cái hộp"
    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position] // Lấy bộ phim tại vị trí hiện tại
        
        // Sử dụng 'binding' để truy cập các ID trong file item_movie.xml
        holder.binding.apply {
            tvTitle.text = movie.title       // Gán tên phim vào TextView
            tvRating.text = "⭐ ${movie.rating}" // Gán điểm vào TextView
            ivPoster.setImageResource(movie.imageResId) // Gán ảnh vào ImageView

            // Bắt sự kiện khi người dùng click vào toàn bộ ô phim (root)
            root.setOnClickListener {
                onItemClick(movie) // Gọi hành động đã giao kèo ở MainActivity
            }
        }
    }

    // Bước 3: Báo cho Android biết có tổng cộng bao nhiêu món (phim) để chuẩn bị đĩa
    override fun getItemCount(): Int = movies.size
}
