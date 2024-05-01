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

}