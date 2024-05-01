package com.example.finalproject.ui.slideshow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.FavoriteRecyclerAdapter
import com.example.finalproject.TicketData
import com.example.finalproject.databinding.FragmentSlideshowBinding
import com.example.finalproject.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    private  val userFavorites = ArrayList<EventData>()
    private var arrListFavorites = ArrayList<String>()
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
        if (user.uid == null) {
            binding.textFavorites.text = "Login to Save and View Favorites and Receive Recommendations"
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        model.list.observe(viewLifecycleOwner) {list ->
            if (list.isNotEmpty() && list != arrListFavorites) {
                arrListFavorites = list
                Log.d(TAG, "Updated Favorites: $list")
                loadFavorites()
                adapter.notifyDataSetChanged()
            } else if(list.isEmpty()) {
                binding.textNoFavs.visibility = View.VISIBLE

            }
        }
        Log.d(TAG, "onCreateView2: $arrListFavorites")

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadFavorites() {
        val idString = arrListFavorites.joinToString(separator =",")

        eventAPI.getEventById(idString, apiKey).enqueue(object :
            Callback<TicketData?> {
            override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                if (response.body()?._embedded == null) {

                } else {
                    userFavorites.addAll(response.body()!!._embedded.events)
                    val filtered = userFavorites.filter{it.isEventPassed == false}
                    userFavorites.clear()
                    userFavorites.addAll(filtered)
                }
                adapter.notifyDataSetChanged()
                //Log.d(TAG, "initRecyclerView: $userFavorites")
            }
            override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                Log.d(TAG, "onFailure: $t")
            }
        })
    }

    private fun initRetrofit() : Retrofit {

        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }


    private fun initRecyclerView() {
        recyclerView = binding.favoriteRecycler
        Log.d(TAG, "initRecyclerView: $userFavorites")
        adapter = FavoriteRecyclerAdapter(requireContext(), userFavorites,false)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)

    }
}