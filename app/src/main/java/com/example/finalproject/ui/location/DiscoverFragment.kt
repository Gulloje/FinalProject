package com.example.finalproject.ui.location


import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.FavoriteRecyclerAdapter
import com.example.finalproject.FirestoreRepo
import com.example.finalproject.TicketData
import com.example.finalproject.UserFavorites
import com.example.finalproject.databinding.FragmentPopularBinding
import com.example.finalproject.eventPassed
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.api.Distribution.BucketOptions.Linear
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class DiscoverFragment : Fragment() {


    private var _binding: FragmentPopularBinding? = null
    private val binding get() = _binding!!

    private  val viewModel: DiscoverViewModel by activityViewModels()
    private val BASE_URL = "https://app.ticketmaster.com/"
    private val apiKey = "yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG"
    private lateinit var popularAdapter: FavoriteRecyclerAdapter
    private lateinit var recommendAdapter: FavoriteRecyclerAdapter
    private var recommendEventData = ArrayList<EventData>()
    private var popularEventData = ArrayList<EventData>()
    private lateinit var popularRecycler: RecyclerView
    private lateinit var recommendRecycler: RecyclerView
    private lateinit var scrollListener: RecyclerView.OnScrollListener
    private val eventAPI = initRetrofit().create(EventDataService::class.java)

    private lateinit var lastVisibileItem: LinearLayoutManager

    private val TAG = "DiscoverFragment"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPopularBinding.inflate(inflater, container, false)
        val root: View = binding.root
        return root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        lastVisibileItem = LinearLayoutManager(requireContext())
        UserFavorites.printFavorites()
        getPopularNearYou()
        getRecommended()

    }

    //should first load the saved events that users have favorited, assuming within range, then just supply remaining space with the ticketmaster suggestions nearby
    // url for suggestions and for distance: https://app.ticketmaster.com/discovery/v2/suggest?geoPoint=40.720721,-74.0073943&apikey=yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG
    //the ai stuff was working at some point and then just stopped
    private fun getPopularNearYou() {
        val geoString = viewModel.cooridinates.value
        var idString =""
        if (geoString =="") {
            binding.textNoLocation.visibility = View.VISIBLE
            //binding.recyclerPopular.visibility = View.GONE
            return
        }
        FirestoreRepo.getAllFavoritedCount(
            onSuccess = {
                idString = it
                eventAPI.getEventByGeoPoint(geoString,idString, apiKey).enqueue(object :
                    Callback<TicketData?> {
                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                            if (response.body()?._embedded == null) {

                            } else {
                                //Log.d(TAG, "onResponse: ${response.body()!!._embedded.events}")
                                popularEventData.addAll(response.body()!!._embedded.events)
                                val filtered = popularEventData.filter{ it.distance <= 70 && !eventPassed(it)}
                                popularEventData.clear()
                                popularEventData.addAll(filtered)
                                popularAdapter.notifyDataSetChanged()
                                if(popularEventData.size < 20) {
                                    //suggestions endpoint only lets you get up to 5 more events, but this way never empty
                                    Handler().postDelayed({ //added delay so api call fails less often
                                        fillInMoreSuggestions(geoString)
                                    }, 1500)
                                    //fillInMoreSuggestions(geoString)
                                }
                            }

                        }
                        override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                            Log.d(TAG, "onFailure: $t")
                        }
                    })
                },
                onFailure = {

                }
        )

    }
    private var recommendPage = 0
    private fun getRecommended() {
        Log.d(TAG, "getRecommended: ${viewModel.cooridinates.value}")
        val usersRecommended = UserFavorites.recommendationLogic().keys.joinToString(",")
        //Log.d(TAG, "createPrompt: $usersRecommended")
        eventAPI.getRecommended(usersRecommended, viewModel.cooridinates.value, recommendPage.toString(), apiKey).enqueue(object :
            Callback<TicketData?> {
            override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                if (response.body()?._embedded == null) {

                } else {
                    //Log.d(TAG, "onResponse: ${response.body()}")
                    recommendEventData.addAll(response.body()!!._embedded.events)
                    Log.d(TAG, "bruh am i getting data: ${response.body()!!._embedded.events[0].name}")
                }
                recommendAdapter.notifyDataSetChanged()

            }

            override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                Log.d(TAG, "onFailure: $t")
            }
        })

    }

    private fun fillInMoreSuggestions(geoString: String?) {

        Log.d(TAG, "fillInMoreSuggestions: $geoString")
        eventAPI.getSuggestedByDistance(geoString, apiKey).enqueue(object :
            Callback<TicketData?> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                if (response.body()?._embedded == null) {

                } else {
                    val newEvents = response.body()!!._embedded.events
                    //want to prevent duplicated from appearing
                    val filtered = newEvents.filter { newEvent ->
                        popularEventData.none { existingEvent -> existingEvent.id == newEvent.id }
                    }
                    popularEventData.addAll(filtered)
                    popularAdapter.notifyDataSetChanged()
                }

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

        popularRecycler = binding.recyclerPopular
        popularAdapter = FavoriteRecyclerAdapter(requireContext(), popularEventData,true)
        popularRecycler.adapter = popularAdapter
        popularRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        recommendRecycler = binding.recycleRecomend
        recommendAdapter = FavoriteRecyclerAdapter(requireContext(), recommendEventData, true)
        recommendRecycler.adapter = recommendAdapter
        recommendRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        setEndOfRecyclerListener()



    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setEndOfRecyclerListener() {
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val totalItemCount = recommendRecycler.layoutManager?.itemCount
                Log.d(TAG, "TOTALITEMCOUNT $totalItemCount and SIZE ${recommendEventData.size}")
                //if (totalItemCount!= null && totalItemCount % 20 == recommendEventData.size) {
                if (totalItemCount!= null && totalItemCount %20 == 0) {
                    recommendPage++
                    getRecommended()
                    //recyclerView.removeOnScrollListener(scrollListener)
                }
            }

        }
        recommendRecycler.addOnScrollListener(scrollListener)

    }

    










}