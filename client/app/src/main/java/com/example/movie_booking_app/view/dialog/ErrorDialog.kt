package com.example.movie_booking_app.view.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import com.example.movie_booking_app.databinding.DialogErrorBinding

class ErrorDialog(
    context: Context,
    private val title: String? = null,
    private val message: String,
    private val onOkClicked: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var binding: DialogErrorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogErrorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make background transparent for rounded corners
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        title?.let {
            binding.tvErrorTitle.text = it
        }
        binding.tvErrorMessage.text = message

        binding.btnOk.setOnClickListener {
            dismiss()
            onOkClicked?.invoke()
        }
    }
}
