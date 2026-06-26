package com.example.movie_booking_app.view.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.entity.Movie
import com.example.movie_booking_app.data.model.ui.HomeItem
import com.example.movie_booking_app.data.model.ui.MovieNews
import com.example.movie_booking_app.data.model.ui.Promo
import com.example.movie_booking_app.data.model.ui.Service
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeMultiAdapter(
    private val onSearchQueryChanged: (String) -> Unit,
    private val onSeeAllClicked: (String) -> Unit,
    private val onMovieClicked: (Movie) -> Unit,
    private val onLoginClicked: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items = listOf<HomeItem>()

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_NOW_PLAYING = 1
        private const val TYPE_COMING_SOON = 2
        private const val TYPE_PROMO = 3
        private const val TYPE_SERVICE = 4
        private const val TYPE_NEWS = 5
        private const val TYPE_EMPTY = 6
    }

    fun submitList(newItems: List<HomeItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HomeItem.HeaderSection -> TYPE_HEADER
            is HomeItem.NowPlayingSection -> TYPE_NOW_PLAYING
            is HomeItem.ComingSoonSection -> TYPE_COMING_SOON
            is HomeItem.PromoSection -> TYPE_PROMO
            is HomeItem.ServiceSection -> TYPE_SERVICE
            is HomeItem.MovieNewsSection -> TYPE_NEWS
            is HomeItem.EmptyState -> TYPE_EMPTY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.layout_home_header, parent, false), onSearchQueryChanged, onLoginClicked)
            TYPE_NOW_PLAYING -> NowPlayingViewHolder(inflater.inflate(R.layout.layout_home_now_playing, parent, false), onSeeAllClicked, onMovieClicked)
            TYPE_COMING_SOON -> ComingSoonViewHolder(inflater.inflate(R.layout.layout_home_coming_soon, parent, false), onSeeAllClicked, onMovieClicked)
            TYPE_PROMO -> PromoViewHolder(inflater.inflate(R.layout.layout_home_promo, parent, false), onSeeAllClicked)
            TYPE_SERVICE -> ServiceViewHolder(inflater.inflate(R.layout.layout_home_services, parent, false), onSeeAllClicked)
            TYPE_NEWS -> NewsViewHolder(inflater.inflate(R.layout.layout_home_movie_news, parent, false), onSeeAllClicked)
            TYPE_EMPTY -> EmptyViewHolder(inflater.inflate(R.layout.layout_home_empty_state, parent, false))
            else -> throw IllegalArgumentException("Invalid ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is HeaderViewHolder -> holder.bind((item as HomeItem.HeaderSection).userName)
            is NowPlayingViewHolder -> holder.bind((item as HomeItem.NowPlayingSection).movies)
            is ComingSoonViewHolder -> holder.bind((item as HomeItem.ComingSoonSection).movies)
            is PromoViewHolder -> holder.bind((item as HomeItem.PromoSection).promos)
            is ServiceViewHolder -> holder.bind((item as HomeItem.ServiceSection).services)
            is NewsViewHolder -> holder.bind((item as HomeItem.MovieNewsSection).news)
        }
    }

    class HeaderViewHolder(view: View, onQueryChanged: (String) -> Unit, onLogin: () -> Unit) : RecyclerView.ViewHolder(view) {
        private val tvHi: TextView = view.findViewById(R.id.tvHi)
        private val edtSearch: EditText = view.findViewById(R.id.edtSearch)
        private val btnLogin: View = view.findViewById(R.id.btnLogin)
        init {
            // Xử lý Safe Area cho Header (Độ cao tai thỏ + 16dp đệm thiết kế)
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val density = v.context.resources.displayMetrics.density
                val extraPadding = (16 * density).toInt() // Chuyển 16dp sang Pixel
                v.updatePadding(top = insets.top + extraPadding)
                windowInsets
            }

            edtSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onQueryChanged(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: Editable?) {}
            })
            btnLogin.setOnClickListener { onLogin() }
        }
        fun bind(name: String) { tvHi.text = "Hi, $name 👋" }
    }

    class NowPlayingViewHolder(view: View, onSeeAll: (String) -> Unit, onMovie: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val viewPager: ViewPager2 = view.findViewById(R.id.vpNowPlaying)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllNowPlaying)
        private val indicator: TabLayout = view.findViewById(R.id.indicatorNowPlaying)
        private val adapter = MovieLargeAdapter(onMovie)
        private var mediator: TabLayoutMediator? = null

        init { 
            viewPager.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Now Playing") }
        }

        fun bind(movies: List<Movie>) { 
            if (movies.isNotEmpty()) {
                adapter.submitList(movies)
                if (mediator == null) {
                    mediator = TabLayoutMediator(indicator, viewPager) { _, _ -> }
                    mediator?.attach()
                }
            } 
        }
    }

    class ComingSoonViewHolder(view: View, onSeeAll: (String) -> Unit, onMovie: (Movie) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvComingSoon)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllComingSoon)
        private val adapter = MovieAdapter(onMovie)
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Coming Soon") }
        }
        fun bind(movies: List<Movie>) { if (movies.isNotEmpty()) adapter.submitList(movies) }
    }

    class PromoViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val viewPager: ViewPager2 = view.findViewById(R.id.vpPromo)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllPromo)
        private val adapter = PromoAdapter()
        init { 
            viewPager.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Promo") }
        }
        fun bind(promos: List<Promo>) { if (promos.isNotEmpty()) adapter.submitList(promos) }
    }

    class ServiceViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvServices)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllService)
        private val adapter = ServiceAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("Service") }
        }
        fun bind(services: List<Service>) { if (services.isNotEmpty()) adapter.submitList(services) }
    }

    class NewsViewHolder(view: View, onSeeAll: (String) -> Unit) : RecyclerView.ViewHolder(view) {
        private val recyclerView: RecyclerView = view.findViewById(R.id.rvMovieNews)
        private val tvSeeAll: TextView = view.findViewById(R.id.tvSeeAllNews)
        private val adapter = MovieNewsAdapter()
        init { 
            recyclerView.adapter = adapter
            tvSeeAll.setOnClickListener { onSeeAll("News") }
        }
        fun bind(news: List<MovieNews>) { if (news.isNotEmpty()) adapter.submitList(news) }
    }

    class EmptyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
