package com.example.movie_booking_app.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.facebook.shimmer.ShimmerFrameLayout
import com.example.movie_booking_app.databinding.LayoutHomeNowPlayingBinding
import com.example.movie_booking_app.databinding.LayoutHomeComingSoonBinding
import com.example.movie_booking_app.databinding.LayoutHomePromoBinding
import com.example.movie_booking_app.databinding.LayoutHomeServicesBinding
import com.example.movie_booking_app.databinding.LayoutHomeMovieNewsBinding
import com.example.movie_booking_app.databinding.LayoutHomeVideosBinding
import com.example.movie_booking_app.databinding.LayoutHomeEmptyStateBinding

class HomeMultiAdapter(
    private val onSeeAllClicked: (String) -> Unit,
    private val onMovieClicked: (Movie) -> Unit
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(HomeItemDiffCallback()) {

    companion object {
        private const val typeNowPlaying = 1
        private const val typeComingSoon = 2
        private const val typePromo = 3
        private const val typeService = 4
        private const val typeNews = 5
        private const val typeEmpty = 6
        private const val typeVideos = 7
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.NowPlayingSection -> typeNowPlaying
            is HomeItem.ComingSoonSection -> typeComingSoon
            is HomeItem.PromoSection -> typePromo
            is HomeItem.ServiceSection -> typeService
            is HomeItem.MovieNewsSection -> typeNews
            is HomeItem.VideosSection -> typeVideos
            is HomeItem.EmptyState -> typeEmpty
            else -> typeEmpty
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeNowPlaying -> NowPlayingViewHolder(LayoutHomeNowPlayingBinding.inflate(inflater, parent, false), onSeeAllClicked, onMovieClicked)
            typeComingSoon -> ComingSoonViewHolder(LayoutHomeComingSoonBinding.inflate(inflater, parent, false), onSeeAllClicked, onMovieClicked)
            typePromo -> PromoViewHolder(LayoutHomePromoBinding.inflate(inflater, parent, false), onSeeAllClicked)
            typeService -> ServiceViewHolder(LayoutHomeServicesBinding.inflate(inflater, parent, false), onSeeAllClicked)
            typeNews -> NewsViewHolder(LayoutHomeMovieNewsBinding.inflate(inflater, parent, false), onSeeAllClicked)
            typeVideos -> VideosViewHolder(LayoutHomeVideosBinding.inflate(inflater, parent, false), onSeeAllClicked, onMovieClicked)
            typeEmpty -> EmptyViewHolder(LayoutHomeEmptyStateBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is NowPlayingViewHolder -> {
                val section = item as HomeItem.NowPlayingSection
                holder.bind(section.movies ?: emptyList(), section.isLoading)
            }
            is ComingSoonViewHolder -> {
                val section = item as HomeItem.ComingSoonSection
                holder.bind(section.movies ?: emptyList(), section.isLoading)
            }
            is PromoViewHolder -> {
                val section = item as HomeItem.PromoSection
                holder.bind(section.promos ?: emptyList(), section.isLoading)
            }
            is ServiceViewHolder -> {
                val section = item as HomeItem.ServiceSection
                holder.bind(section.services ?: emptyList(), section.isLoading)
            }
            is NewsViewHolder -> {
                val section = item as HomeItem.MovieNewsSection
                holder.bind(section.news ?: emptyList(), section.isLoading)
            }
            is VideosViewHolder -> {
                val section = item as HomeItem.VideosSection
                holder.bind(section.videos ?: emptyList(), section.isLoading)
            }
        }
    }

    class NowPlayingViewHolder(
        private val binding: LayoutHomeNowPlayingBinding,
        onSeeAll: (String) -> Unit,
        onMovie: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val viewPager: ViewPager2 = binding.vpNowPlaying
        private val tvSeeAll: TextView = binding.tvSeeAllNowPlaying
        private val indicatorTrack: View = binding.layoutIndicatorNowPlaying
        private val indicatorThumb: View = binding.viewIndicatorNowPlayingThumb
        private val layoutContent: View = binding.layoutContentNowPlaying
        private val shimmer: ShimmerFrameLayout = binding.shimmerNowPlaying
        private val adapter = MovieLargeAdapter(onMovie)
        private var isCallbackAdded = false

        init { 
            viewPager.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Now Playing") }
            
            viewPager.offscreenPageLimit = 3
            
            viewPager.setPageTransformer { page, position ->
                val r = 1 - Math.abs(position)
                page.scaleY = 0.85f + r * 0.15f
                page.scaleX = 0.85f + r * 0.15f
                page.alpha = 0.5f + r * 0.5f
                val density = page.context.resources.displayMetrics.density
                page.translationX = position * -32 * density
            }
        }

        fun bind(movies: List<Movie>, isLoading: Boolean) { 
            if (isLoading) {
                shimmer.visibility = View.VISIBLE
                shimmer.startShimmer()
                layoutContent.visibility = View.GONE
            } else {
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                layoutContent.visibility = View.VISIBLE
                if (movies.isNotEmpty()) {
                    val isFirstSubmit = adapter.currentList.isEmpty()
                    adapter.submitList(movies) {
                        if (isFirstSubmit) {
                            val size = movies.size
                            val middle = size * 500
                            viewPager.setCurrentItem(middle, false)
                        }
                    }
                    if (!isCallbackAdded) {
                        isCallbackAdded = true
                        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                                val size = movies.size
                                if (size > 1) {
                                    indicatorTrack.post {
                                        val totalScrollableWidth = indicatorTrack.width - indicatorThumb.width
                                        val progress = ((position % size) + positionOffset) / (size - 1)
                                        val safeProgress = progress.coerceIn(0f, 1f)
                                        indicatorThumb.translationX = safeProgress * totalScrollableWidth
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    class ComingSoonViewHolder(
        private val binding: LayoutHomeComingSoonBinding,
        onSeeAll: (String) -> Unit,
        onMovie: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val recyclerView: RecyclerView = binding.rvComingSoon
        private val tvSeeAll: TextView = binding.tvSeeAllComingSoon
        private val shimmer: ShimmerFrameLayout = binding.shimmerComingSoon
        private val adapter = MovieAdapter(onMovie)
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Coming Soon") }
        }
        fun bind(movies: List<Movie>, isLoading: Boolean) { 
            if (isLoading) {
                shimmer.visibility = View.VISIBLE
                shimmer.startShimmer()
                recyclerView.visibility = View.GONE
            } else {
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                if (movies.isNotEmpty()) adapter.submitList(movies) 
            }
        }
    }

    class PromoViewHolder(
        private val binding: LayoutHomePromoBinding,
        onSeeAll: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val viewPager: ViewPager2 = binding.vpPromo
        private val tvSeeAll: TextView = binding.tvSeeAllPromo
        private val shimmer: ShimmerFrameLayout = binding.shimmerPromo
        private val adapter = PromoAdapter()
        init { 
            viewPager.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Promo") }
        }
        fun bind(promos: List<Promo>, isLoading: Boolean) { 
            if (isLoading) {
                shimmer.visibility = View.VISIBLE
                shimmer.startShimmer()
                viewPager.visibility = View.GONE
            } else {
                shimmer.stopShimmer()
                shimmer.visibility = View.GONE
                viewPager.visibility = View.VISIBLE
                if (promos.isNotEmpty()) adapter.submitList(promos) 
            }
        }
    }

    class ServiceViewHolder(
        private val binding: LayoutHomeServicesBinding,
        onSeeAll: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val recyclerView: RecyclerView = binding.rvServices
        private val tvSeeAll: TextView = binding.tvSeeAllService
        private val adapter = ServiceAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Service") }
        }
        fun bind(services: List<Service>, isLoading: Boolean) { 
            if (services.isNotEmpty()) adapter.submitList(services) 
        }
    }

    class NewsViewHolder(
        private val binding: LayoutHomeMovieNewsBinding,
        onSeeAll: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val recyclerView: RecyclerView = binding.rvMovieNews
        private val tvSeeAll: TextView = binding.tvSeeAllNews
        private val adapter = MovieNewsAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("News") }
        }
        fun bind(news: List<MovieNews>, isLoading: Boolean) { 
            if (news.isNotEmpty()) adapter.submitList(news) 
        }
    }

    class VideosViewHolder(
        private val binding: LayoutHomeVideosBinding,
        onSeeAll: (String) -> Unit,
        onMovie: (Movie) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val recyclerView: RecyclerView = binding.rvVideos
        private val tvSeeAll: TextView = binding.tvSeeAllVideos
        private val adapter = VideoHorizontalAdapter { movie ->
            onSeeAll("Videos")
        }
        init {
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Videos") }
        }
        fun bind(movies: List<Movie>, isLoading: Boolean) {
            if (movies.isNotEmpty()) adapter.submitList(movies)
        }
    }

    class EmptyViewHolder(private val binding: LayoutHomeEmptyStateBinding) : RecyclerView.ViewHolder(binding.root)

    class HomeItemDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return when {
                oldItem is HomeItem.NowPlayingSection && newItem is HomeItem.NowPlayingSection -> true
                oldItem is HomeItem.ComingSoonSection && newItem is HomeItem.ComingSoonSection -> true
                oldItem is HomeItem.PromoSection && newItem is HomeItem.PromoSection -> true
                oldItem is HomeItem.ServiceSection && newItem is HomeItem.ServiceSection -> true
                oldItem is HomeItem.MovieNewsSection && newItem is HomeItem.MovieNewsSection -> true
                oldItem is HomeItem.VideosSection && newItem is HomeItem.VideosSection -> true
                oldItem is HomeItem.EmptyState && newItem is HomeItem.EmptyState -> true
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }
}
