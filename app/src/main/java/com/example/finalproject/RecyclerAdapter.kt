package com.example.finalproject

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.opengl.Visibility
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.finalproject.EventData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.floor

class RecyclerAdapter(private val context: Context, private val eventList: ArrayList<EventData>, private val userFavorites: ArrayList<String>,
                      private val onSeeMoreClicked: () -> Unit ): RecyclerView.Adapter<RecyclerAdapter.ViewHolder>()
{
    //code for adding the see more button: https://stackoverflow.com/questions/29106484/how-to-add-a-button-at-the-end-of-recyclerview
    val TAG = "Recycler Adapter"
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().uid
    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val eventName = itemView.findViewById<TextView>(R.id.textEventName)
        val eventLocation = itemView.findViewById<TextView>(R.id.textLocation)
        val address = itemView.findViewById<TextView>(R.id.textAddress)
        val date = itemView.findViewById<TextView>(R.id.textDate)
        val priceRange = itemView.findViewById<TextView>(R.id.textPriceRange)
        val image = itemView.findViewById<ImageView>(R.id.imageView)
        val btnSeeMore = itemView.findViewById<Button>(R.id.btnSeeMore)
        val btnSeeTickets = itemView.findViewById<Button>(R.id.btnSeeTickets)
        val checkFavorite = itemView.findViewById<CheckBox>(R.id.checkFavorite)


        init {
            btnSeeTickets?.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW)
                browserIntent.data = Uri.parse(eventList[position].url)
                context.startActivity( browserIntent)
            }

            btnSeeMore?.setOnClickListener {
                onSeeMoreClicked()
            }
            checkFavorite?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (user == null) {
                    Toast.makeText(context, "Must Login to Favorite Events", Toast.LENGTH_SHORT).show()
                    checkFavorite.isChecked = false
                    //holder.checkFavorite.visibility = View.GONE //if i think it is better to just hide the button altogether
                    return@setOnCheckedChangeListener
                }
                val currentEventId = eventList[position].id

                //update favorite status based on checkbox
                if (isChecked) {
                    if (!userFavorites.contains(currentEventId)) {
                        addFavorite(eventList[position])
                        userFavorites.add(currentEventId)
                    }
                } else {
                    if (userFavorites.contains(currentEventId)) {
                        deleteFavorite(eventList[position])
                        userFavorites.remove(currentEventId)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = when (viewType) {
            //if the item is a row, give the row_item xml, else its the button
            R.layout.row_item -> LayoutInflater.from(parent.context).inflate(R.layout.row_item, parent, false)
            else -> LayoutInflater.from(parent.context).inflate(R.layout.see_more_btn, parent, false)
        }
        return ViewHolder(itemView)
    }



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder: $position")

        if (position == eventList.size) {

            if (eventList.size % 20 == 0 && eventList.size != 0) {
                holder.btnSeeMore.visibility = View.VISIBLE
            } else {
                holder.btnSeeMore.visibility = View.GONE
            }
        } else if (eventList.size > 0){
            val curItem = eventList[position]
            holder.eventName.text = "${curItem.name}"

            //date  https://www.datetimeformatter.com/how-to-format-date-time-in-kotlin/
            var stringDate = curItem.dates.start.localDate
            var date = SimpleDateFormat("yyyy-MM-dd").parse(stringDate)
            stringDate = SimpleDateFormat("MM/dd/yyyy").format(date)

            //time
            try {
                var time24 = curItem.dates.start.localTime
                var time12 = SimpleDateFormat("H:mm:ss").parse(time24)
                var realTime = SimpleDateFormat("h:mm a").format(time12)
                holder.date.text = "$stringDate at $realTime"
            } catch (e: Exception) { //i had a date be null at one point
                holder.date.text = "Date: N/A"
            }


            //address
            holder.eventLocation.text = "${curItem._embedded.venues[0].name}"
            holder.address.text = "${curItem._embedded.venues[0].address.line1}, ${curItem._embedded.venues[0].city.name}, ${curItem._embedded.venues[0].state.stateCode}"

            //wanted to get rid of .0 and .5 and handle null
            try {
                holder.priceRange.text = "Price Range: $${ceil(curItem.priceRanges[0].min).toInt()} - $${ceil(curItem.priceRanges[0].max).toInt()}"
            } catch(e: Exception) {
                holder.priceRange.text = "PriceRange: N/A"
            }

            val highestQualityImage = curItem.images.maxByOrNull {
                it.width.toInt() * it.height.toInt()

            }
            val context = holder.itemView.context
            Glide.with(context).load(highestQualityImage?.url).into(holder.image)

            //Log.d(TAG, "favorites: $userFavorites")
            //Log.d(TAG, "itemid: ${curItem.id}")

            if(userFavorites.contains(curItem.id)) {
                holder.checkFavorite.isChecked = true
            } else {
                holder.checkFavorite.isChecked = false
            }
        }




    }

    override fun getItemViewType(position: Int): Int {
        return if (position == eventList.size) {
            R.layout.see_more_btn
        } else {
            R.layout.row_item
        }
    }


    override fun getItemCount(): Int {
        return eventList.size + 1
    }

    private fun addFavorite(event: EventData) {
        //add it to the users favorites and increment to the favorited events
        val usersFavorites = db.document("users/${user}")
        usersFavorites.update("favorites", FieldValue.arrayUnion(event.id)) //https://firebase.google.com/docs/firestore/manage-data/add-data
        val eventToAdd = mutableMapOf<String, Any>()
        eventToAdd[event.id] = FieldValue.increment(1);
        val eventRef = db.collection("favoritedEvents").document("favoriteEventsCounter")
        eventRef.update(eventToAdd)
        UserFavorites.addFavorite(event)



    }

    private fun deleteFavorite(event: EventData) {
        val usersFavorites = db.document("users/${user}/")
        usersFavorites.update("favorites", FieldValue.arrayRemove(event.id))
        val eventToAdd = mutableMapOf<String, Any>()
        eventToAdd[event.id] = FieldValue.increment(-1);
        val eventRef = db.collection("favoritedEvents").document("favoriteEventsCounter")
        eventRef.update(eventToAdd)
        UserFavorites.removeFavorite(event)

    }
}