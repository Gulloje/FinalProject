package com.example.finalproject.ui.location



import android.media.metrics.Event
import android.os.Build
import android.os.Bundle
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
import com.example.finalproject.TicketData
import com.example.finalproject.UserFavorites
import com.example.finalproject.databinding.FragmentPopularBinding
import com.example.finalproject.eventPassed
import com.example.finalproject.ui.home.HomeViewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.firebase.firestore.FirebaseFirestore
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

    private val model: HomeViewModel by activityViewModels()
    private  val viewModel: DiscoverViewModel by activityViewModels()
    private val BASE_URL = "https://app.ticketmaster.com/"
    private val apiKey = "yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG"
    private lateinit var popularAdapter: FavoriteRecyclerAdapter
    private lateinit var recommendAdapter: FavoriteRecyclerAdapter
    private var recommendEventData = ArrayList<EventData>()
    private var popularEventData = ArrayList<EventData>()
    private lateinit var popularRecycler: RecyclerView
    private lateinit var recommendRecycler: RecyclerView
    private val eventAPI = initRetrofit().create(EventDataService::class.java)

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
        UserFavorites.printFavorites()
        createPrompt()

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
                    //comeback to only get ids with a certain number of favorites
                    var idString = document.data?.keys?.joinToString(separator = ",").toString()
                    Log.d(TAG, "loadNearYou: $idString")
                    //Log.d(TAG, "loadNearYou: ${document.data?.keys}")

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
                                    //suggestions endpoint only lets you get up to 5 more events
                                    fillInMoreSuggestions(geoString)
                                }
                            }

                        }
                        override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                            Log.d(TAG, "onFailure: $t")
                        }
                    })


                }
        }


    }

    private fun createPrompt() {
        val geminiapikey = "AIzaSyCPFOue41NY2_HJQ5-LeUaaj02hM4QlPTM"
        val harassmentSafety = SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH)
        val hateSpeechSafety = SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE)
        val generativeModel = GenerativeModel(
            // Use a model that's applicable for your use case (see "Implement basic use cases" below)
            modelName = "gemini-pro",
            // Access your API key as a Build Configuration variable (see "Set up your API key" above)
            apiKey = geminiapikey,
            safetySettings = listOf(harassmentSafety, hateSpeechSafety)
        )

        CoroutineScope(Dispatchers.Main + CoroutineName("airecc")).launch {
            var prompt = "Given the following user genre favorites: ${UserFavorites.recommendationLogic()}, " +
                    "provide a list of recommended classifications of events to search for in the exact form class1,class2,class3...class8 with the location ${viewModel.cooridinates.value} in mind"
            //prompt = "Generate a list of keywords similar to   in a 100 mile radius around location ${viewModel.cooridinates.value} that might interest a user who enjoys live music in the exact form a,b,c where a,b,c are the name of the artist."
            val response = generativeModel.generateContent(prompt)
            Log.d(TAG, "createPrompt: $prompt")
            Log.d(TAG, "createPrompt: ${response.text}")
            response.text?.let {
                eventAPI.getRecommended(it, viewModel.cooridinates.value, apiKey).enqueue(object :
                    Callback<TicketData?> {
                    override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                        if (response.body()?._embedded == null) {

                        } else {
                            Log.d(TAG, "onResponse: ${response.body()}")
                            recommendEventData.addAll(response.body()!!._embedded.events)
                        }
                        recommendAdapter.notifyDataSetChanged()

                    }

                    override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                        Log.d(TAG, "onFailure: $t")
                    }
                })
            }
        }
    }

    private fun fillInMoreSuggestions(geoString: String) {

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



    }









}