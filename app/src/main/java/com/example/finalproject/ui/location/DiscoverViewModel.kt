package com.example.finalproject.ui.location

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DiscoverViewModel : ViewModel() {
    private val _coordinates = MutableLiveData<String>()
    val cooridinates: LiveData<String> = _coordinates


    fun setUserCoords(coords: String) {
        _coordinates.value = coords
    }

}