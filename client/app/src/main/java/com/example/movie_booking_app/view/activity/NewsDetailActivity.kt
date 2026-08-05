package com.example.movie_booking_app.view.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide
import com.example.movie_booking_app.R
import com.example.movie_booking_app.data.model.response.CommentItemResponse
import com.example.movie_booking_app.databinding.ActivityNewsDetailBinding
import com.example.movie_booking_app.utils.Constants
import com.example.movie_booking_app.view.adapter.CommentsAdapter
import com.example.movie_booking_app.viewmodel.NewsDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewsDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewsDetailBinding
    private val viewModel: NewsDetailViewModel by viewModels()
    private lateinit var adapter: CommentsAdapter

    private var newsId: String = ""
    private var selectedParentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Parse parameters from intent
        newsId = intent.getStringExtra("newsId") ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val imageUrl = intent.getStringExtra("imageUrl") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val content = intent.getStringExtra("content") ?: ""
        val taggedMovieId = intent.getStringExtra("movieId")
        val taggedMovieName = intent.getStringExtra("movieName")
        val taggedMoviePoster = intent.getStringExtra("moviePoster")

        if (newsId.isEmpty()) {
            Toast.makeText(this, getString(R.string.auto_str_news_article_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI(title, imageUrl, date, content, taggedMovieId, taggedMovieName, taggedMoviePoster)
        observeViewModel()
        viewModel.loadComments(newsId)
    }

    private fun setupUI(
        title: String,
        imageUrl: String,
        date: String,
        content: String,
        taggedMovieId: String?,
        taggedMovieName: String?,
        taggedMoviePoster: String?
    ) {
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Bind main news content
        binding.tvNewsTitle.text = title
        binding.tvNewsDate.text = getString(R.string.news_published_on, date)
        binding.tvNewsContent.text = content

        Glide.with(this)
            .load(imageUrl.ifEmpty { null })
            .placeholder(R.drawable.movie)
            .error(R.drawable.movie)
            .into(binding.ivNewsBanner)

        // Bind Movie Tag Card if present
        if (!taggedMovieId.isNullOrEmpty()) {
            binding.cardMovieTag.visibility = View.VISIBLE
            binding.tvMovieName.text = taggedMovieName ?: getString(R.string.news_movie_details)

            val fullPosterUrl = when {
                taggedMoviePoster.isNullOrEmpty() -> ""
                taggedMoviePoster.startsWith("http") -> taggedMoviePoster
                else -> "${Constants.BASE_URL.removeSuffix("/")}$taggedMoviePoster"
            }

            Glide.with(this)
                .load(fullPosterUrl.ifEmpty { null })
                .placeholder(R.drawable.movie)
                .error(R.drawable.movie)
                .into(binding.ivMoviePoster)

            // Open Movie Detail screen on click
            binding.btnBookNow.setOnClickListener {
                val intent = Intent(this, MovieDetailActivity::class.java).apply {
                    putExtra("movieId", taggedMovieId)
                }
                startActivity(intent)
            }
        } else {
            binding.cardMovieTag.visibility = View.GONE
        }

        // Setup Comments RecyclerView
        adapter = CommentsAdapter(
            onReplyClick = { comment ->
                // Enable replying to user state
                selectedParentId = comment.id
                binding.layoutReplyIndicator.visibility = View.VISIBLE
                binding.tvReplyUser.text = getString(R.string.news_replying_to, comment.commenterName)
                binding.etComment.requestFocus()
            },
            onLikeClick = { comment, type ->
                viewModel.reactToComment(newsId, comment.id, type)
            }
        )

        binding.rvComments.layoutManager = LinearLayoutManager(this)
        binding.rvComments.adapter = adapter
        (binding.rvComments.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false

        // Setup Send Comment trigger
        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.postComment(newsId, text, selectedParentId)
                binding.etComment.text.clear()
                resetReplyBanner()
            }
        }

        // Cancel Reply Mode trigger
        binding.btnCancelReply.setOnClickListener {
            resetReplyBanner()
        }
    }

    private fun resetReplyBanner() {
        selectedParentId = null
        binding.layoutReplyIndicator.visibility = View.GONE
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.comments.collect { list ->
                    if (list.isEmpty()) {
                        binding.rvComments.visibility = View.GONE
                        binding.tvEmptyComments.visibility = View.VISIBLE
                    } else {
                        binding.rvComments.visibility = View.VISIBLE
                        binding.tvEmptyComments.visibility = View.GONE
                        
                        // Flatten tree for list displaying
                        val flatList = flattenComments(list)
                        adapter.submitList(flatList)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.error.collect { errorMsg ->
                    errorMsg?.let {
                        Toast.makeText(this@NewsDetailActivity, it, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun flattenComments(comments: List<CommentItemResponse>): List<Pair<CommentItemResponse, Int>> {
        val flatList = mutableListOf<Pair<CommentItemResponse, Int>>()
        fun traverse(comment: CommentItemResponse, level: Int) {
            flatList.add(Pair(comment, level))
            comment.replies?.forEach { reply ->
                traverse(reply, level + 1)
            }
        }
        comments.forEach { comment ->
            traverse(comment, 0)
        }
        return flatList
    }
}
