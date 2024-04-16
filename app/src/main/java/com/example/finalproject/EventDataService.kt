package com.example.finalproject

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface EventDataService {

    @GET("discovery/v2/events.json?sort=date,asc&size=20")
    fun getEventNameByCity(@Query("city") city: String,
                           @Query("keyword") keyword: String,
                           @Query("page") page: String,
                           @Query("apikey") apiKey: String): Call<TicketData>
}