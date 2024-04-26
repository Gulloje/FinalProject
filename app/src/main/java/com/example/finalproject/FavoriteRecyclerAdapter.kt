
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class FavoriteRecyclerAdapter(private val context: Context, private val eventData: ArrayList<EventData>): RecyclerView.Adapter<FavoriteRecyclerAdapter.FavoriteHolder>()
{
    private val user = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
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
                //update favorite status based on checkbox
                if (!isChecked) {

                    createDialog(adapterPosition)
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
        if (daysLeft < 1) { //COMEBACK, idk what happens if you send an old id
            //removeFavorite(position)
        } else if (daysLeft > 21) { //https://stackoverflow.com/questions/8472349/how-to-set-text-color-of-a-textview-programmatically
            holder.timeLeft.setTextColor(Color.parseColor("#00C40D"))
        } else {
            holder.timeLeft.setTextColor(Color.parseColor("#FF0000"))
        }
        holder.timeLeft.text = "$daysLeft Days Left!"
        holder.checkFavorite.isChecked = eventData.contains(curItem)

        val highestQualityImage = curItem.images.maxByOrNull {
            it.width.toInt() * it.height.toInt()

        }
        val context = holder.itemView.context
        Glide.with(context).load(highestQualityImage?.url).into(holder.image)



    }

    override fun getItemCount(): Int {
        return eventData.size
    }


    private fun createDialog(position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        builder.setTitle("Remove from Favorites")
        builder.setMessage("Are you sure you want to remove this from your favorites?")
        builder.setPositiveButton("Yes") { dialog, which ->
            removeFavorite(position)
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.show()
    }

    //should remove from firebase and from temp list
    //https://stackoverflow.com/questions/26076965/android-recyclerview-addition-removal-of-items
    private fun removeFavorite(position: Int) {
        val usersFavorites = db.document("users/${user.uid}/")
        usersFavorites.update("favorites", FieldValue.arrayRemove(eventData[position].id))
        eventData.removeAt(position)
        notifyItemRemoved(position)

    }





}
