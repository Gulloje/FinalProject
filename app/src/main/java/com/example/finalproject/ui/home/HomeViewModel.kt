package com.example.finalproject.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.finalproject.EventData

class HomeViewModel : ViewModel() {

    /*private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text*/

    private val _list = MutableLiveData<ArrayList<String>>()
    val list: LiveData<ArrayList<String>> = _list

    fun setList(newList: ArrayList<String>) {
        _list.value = newList
    }
}