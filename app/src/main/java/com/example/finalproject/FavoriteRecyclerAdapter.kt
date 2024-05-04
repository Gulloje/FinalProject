
package com.example.finalproject

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.auth.data.model.User
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class FavoriteRecyclerAdapter(private val context: Context, private var eventData: ArrayList<EventData>,
                              private val showDistance: Boolean = false): RecyclerView.Adapter<FavoriteRecyclerAdapter.FavoriteHolder>()
{
    private val user = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FavoriteRecyclerAdapter"

    inner class FavoriteHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val eventName = itemView.findViewById<TextView>(R.id.textEventName)
        val timeLeft = itemView.findViewById<TextView>(R.id.textDaysLeft)
        val eventLocation = itemView.findViewById<TextView>(R.id.textLocation)
        val image = itemView.findViewById<ImageView>(R.id.imageView)
        val btnSeeTickets = itemView.findViewById<Button>(R.id.btnSeeTickets)
        val checkFavorite = itemView.findViewById<CheckBox>(R.id.checkFavorite)
        val textDate = itemView.findViewById<TextView>(R.id.textDate)
        val btnMoreInfo = itemView.findViewById<Button>(R.id.btnMoreInfo)


        init {

            btnSeeTickets?.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW)
                browserIntent.data = Uri.parse(eventData[position].url)
                context.startActivity( browserIntent)
            }

            btnMoreInfo?.setOnClickListener {
                moreInfoDialog(eventData[adapterPosition])
            }

            checkFavorite?.setOnClickListener {
                //if the view is on the favorites tab, verify they want to remove and remove from the view
                if (!showDistance) {
                    btnMoreInfo.visibility = View.VISIBLE
                    if (!checkFavorite.isChecked) {
                        createDialog(adapterPosition, checkFavorite)


                    }
                } else { //if other tab, treat like search functionality

                    if (FirestoreRepo.getUser() == null) {
                        Toast.makeText(context, "Must Login to Favorite Events", Toast.LENGTH_SHORT).show()
                        checkFavorite.isChecked = false
                        //holder.checkFavorite.visibility = View.GONE //if i think it is better to just hide the button altogether
                        return@setOnClickListener
                    }
                    val currentEventId = eventData[adapterPosition].id

                    if (checkFavorite.isChecked) {
                        if (!UserFavorites.favoriteIds.contains(currentEventId)) {
                            FirestoreRepo.addFavorite(eventData[adapterPosition])
                        }
                    } else {
                        if (UserFavorites.favoriteIds.contains(currentEventId)) {
                            val eventToRemove = eventData[adapterPosition]

                            //FirestoreRepo.deleteFavorite(eventToRemove)
                            FirestoreRepo.deleteFavorite( eventData[adapterPosition])


                        }
                    }
                }
            }

        }
    }

    fun setFilter(eventData: ArrayList<EventData>) {
        this.eventData = eventData
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteRecyclerAdapter.FavoriteHolder {

        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.favorite_item, parent, false)
        return FavoriteHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        val curItem = eventData[position]
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        if (showDistance) {
            holder.btnMoreInfo.visibility = View.GONE
            if(curItem.distance.toInt() == 0) { //if it is 0, means the user never entered location permissions, so just display city and state
                holder.timeLeft.text = "${curItem._embedded.venues[0].city.name}, ${curItem._embedded.venues[0].state.stateCode}"
                holder.timeLeft.textSize = 14f
                holder.timeLeft.setTextColor(Color.parseColor("#000000"))
            } else if (curItem.distance.roundToInt() <= 1) {
                holder.timeLeft.text = "1 Mile Away!"
                holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
            } else {
                holder.timeLeft.text = curItem.distance.roundToInt().toString() + " Miles Away!"
                holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
            }

        } else  {

            //idk why this is so dumb looking https://stackoverflow.com/questions/42553017/android-calculate-days-between-two-dates
            var date = sdf.parse(curItem.dates.start.localDate)
            val millionSeconds = date.time - Calendar.getInstance().timeInMillis
            var daysLeft = millionSeconds/(24*60*60*1000)+1
            if (daysLeft < 1) { //expired
                val usersFavorites = db.document("users/${user.uid}/")
                usersFavorites.update("favorites", FieldValue.arrayRemove(eventData[position].id))
                //maybe see if i can just call delete favorite
            } else if (daysLeft > 21) { //https://stackoverflow.com/questions/8472349/how-to-set-text-color-of-a-textview-programmatically
                holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
            } else {
                holder.timeLeft.setTextColor(Color.parseColor("#FF0000"))
            }
            holder.timeLeft.text = "$daysLeft Days Left!"

        }
        holder.eventName.text = "${curItem.name}"
        holder.eventLocation.text = "${curItem._embedded.venues[0].name}"
        if(UserFavorites.favoriteIds.contains(curItem.id)) {
            holder.checkFavorite.isChecked = true
        } else {
            holder.checkFavorite.isChecked = false
        }
        //holder.checkFavorite.isChecked = UserFavorites.favoriteIds.contains(curItem.id)

        var stringDate = curItem.dates.start.localDate
        var date = java.text.SimpleDateFormat("yyyy-MM-dd").parse(stringDate)
        stringDate = java.text.SimpleDateFormat("MM/dd/yyyy").format(date)
        holder.textDate.text = stringDate

        val highestQualityImage = curItem.images.maxByOrNull {
            it.width.toInt() * it.height.toInt()

        }
        val context = holder.itemView.context
        Glide.with(context).load(highestQualityImage?.url).into(holder.image)



    }

    override fun getItemCount(): Int {
        return eventData.size
    }


    private fun createDialog(position: Int, checkBox: CheckBox) {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setTitle("Remove from Favorites")
        builder.setMessage("Are you sure you want to remove this from your favorites?")
        builder.setPositiveButton("Yes") { dialog, which ->
            //had weird alias thing when updating the recycler view, so remove from the view, then remove from the static list and db
            val eventToRemove = eventData[position]
            eventData.remove(eventData[position])
            FirestoreRepo.deleteFavorite(eventToRemove)
            notifyItemRemoved(position) //https://stackoverflow.com/questions/26076965/android-recyclerview-addition-removal-of-items
        }
        builder.setNegativeButton("No") { dialog, which ->
            checkBox.isChecked = true
            dialog.dismiss()
        }
        builder.show()
    }

    private fun moreInfoDialog(event: EventData) {
        CoroutineScope(Dispatchers.Main + CoroutineName("MoreInfo")).launch {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Event Info")
            builder.setPositiveButton("Close") { dialog, _->
                dialog.dismiss()
            }
            //put the message text in a scroll view
            val textResponse = TextView(context)
            val scrollView = ScrollView(context)
            textResponse.setPadding(24,12,24,12);

            textResponse.textSize = 18f
            //holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
            textResponse.setTextColor(Color.parseColor("#262626"))
            scrollView.addView(textResponse)
            builder.setView(scrollView)


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


            var prompt = "Give me a 150 word max summary about: ${event.name}. " + //Include a 1 sentence description about the venue: ${event._embedded.venues[0].name}. " +
                    "Provide appropriate line breaks."
            builder.show()
            //val response = generativeModel.generateContent(prompt)
            //Log.d(TAG, "getRecommended: ${response.text}")
            var fullResponse = ""
            generativeModel.generateContentStream(prompt).collect { chunk ->
                fullResponse += chunk.text?.replace("*", "")
                textResponse.text = fullResponse
            }



        }
    }




}
