package com.example.finalproject.ui.slideshow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.finalproject.databinding.FragmentSlideshowBinding
import com.example.finalproject.ui.home.HomeViewModel

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val TAG = "slideshowfragment"
    private val model: HomeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textSlideshow
        slideshowViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.list.observe(viewLifecycleOwner) { list ->
            Log.d(TAG, "onViewCreated: $list")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}