package com.example.finalproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.example.finalproject.databinding.ActivityMainBinding
import com.example.finalproject.ui.home.HomeViewModel
import com.example.finalproject.ui.location.DiscoverViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    //TO ASK YUSUF: HOW DO I CHANGE THE TEXT AT THE TOP OF THE ACTION BAR, WHY CANT I HIDE THE KEYBOARD WHEN I NAVIGATE BACK
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private val user = FirebaseAuth.getInstance().currentUser
    private val db = FirebaseFirestore.getInstance()

    private val TAG = "MainActivity"


    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        /*binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }*/


        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_search_ticket, R.id.nav_sign_out, R.id.nav_favorites, R.id.nav_popular, R.id.nav_popular
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)




        if (user == null) {
            navView.menu.findItem(R.id.nav_sign_out).title = "Login"
            navView.getHeaderView(0).findViewById<TextView>(R.id.navUsername).text = "Login Below"
            navView.getHeaderView(0).findViewById<TextView>(R.id.navEmail).text = ""

        } else {
            navView.getHeaderView(0).findViewById<TextView>(R.id.navUsername).text = "Welcome ${user.displayName}!"
            navView.getHeaderView(0).findViewById<TextView>(R.id.navEmail).text = "${user.email}"
        }
        //if i need to override the default menu behavior, ex for logging out
        val signOutItem = navView.menu.findItem(R.id.nav_sign_out)
        signOutItem.setOnMenuItemClickListener {
            AuthUI.getInstance().signOut(this)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val intent = Intent(this, LoginPage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Cannot Log Out at This Time", Toast.LENGTH_SHORT).show()
                    }
                }
            true //consumes the button click
        }


    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    private val viewModel: DiscoverViewModel by viewModels()
    override fun onResumeFragments() {
        super.onResumeFragments()
        //deal with getting location
        Log.d(TAG, "onResumeFragments: ????????")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
            getUserLocation()
            user

        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                5)
        }

    }


    //HELPER FUNCTIONS

    //https://www.geeksforgeeks.org/how-to-get-current-location-in-android/#
    //i just needed longitude and latitude
    private fun getUserLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            interval = TimeUnit.SECONDS.toMillis(60)
            fastestInterval = TimeUnit.SECONDS.toMillis(30)
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.lastLocation?.let {
                    Log.d(TAG, "onLocationResult:$it \n Latitude=${String.format("%.6f", it.latitude)} Longitude=${it.longitude}")
                    val locationData = mutableMapOf<String, Any>()
                    locationData["Latitude"] = String.format("%.6f", it.latitude)
                    locationData["Longitude"] = String.format("%.6f", it.longitude)
                    viewModel.setUserCoords(String.format("%.6f", it.latitude) +"," + String.format("%.6f", it.longitude))
                    //write location to firestore
                    db.document("users/${user?.uid}").update(locationData)
                        .addOnFailureListener {
                            Log.d(TAG, "onLocationResult: couldnt write data")
                        }
                }
            }
        }


        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }







}