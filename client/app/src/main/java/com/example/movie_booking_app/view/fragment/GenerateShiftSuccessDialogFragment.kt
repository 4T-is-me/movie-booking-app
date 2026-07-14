package com.example.movie_booking_app.view.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.data.model.response.GenerateShiftResponse
import com.example.movie_booking_app.databinding.FragmentGenerateShiftSuccessDialogBinding

class GenerateShiftSuccessDialogFragment(private val response: GenerateShiftResponse,
                                         private val onDismissAction: () -> Unit) : DialogFragment() {

    private var _binding: FragmentGenerateShiftSuccessDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGenerateShiftSuccessDialogBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Binding data response
        binding.tvGeneratedCount.text = "${response.generated} Shifts Created"
        binding.tvSuccessMessage.text = response.message

        binding.btnDone.setOnClickListener {
            onDismissAction()
            dismiss()
        }

    }
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val displayMetrics = resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.85).toInt() // 85%

            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}