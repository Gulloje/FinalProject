
package com.example.finalproject

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth


class FavoriteRecyclerAdapter(private val context: Context, private val eventData: ArrayList<EventData>,
                              private val userFavorites: ArrayList<String>): RecyclerView.Adapter<FavoriteRecyclerAdapter.FavoriteHolder>()
{
    private val user = FirebaseAuth.getInstance()
    inner class FavoriteHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val eventName = itemView.findViewById<TextView>(R.id.textEventName)
        val timeLeft = itemView.findViewById<TextView>(R.id.textDaysLeft)
        val eventLocation = itemView.findViewById<TextView>(R.id.textLocation)
        val image = itemView.findViewById<ImageView>(R.id.imageView)
        val btnSeeTickets = itemView.findViewById<Button>(R.id.btnSeeTickets)
        val checkFavorite = itemView.findViewById<CheckBox>(R.id.checkFavorite)


        init {
            btnSeeTickets?.setOnClickListener {
                val browserIntent = Intent(Intent.ACTION_VIEW)
                browserIntent.data = Uri.parse(eventData[position].url)
                context.startActivity( browserIntent)
            }


            checkFavorite?.setOnCheckedChangeListener { buttonView, isChecked ->
                if (user == null) {
                    Toast.makeText(context, "Must Login to Favorite Events", Toast.LENGTH_SHORT).show()
                    checkFavorite.isChecked = false
                    //holder.checkFavorite.visibility = View.GONE //if i think it is better to just hide the button altogether
                    return@setOnCheckedChangeListener
                }

                val currentEventId = eventData[position].id

                //update favorite status based on checkbox
                if (!isChecked) {
                    //deleteFavorite(currentEventId)
                    createDialog()
                    userFavorites.remove(currentEventId)
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteRecyclerAdapter.FavoriteHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.favorite_item, parent, false)
        return FavoriteHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavoriteHolder, position: Int) {
        val curItem = eventData[position]
        holder.eventName.text = "${curItem.name}"
        holder.eventLocation.text = "${curItem._embedded.venues[0].name}"

        val sdf = SimpleDateFormat("yyyy-MM-dd")


        //idk why this is so dumb looking https://stackoverflow.com/questions/42553017/android-calculate-days-between-two-dates
        var date = sdf.parse(curItem.dates.start.localDate)
        val millionSeconds = date.time - Calendar.getInstance().timeInMillis
        var daysLeft = millionSeconds/(24*60*60*1000)+1
        if (daysLeft > 21) { //https://stackoverflow.com/questions/8472349/how-to-set-text-color-of-a-textview-programmatically
            holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
        } else {
            holder.timeLeft.setTextColor(Color.parseColor("#FF0000"))
        }
        holder.timeLeft.text = "$daysLeft Days Left!"
        holder.checkFavorite.isChecked = true

        val highestQualityImage = curItem.images.maxByOrNull {
            it.width.toInt() * it.height.toInt()

        }
        val context = holder.itemView.context
        Glide.with(context).load(highestQualityImage?.url).into(holder.image)



    }

    override fun getItemCount(): Int {
        return eventData.size
    }

    private fun createDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setTitle("Remove from Favorites")
        builder.setMessage("Are you sure you want to remove this from your favorites?")
        builder.setPositiveButton("Yes") { dialog, which ->
            // Code to remove the item from favorites
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }



}
