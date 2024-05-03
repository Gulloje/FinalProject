package com.example.finalproject

import android.media.metrics.Event
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreRepo {
    private val db = FirebaseFirestore.getInstance()
    private var user = FirebaseAuth.getInstance().currentUser
    private const val TAG = "FirestoreRepo"
    fun addUser(user: FirebaseUser) {
        val userData = mapOf<String, Any>() //since i dont really care about also writing the uid as a field under the document with uid, just need an object to send to .set
        db.collection("users").document(user.uid).set(userData)
            .addOnFailureListener {
                Log.d(TAG, "Error adding user.")
            }
    }
    
    fun getUser() : FirebaseUser?{
        return user
    }
    
    

    //retrieves whatever id's the user has favorited that is in firebase
    fun setFavoriteIds(onSuccess: (ArrayList<String>) -> Unit, onFailure: () -> Unit)  {
        var favorites: ArrayList<String>
        db.document("users/${user?.uid}").get()
            .addOnSuccessListener { document ->
                if(document.data?.get("favorites") != null) {
                    favorites = document.data?.get("favorites") as ArrayList<String>
                    UserFavorites.addIdAsList(favorites)
                    onSuccess(favorites)
                }
            }
            .addOnFailureListener{
                onFailure()
                Log.d(TAG, "setFavoriteIds: Could not retrieve favorites")
            }
    }
    fun clearUser() {
        user = null
    }
    fun setUser(user: FirebaseUser?) {
        this.user = user

    }

    fun addFavorite(event: EventData) {
        //add it to the users favorites and increment to the favorited events
        val usersFavorites = db.document("users/${user?.uid}")
        usersFavorites.update("favorites", FieldValue.arrayUnion(event.id)) //https://firebase.google.com/docs/firestore/manage-data/add-data
        val eventToAdd = mutableMapOf<String, Any>()
        eventToAdd[event.id] = FieldValue.increment(1);
        val eventRef = db.collection("favoritedEvents").document("favoriteEventsCounter")
        eventRef.update(eventToAdd)
        UserFavorites.addFavorite(event)
    }

    fun deleteFavorite(event: EventData) {
        val usersFavorites = db.document("users/${user?.uid}/")
        usersFavorites.update("favorites", FieldValue.arrayRemove(event.id))
        val eventToAdd = mutableMapOf<String, Any>()
        eventToAdd[event.id] = FieldValue.increment(-1);
        val eventRef = db.collection("favoritedEvents").document("favoriteEventsCounter")
        eventRef.update(eventToAdd)
        UserFavorites.removeFavorite(event)
    }
    
    fun getAllFavoritedCount(onSuccess: (String) -> Unit, onFailure: () -> Unit) {
        db.document("favoritedEvents/favoriteEventsCounter").get()
            .addOnSuccessListener { document ->
                var idString = document.data?.keys?.joinToString(separator = ",").toString()
                onSuccess(idString)
            }
            .addOnFailureListener {
                Log.d(TAG, "getAllFavoritedCount: couldnt retrieve all favorites")
            }
    }
}