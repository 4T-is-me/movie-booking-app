package com.example.testcinemabookingproject.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.testcinemabookingproject.databinding.FragmentProfileBinding
import com.example.testcinemabookingproject.viewmodel.ProfileViewModel

/**
 * PROFILE FRAGMENT: Mảnh ghép luyện tập Profile
 */
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel.userProfile.observe(viewLifecycleOwner) { profile ->
            binding.tvName.text = profile.name
            binding.tvJob.text = profile.job
            binding.tvAvatar.setImageResource(profile.avatarResId)
        }

        binding.addFriendbutton.setOnClickListener {
            Toast.makeText(requireContext(), "add friend request sent", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
