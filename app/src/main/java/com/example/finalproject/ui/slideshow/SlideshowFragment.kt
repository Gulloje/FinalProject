package com.example.finalproject.ui.slideshow

import android.media.metrics.Event
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast

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
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale
import kotlin.collections.ArrayList

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
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slideshowViewModel =
            ViewModelProvider(this).get(SlideshowViewModel::class.java)

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root
        Log.d(TAG, "onCreateView: ${UserFavorites.favoriteEvents[0].name}")
        

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //https://www.youtube.com/watch?v=SD097oVVrPE Reference for search view
        searchView = binding.searchView
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
        if (user.uid == null) {
            binding.textFavorites.text = "Login to Save and View Favorites and Receive Recommendations"
        }
        if (!UserFavorites.favoriteIds.isEmpty()) {
            initRecyclerView()

        }

    }
    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<EventData>()
            for (item in UserFavorites.favoriteEvents) {
                if (item.name.lowercase(Locale.ROOT).contains(query.lowercase())) {
                    filteredList.add(item)
                }
            }
            if (filteredList.isEmpty()) {
                Toast.makeText(requireContext(), "Event Not Found", Toast.LENGTH_SHORT).show()
            } else {
                adapter.setFilter(filteredList)
            }
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




