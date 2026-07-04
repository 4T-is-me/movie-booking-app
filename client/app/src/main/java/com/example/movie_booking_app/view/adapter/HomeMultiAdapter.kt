package com.example.movie_booking_app.view.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.entity.MovieNews
import com.example.movie_booking_app.data.model.entity.Promo
import com.example.movie_booking_app.data.model.entity.Service
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.facebook.shimmer.ShimmerFrameLayout

class HomeMultiAdapter(
    private val onSearchQueryChanged: (String) -> Unit,
    private val onSeeAllClicked: (String) -> Unit,
    private val onMovieClicked: (Movie) -> Unit,
    private val onLoginClicked: () -> Unit
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(HomeItemDiffCallback()) {

    companion object {
        private const val typeHeader = 0
        private const val typeNowPlaying = 1
        private const val typeComingSoon = 2
        private const val typePromo = 3
        private const val typeService = 4
        private const val typeNews = 5
        private const val typeEmpty = 6
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.HeaderSection -> typeHeader
            is HomeItem.NowPlayingSection -> typeNowPlaying
            is HomeItem.ComingSoonSection -> typeComingSoon
            is HomeItem.PromoSection -> typePromo
            is HomeItem.ServiceSection -> typeService
            is HomeItem.MovieNewsSection -> typeNews
            is HomeItem.EmptyState -> typeEmpty
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeHeader -> HeaderViewHolder(inflater.inflate(R.layout.layout_home_header, parent, false), onSearchQueryChanged, onLoginClicked)
            typeNowPlaying -> NowPlayingViewHolder(inflater.inflate(R.layout.layout_home_now_playing, parent, false), onSeeAllClicked, onMovieClicked)
            typeComingSoon -> ComingSoonViewHolder(inflater.inflate(R.layout.layout_home_coming_soon, parent, false), onSeeAllClicked, onMovieClicked)
            typePromo -> PromoViewHolder(inflater.inflate(R.layout.layout_home_promo, parent, false), onSeeAllClicked)
            typeService -> ServiceViewHolder(inflater.inflate(R.layout.layout_home_services, parent, false), onSeeAllClicked)
            typeNews -> NewsViewHolder(inflater.inflate(R.layout.layout_home_movie_news, parent, false), onSeeAllClicked)
            typeEmpty -> EmptyViewHolder(inflater.inflate(R.layout.layout_home_empty_state, parent, false))
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HeaderViewHolder -> holder.bind((item as HomeItem.HeaderSection).userName)
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
        }
    }

    class HeaderViewHolder(view: View, onQueryChanged: (String) -> Unit, onLogin: () -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvHi: TextView = view.findViewById(R.id.tvHi)
        private val edtSearch: EditText = view.findViewById(R.id.edtSearch)
        private val btnLogin: View = view.findViewById(R.id.btnLogin)
        init {
            edtSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onQueryChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            btnLogin.setOnClickListener { onLogin() }
        }
        fun bind(name: String) { 
            tvHi.text = "Hi, $name 👋" 
            val token = com.example.movie_booking_app.data.local.TokenManager.getAccessToken(itemView.context)
            if (token != null) {
                btnLogin.visibility = View.GONE
            } else {
                btnLogin.visibility = View.VISIBLE
            }
        }
    }

    class NowPlayingViewHolder(view: View, onSeeAll: (String) -> Unit, onMovie: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val viewPager: ViewPager2 = view.findViewById(R.id.vpNowPlaying)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllNowPlaying)
        private val indicatorTrack: View = view.findViewById(R.id.layoutIndicatorNowPlaying)
        private val indicatorThumb: View = view.findViewById(R.id.viewIndicatorNowPlayingThumb)
        private val layoutContent: View = view.findViewById(R.id.layoutContentNowPlaying)
        private val shimmer: ShimmerFrameLayout = view.findViewById(R.id.shimmerNowPlaying)
        private val adapter = MovieLargeAdapter(onMovie)
        private var isCallbackAdded = false

        init { 
            viewPager.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Now Playing") }
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
                    adapter.submitList(movies)
                    if (!isCallbackAdded) {
                        isCallbackAdded = true
                        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                                val itemCount = adapter.itemCount
                                if (itemCount > 1) {
                                    indicatorTrack.post {
                                        val totalScrollableWidth = indicatorTrack.width - indicatorThumb.width
                                        val progress = (position + positionOffset) / (itemCount - 1)
                                        indicatorThumb.translationX = progress * totalScrollableWidth
                                    }
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    class ComingSoonViewHolder(view: View, onSeeAll: (String) -> Unit, onMovie: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvComingSoon)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllComingSoon)
        private val shimmer: ShimmerFrameLayout = view.findViewById(R.id.shimmerComingSoon)
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

    class PromoViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val viewPager: ViewPager2 = view.findViewById(R.id.vpPromo)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllPromo)
        private val shimmer: ShimmerFrameLayout = view.findViewById(R.id.shimmerPromo)
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

    class ServiceViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvServices)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllService)
        private val adapter = ServiceAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Service") }
        }
        fun bind(services: List<Service>, isLoading: Boolean) { 
            if (services.isNotEmpty()) adapter.submitList(services) 
        }
    }

    class NewsViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvMovieNews)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllNews)
        private val adapter = MovieNewsAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("News") }
        }
        fun bind(news: List<MovieNews>, isLoading: Boolean) { 
            if (news.isNotEmpty()) adapter.submitList(news) 
        }
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    class HomeItemDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return when {
                oldItem is HomeItem.HeaderSection && newItem is HomeItem.HeaderSection -> true
                oldItem is HomeItem.NowPlayingSection && newItem is HomeItem.NowPlayingSection -> true
                oldItem is HomeItem.ComingSoonSection && newItem is HomeItem.ComingSoonSection -> true
                oldItem is HomeItem.PromoSection && newItem is HomeItem.PromoSection -> true
                oldItem is HomeItem.ServiceSection && newItem is HomeItem.ServiceSection -> true
                oldItem is HomeItem.MovieNewsSection && newItem is HomeItem.MovieNewsSection -> true
                oldItem is HomeItem.EmptyState && newItem is HomeItem.EmptyState -> true
                else -> false
            }
        }
        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }
}
