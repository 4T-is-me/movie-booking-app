package com.example.movie_booking_app.view.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.movie_booking_app.R
import com.example.movie_booking_app.databinding.ActivityChatBinding
import com.example.movie_booking_app.view.adapter.ChatAdapter
import com.example.movie_booking_app.viewmodel.ChatViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.Locale
import java.util.UUID

@AndroidEntryPoint
class ChatActivity : BaseActivity() {

    private lateinit var binding: ActivityChatBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter
    private var sessionId: String = ""



    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val pickGalleryImage = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            uploadAndAnalyzeUri(uri)
        } else {
            Toast.makeText(this, getString(R.string.auto_str_kh_ng_c_nh_n_o_c_ch_n), Toast.LENGTH_SHORT).show()
        }
    }

    private val requestAudioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) toggleVoiceRecognition()
        else Toast.makeText(this, getString(R.string.auto_str_c_n_c_p_quy_n_micro_thu_m_gi_n), Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        setupSpeechRecognizer()
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            hideKeyboard()
            finish()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { selectedCinemaName ->
            viewModel.sendMessage(selectedCinemaName, sessionId)
        }
        binding.rvChatMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
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

        binding.btnCamera.setOnClickListener {
            pickGalleryImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnVoice.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                toggleVoiceRecognition()
            } else {
                requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }



    // --- Voice: Trực tiếp ngầm trong App (không mở dialog popup Google) ---
    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    setListeningUI(true)
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    setListeningUI(false)
                    isListening = false
                }

                override fun onError(error: Int) {
                    setListeningUI(false)
                    isListening = false
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Chưa nghe rõ, bạn thử nói lại nhé."
                        SpeechRecognizer.ERROR_AUDIO -> "Lỗi mic thu âm."
                        SpeechRecognizer.ERROR_NETWORK -> "Lỗi kết nối mạng."
                        else -> null
                    }
                    if (errorMsg != null) {
                        Toast.makeText(this@ChatActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResults(results: Bundle?) {
                    setListeningUI(false)
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        binding.etChatMessage.setText(text)
                        binding.etChatMessage.setSelection(text.length)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun toggleVoiceRecognition() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer()
        }

        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            setListeningUI(false)
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "vi-VN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, getString(R.string.auto_str_kh_ng_th_k_ch_ho_t_micro), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setListeningUI(listening: Boolean) {
        if (listening) {
            binding.btnVoice.setColorFilter(Color.parseColor("#E5484D")) // Đỏ nhấp nháy/báo hiệu đang nghe
            binding.etChatMessage.hint = "🔴 Đang nghe bạn nói..."
        } else {
            binding.btnVoice.clearColorFilter()
            binding.etChatMessage.hint = "Nhập câu hỏi (giá vé, lịch chiếu...)..."
        }
    }

    private fun uploadAndAnalyzeUri(uri: Uri) {
        try {
            val tempFile = File(cacheDir, "vision_upload.jpg")
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                Toast.makeText(this, getString(R.string.auto_str_nh_kh_ng_h_p_l_vui_l_ng_th_l_i), Toast.LENGTH_SHORT).show()
                return
            }

            val reqFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", tempFile.name, reqFile)

            Toast.makeText(this, getString(R.string.auto_str_gemini_ai_ang_ph_n_t_ch_h_nh_n), Toast.LENGTH_SHORT).show()
            viewModel.analyzeImage(body) { recognizedMovie ->
                runOnUiThread {
                    if (!recognizedMovie.isNullOrBlank()) {
                        Toast.makeText(this, "🎬 Đã tìm thấy phim: $recognizedMovie", Toast.LENGTH_LONG).show()
                        viewModel.sendMessage(recognizedMovie, sessionId, isFromVision = true)
                    } else {
                        Toast.makeText(this, getString(R.string.auto_str_ai_kh_ng_nh_n_ra_t_n_phim_tron), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Lỗi khi xử lý ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
