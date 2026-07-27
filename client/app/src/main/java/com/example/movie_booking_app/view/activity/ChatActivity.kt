package com.example.movie_booking_app.view.activity

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.databinding.ActivityChatBinding
import com.example.movie_booking_app.view.adapter.ChatAdapter
import com.example.movie_booking_app.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var sessionId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Generate or retrieve persistent Session ID for chatbot context
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", null)
        sessionId = userId ?: sharedPref.getString("chat_session_id", null) ?: run {
            val newId = "session_" + UUID.randomUUID().toString().take(8)
            sharedPref.edit().putString("chat_session_id", newId).apply()
            newId
        }

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        setupListeners()
    }

    /**
     * Set up Top Toolbar with Back Button.
     * Calling finish() returns the user directly to the screen they were previously on.
     */
    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            hideKeyboard()
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvChatMessages.layoutManager = layoutManager
        binding.rvChatMessages.adapter = chatAdapter
    }

    private fun observeViewModel() {
        viewModel.messages.observe(this) { messageList ->
            chatAdapter.submitList(messageList) {
                if (messageList.isNotEmpty()) {
                    binding.rvChatMessages.post {
                        binding.rvChatMessages.smoothScrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etChatMessage.text.toString()
            if (text.trim().isNotEmpty()) {
                viewModel.sendMessage(text, sessionId)
                binding.etChatMessage.setText("")
            }
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
