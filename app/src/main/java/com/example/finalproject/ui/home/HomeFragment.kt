package com.example.finalproject.ui.home

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.RecyclerAdapter
import com.example.finalproject.TicketData
import com.example.finalproject.UserFavorites
import com.example.finalproject.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.concurrent.thread
import kotlin.math.log

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val TAG = "HomeFragment"
    //Constants for recycler view

    private val BASE_URL = "https://app.ticketmaster.com/"
    private val apiKey = "yL6rMKTtCDSqaZBhQ1FCUHf4z6mO3htG"
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RecyclerAdapter
    private  val eventList = ArrayList<EventData>()
    private val eventAPI = initRetrofit().create(EventDataService::class.java)

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance()

    private val model: HomeViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //initRecyclerView()

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSearch.setOnClickListener {
            search()
        }
        initRecyclerView()

        Log.d(TAG, "onViewCreated: ${UserFavorites.printFavorites()}")


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var seeMoreCounter = 0;
    // wanted to prevent changing the text field and then hitting seeMore causing the new loaded events to be that of the changed text field
    private var previousCityName =""
    private var previousKeyword = ""
    private fun search() {
        eventList.clear()
        previousCityName = binding.textCity.text.toString()
        previousKeyword = binding.textKeyword.text.toString()

        seeMoreCounter = 0
        loadTickets(previousCityName, previousKeyword)
        view?.hideKeyboard()
    }
    private fun seeMore() {
        seeMoreCounter++
        loadTickets(previousCityName, previousKeyword)

    }

    private fun loadTickets(cityName: String, keyword: String) {


        if (keyword =="") {
            createDialog("Missing Keyword", "Enter a keyword to search for.")
        } else if (cityName =="") {
            createDialog("Missing City Name", "Please enter a city.")
        } else {

            eventAPI.getEventNameByCity(cityName, keyword, seeMoreCounter.toString(), apiKey).enqueue(object :
                Callback<TicketData?> {
                override fun onResponse(call: Call<TicketData?>, response: Response<TicketData?>) {
                    if (response.body()?._embedded == null) {
                        //Toast.makeText(this@MainActivity, "No Events Found", Toast.LENGTH_SHORT).show()
                        binding.noResultsTextView.visibility = View.VISIBLE
                    } else {
                        //Log.d(TAG, "onResponse: ${response.body()}")
                        //Log.d(TAG, "Name ${response.body()!!._embedded.events[0]}")
                        //Log.d(TAG, "Body: ${response.body()}")
                        binding.noResultsTextView.visibility = View.GONE
                        eventList.addAll(response.body()!!._embedded.events)

                    }
                    adapter.notifyDataSetChanged()

                }

                override fun onFailure(call: Call<TicketData?>, t: Throwable) {
                    Log.d(TAG, "onFailure: $t")
                }
            })
        }

    }
    private fun createDialog(title: String, message: String ) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }
    //when first load recycler view (done on startup regardless), have to determine what events are marked as favorite
    //then can just save this to the UserFavorites Singleton so dont have to use api constantly
    private fun initRecyclerView() {
        recyclerView = binding.recycleView
        //i needed to send a function to set the seeMore button to when the recylerview in initialized
        var favorites: ArrayList<String>

        //COMEBACK: trying to factor stuff out made this crash when user not logged in idk whats going on here
        db.document("users/${user.uid}").get()
            .addOnSuccessListener {document ->
                if (document.data?.get("favorites") != null) {
                    favorites = document.data?.get("favorites") as ArrayList<String>
                    UserFavorites.addIdAsList(favorites)
                    theWTFFunction()


                }
            }
            .addOnFailureListener {
                theWTFFunction()

            }
        theWTFFunction()
        Log.d(TAG, "onViewCreated: ${UserFavorites.printFavorites()}")


    }
    private fun initRetrofit() : Retrofit {
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit
    }

    private fun theWTFFunction() {
        adapter = RecyclerAdapter(requireContext(), eventList, UserFavorites.favoriteIds) {
            seeMore()
        };
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }


}