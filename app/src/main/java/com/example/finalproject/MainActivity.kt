package com.example.finalproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.finalproject.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    //TO ASK YUSUF: HOW DO I CHANGE THE TEXT AT THE TOP OF THE ACTION BAR, WHY CANT I HIDE THE KEYBOARD WHEN I NAVIGATE BACK
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

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
                R.id.nav_search_ticket, R.id.nav_sign_out, R.id.nav_favorites
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)


        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            navView.menu.findItem(R.id.nav_sign_out).title = "Login"
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


    //HELPER FUNCTIONS

    fun setFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        fragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, fragment)
            .commit()
    }
    //THIS DOES NOT WORK
    private fun View.hideKeyboard() {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onResume() {
        super.onResume()
        //want to hide the keyboard when they go back to it
        //view = findViewById(android.R.id.content).getRootView().getWindowToken();  https://rmirabelle.medium.com/close-hide-the-soft-keyboard-in-android-db1da22b09d2
        findViewById<View>(android.R.id.content).hideKeyboard()

    }





}