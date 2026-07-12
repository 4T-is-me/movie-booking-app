package com.example.movie_booking_app.view.activity

import android.graphics.Rect
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.databinding.ActivityVideoListBinding
import com.example.movie_booking_app.view.adapter.VideoListAdapter
import com.example.movie_booking_app.viewmodel.MovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class VideoListActivity : BaseActivity() {

    private lateinit var binding: ActivityVideoListBinding
    private val viewModel: MovieViewModel by viewModels()
    private lateinit var videoAdapter: VideoListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        videoAdapter = VideoListAdapter(lifecycle)
        binding.rvVideos.apply {
            layoutManager = LinearLayoutManager(this@VideoListActivity)
            adapter = videoAdapter

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScrollToPause()
                }
            })
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.homeItems.collect { items ->
                    val allMovies = mutableListOf<Movie>()
                    items.forEach { item ->
                        when (item) {
                            is HomeItem.NowPlayingSection -> {
                                item.movies?.let { allMovies.addAll(it) }
                            }
                            is HomeItem.ComingSoonSection -> {
                                item.movies?.let { allMovies.addAll(it) }
                            }
                            else -> {}
                        }
                    }
                    val uniqueMovies = allMovies.distinctBy { it.id }
                    videoAdapter.submitList(uniqueMovies)
                }
            }
        }
    }

    private fun checkScrollToPause() {
        val layoutManager = binding.rvVideos.layoutManager as? LinearLayoutManager ?: return
        val activePos = videoAdapter.getActivePlayerPosition()
        if (activePos == -1) return

        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()

        if (activePos < firstVisible || activePos > lastVisible) {
            // Out of bounds, release
            videoAdapter.releaseActivePlayer()
        } else {
            // Inside visible bounds, verify height ratio
            val holder = binding.rvVideos.findViewHolderForAdapterPosition(activePos) as? VideoListAdapter.VideoViewHolder
            holder?.let {
                val rect = Rect()
                val isVisible = it.itemView.getGlobalVisibleRect(rect)
                val height = it.itemView.height
                if (!isVisible || rect.height() < height / 2) {
                    videoAdapter.releaseActivePlayer()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        videoAdapter.pauseActivePlayer()
    }

    override fun onStop() {
        super.onStop()
        videoAdapter.releaseActivePlayer()
    }
}
