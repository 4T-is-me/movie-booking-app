package com.example.movie_booking_app.view.fragment.staff

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.movie_booking_app.databinding.DialogLeaveStatusFilterBinding

class LeaveStatusFilterDialogFragment(private val onStatusSelected: (String?) -> Unit) : DialogFragment() {
    private var _binding: DialogLeaveStatusFilterBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = DialogLeaveStatusFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val params = attributes
            params.width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            params.height = WindowManager.LayoutParams.WRAP_CONTENT
            attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFilterAll.setOnClickListener {
            onStatusSelected(null) // null represents displaying all status
            dismiss()
        }
        binding.btnFilterPending.setOnClickListener {
            onStatusSelected("Pending")
            dismiss()
        }

        binding.btnFilterApproved.setOnClickListener {
            onStatusSelected("Approved")
            dismiss()
        }

        binding.btnFilterRejected.setOnClickListener {
            onStatusSelected("Rejected")
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}