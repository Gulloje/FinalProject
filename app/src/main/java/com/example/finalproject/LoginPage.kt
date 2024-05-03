package com.example.finalproject

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class LoginPage : AppCompatActivity() {
    private val TAG = "RegisterActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)


        //Set the title of the default navbar
        getSupportActionBar()?.setTitle("Some App Name");

        val curUser = FirebaseAuth.getInstance().currentUser
        //continue if a user already exists
        if (curUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            val signActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user != null && (user.metadata?.creationTimestamp ==user.metadata?.lastSignInTimestamp)) {
                        FirestoreRepo.addUser(user)
                    }
                    FirestoreRepo.setUser(user)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    val response = IdpResponse.fromResultIntent(result.data)
                    if (response == null) {
                        Log.d(TAG, "onActivityResult: the user has cancelled the sign in request")
                    } else {
                        Log.d(TAG, "onActivityResult: ${response.error?.errorCode}")
                    }
                }
            }
            findViewById<Button>(R.id.btnLogin).setOnClickListener {
                // Choose authentication providers -- make sure enable them on your firebase account first
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build()
                )

                // Create  sign-in intent
                val signInIntent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .setAlwaysShowSignInMethodScreen(false) // use this if you have only one provider and really want the see the signin page
                    .setIsSmartLockEnabled(false)
                    .build()

                // Launch sign-in Activity with the sign-in intent above
                signActivityLauncher.launch(signInIntent)
            }
        }
        findViewById<TextView>(R.id.textBtnNoLogin).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

    }
    /*private fun addUserToFirestore(user: FirebaseUser) {
        val db = FirebaseFirestore.getInstance()
        val userData = mapOf<String, Any>() //since i dont really care about also writing the uid as a field under the document with uid, just need an object to send to .set
        db.collection("users").document(user.uid).set(userData)
            .addOnFailureListener {
                Log.d(TAG, "Error adding user.")
            }
    }*/


}