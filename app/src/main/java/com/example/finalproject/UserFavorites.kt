package com.example.finalproject

import android.util.Log

object UserFavorites {
    private const val TAG = "UserFavorites"
    var favoriteEvents = ArrayList<EventData>()
    var favoriteIds = ArrayList<String>()

    fun isFavorite(event: EventData) = favoriteEvents.contains(event)

    fun addFavorite(event: EventData) {
        if (!isFavorite(event)) {
            favoriteEvents.add(event)
            favoriteIds.add(event.id)
        }


    }

    fun removeFavorite(event: EventData) {
        favoriteEvents.remove(event)
        favoriteIds.remove(event.id)
    }
    fun addFavorite(eventList: List<EventData>) {
        eventList.forEach { event ->
            if (!isFavorite(event)) {
                favoriteEvents.add(event)
            }
        }

    }

    fun addIdAsList(eventIdList: List<String>) {
        val unique = eventIdList.toSet()
        unique.forEach {
            if (!favoriteIds.contains(it)) {
                favoriteIds.add(it)
            }
        }

    }

    fun resetFavorites() {
        favoriteEvents.clear()
        favoriteIds.clear()
    }

    fun printFavorites() {
        Log.d(TAG, "Singleton printFavorites: ${favoriteIds }")
    }

    fun recommendationLogic() :Map<String, Int> {
        val genreCount = mutableMapOf<String, Int>()
        for(event in favoriteEvents) {
            //segment is more vaugue then genre, ex segment = sport, genre = baseball
            //val segmentName = event.classifications[0].segment.name
            val genre = event.classifications[0].genre.name
            genreCount[genre] = genreCount.getOrDefault(genre,0) + 1
        }
        Log.d(TAG, "recomendation logic $genreCount")
        return genreCount
    }

}