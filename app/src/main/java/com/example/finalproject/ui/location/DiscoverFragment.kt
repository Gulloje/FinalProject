package com.example.finalproject.ui.location



import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.FavoriteRecyclerAdapter
import com.example.finalproject.TicketData
import com.example.finalproject.databinding.FragmentPopularBinding
import com.example.finalproject.ui.home.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class DiscoverFragment : Fragment() {


    private var _binding: FragmentPopularBinding? = null
    private val binding get() = _binding!!

    private val model: HomeViewModel by activityViewModels()
    private  val viewModel: DiscoverViewModel by activityViewModels()
    private val BASE_URL = "https://app.ticketmaster.com/"
    private val apiKey = "yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG"
    private lateinit var popularAdapter: FavoriteRecyclerAdapter
    private var popularEventData = ArrayList<EventData>()
    private lateinit var popularRecycler: RecyclerView
    private val eventAPI = initRetrofit().create(EventDataService::class.java)
    private val user = FirebaseAuth.getInstance()
    private val TAG = "DiscoverFragment"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPopularBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        loadNearYou()

    }

    //should first load the saved events that users have favorited, assuming within range, then just supply remaining space with the ticketmaster suggestions nearby
    // url for suggestions and for distance: https://app.ticketmaster.com/discovery/v2/suggest?geoPoint=40.720721,-74.0073943&apikey=yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG
    private fun loadNearYou() {
        val db = FirebaseFirestore.getInstance()
        //var geoString: String;
        viewModel.cooridinates.observe(viewLifecycleOwner) { geoString ->
            if (geoString == null) {
                Log.d(TAG, "loadNearYou: no location")
                return@observe
            }
            Log.d(TAG, "loadNearYou: $geoString")
            //now need id of events
            db.document("favoritedEvents/favoriteEventsCounter").get()
                .addOnSuccessListener { document ->

                    var idString = document.data?.keys?.joinToString(separator = ",").toString()
                    Log.d(TAG, "loadNearYou: $idString")
                    //Log.d(TAG, "loadNearYou: ${document.data?.keys}")

                    eventAPI.getEventByGeoPoint(geoString,idString, apiKey).enqueue(object :
                        Callback<TicketData?> {
                        override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                            if (response.body()?._embedded == null) {

                            } else {
                                Log.d(TAG, "onResponse: ${response.body()!!._embedded.events}")
                                popularEventData.addAll(response.body()!!._embedded.events)
                                popularAdapter.notifyDataSetChanged()
                            }

                        }
                        override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                            Log.d(TAG, "onFailure: $t")
                        }
                    })
                }
        }


    }


    private fun initRetrofit() : Retrofit {

        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }

    private fun initRecyclerView() {

        popularRecycler = binding.recyclerPopular

        popularAdapter = FavoriteRecyclerAdapter(requireContext(), popularEventData,true)
        popularRecycler.adapter = popularAdapter
        popularRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)





    }









}