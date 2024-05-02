package com.example.finalproject.ui.slideshow

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.FavoriteRecyclerAdapter
import com.example.finalproject.TicketData
import com.example.finalproject.UserFavorites
import com.example.finalproject.databinding.FragmentSlideshowBinding
import com.example.finalproject.eventPassed
import com.example.finalproject.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.ArrayList

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val TAG = "slideshowfragment"
    private val model: HomeViewModel by activityViewModels()


    private val BASE_URL = "https://app.ticketmaster.com/"
    private val apiKey = "yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FavoriteRecyclerAdapter
    private val eventAPI = initRetrofit().create(EventDataService::class.java)
    private val user = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root
        Log.d(TAG, "onCreateView: ${user.uid}")


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (user.uid == null) {
            binding.textFavorites.text = "Login to Save and View Favorites and Receive Recommendations"
        }
        if (!UserFavorites.favoriteIds.isEmpty()) {
            initRecyclerView()

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun initRetrofit() : Retrofit {

        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }



    private fun initRecyclerView() {
        recyclerView = binding.favoriteRecycler
        Log.d(TAG, "initRecyclerView: ${UserFavorites.favoriteEvents}")
        //since using singleton that gets intialized at the start, just have to verify update that
        adapter = FavoriteRecyclerAdapter(requireContext(), UserFavorites.favoriteEvents,false)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)


    }
}