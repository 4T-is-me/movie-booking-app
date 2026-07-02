package com.example.movie_booking_app.view.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.movie_booking_app.data.repository.MovieRepository
import com.example.movie_booking_app.data.repository.RepositoryProvider
import com.example.movie_booking_app.databinding.FragmentGenericListBinding
import com.example.movie_booking_app.view.adapter.MovieNewsAdapter
import com.example.movie_booking_app.view.adapter.PromoAdapter
import com.example.movie_booking_app.view.adapter.ServiceAdapter
import kotlinx.coroutines.launch

class GenericGridFragment : Fragment() {

    private var _binding: FragmentGenericListBinding? = null
    private val binding get() = _binding!!

    private val movieRepository: MovieRepository = RepositoryProvider.provideMovieRepository()
    private var sectionName: String = ""

    companion object {
        fun newInstance(sectionName: String): GenericGridFragment {
            val fragment = GenericGridFragment()
            val args = Bundle()
            args.putString("SECTION_NAME", sectionName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenericListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sectionName = arguments?.getString("SECTION_NAME") ?: ""
        binding.tvTitle.text = sectionName
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        when (sectionName) {
            "Promo" -> {
                binding.rvGenericList.layoutManager = GridLayoutManager(context, 1)
                val adapter = PromoAdapter()
                binding.rvGenericList.adapter = adapter
                viewLifecycleOwner.lifecycleScope.launch { movieRepository.getPromos().collect { adapter.submitList(it) } }
            }
            "Service" -> {
                binding.rvGenericList.layoutManager = GridLayoutManager(context, 3)
                val adapter = ServiceAdapter()
                binding.rvGenericList.adapter = adapter
                viewLifecycleOwner.lifecycleScope.launch { movieRepository.getServices().collect { adapter.submitList(it) } }
            }
            "News" -> {
                binding.rvGenericList.layoutManager = GridLayoutManager(context, 1)
                val adapter = MovieNewsAdapter()
                binding.rvGenericList.adapter = adapter
                viewLifecycleOwner.lifecycleScope.launch { movieRepository.getNews().collect { adapter.submitList(it) } }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
