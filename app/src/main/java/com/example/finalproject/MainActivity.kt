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
import com.example.finalproject.databinding.ActivityMainBinding
import com.example.finalproject.ui.location.DiscoverViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    private val user = FirestoreRepo.getUser()

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastKnownLocation()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_search_ticket, R.id.nav_sign_out, R.id.nav_favorites, R.id.nav_popular
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
            UserFavorites.resetFavorites()
            FirestoreRepo.clearUser()
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

    private fun getLastKnownLocation () {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                5)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val locationData = mutableMapOf<String, Any>()
                    locationData["Latitude"] = String.format("%.6f", location.latitude)
                    locationData["Longitude"] = String.format("%.6f", location.longitude)
                    viewModel.setUserCoords(String.format("%.6f", location.latitude) +"," + String.format("%.6f", location.longitude))
                } else {
                    viewModel.setUserCoords("")
                }
                Log.d(TAG, "getLastKnownLocation: ${viewModel.cooridinates.value}")
            }
            .addOnFailureListener {
                viewModel.setUserCoords("")
                Log.d(TAG, "getLastKnownLocation FAILED: ${viewModel.cooridinates.value}")
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 5) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                getLastKnownLocation()
            } else {
                viewModel.setUserCoords("")
                Toast.makeText(this, "Location permission denied. Please enable it in settings to view events near you.", Toast.LENGTH_SHORT).show()
            }
        }
    }











}