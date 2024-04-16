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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproject.EventData
import com.example.finalproject.EventDataService
import com.example.finalproject.RecyclerAdapter
import com.example.finalproject.TicketData
import com.example.finalproject.databinding.FragmentHomeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        initRecyclerView()

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnSearch.setOnClickListener {
            search()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private var seeMoreCounter = 0;
    // wanted to prevent changing the text field and then hitting seeMore causing the new loaded events to be that of the changed text field
    private var previousCityName =""
    private var previousKeyword = ""
    fun search() {
        eventList.clear()
        previousCityName = binding.textCity.text.toString()
        previousKeyword = binding.textKeyword.text.toString()

        seeMoreCounter = 0
        loadTickets(previousCityName, previousKeyword)
        view?.hideKeyboard()
    }
    fun seeMore() {
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
                        Log.d(TAG, "onResponse: ${response.body()}")
                        Log.d(TAG, "Name ${response.body()!!._embedded.events[0]}")
                        Log.d(TAG, "Body: ${response.body()}")
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
    private fun initRecyclerView() {
        recyclerView = binding.recycleView
        //i needed to send a function to set the seeMore button to when the recylerview in initialized
        adapter = RecyclerAdapter(requireContext(), eventList) {
            seeMore()
        };
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

    }
    private fun initRetrofit() : Retrofit {
        val retrofit = Retrofit.Builder().baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit
    }

    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        //want to hide the keyboard when they go back to it
        //view = findViewById(android.R.id.content).getRootView().getWindowToken();  https://rmirabelle.medium.com/close-hide-the-soft-keyboard-in-android-db1da22b09d2
        //findViewById<View>(android.R.id.content).hideKeyboard()
        binding.root.hideKeyboard()


    }
}