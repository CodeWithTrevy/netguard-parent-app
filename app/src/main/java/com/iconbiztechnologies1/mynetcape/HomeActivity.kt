package com.iconbiztechnologies1.mynetcape

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        val email = currentUser?.email

        if (email != null) {
            fetchChildName(email)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchChildName(email: String) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val childName = document.getString("child_name") ?: "No Name"
                        updateUI(childName)
                    }
                } else {
                    Toast.makeText(this, "No child linked to this email", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching child: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(childName: String) {
        val childNameTextView = findViewById<TextView>(R.id.child_name_text)
        val childInitialTextView = findViewById<TextView>(R.id.child_initial)
        val childCard = findViewById<CardView>(R.id.child_card)

        childNameTextView.text = childName
        childInitialTextView.text = childName.first().toString().uppercase()
        childCard.visibility = View.VISIBLE // Make the child card visible

        // Handle click event on child card
        childCard.setOnClickListener {
            fetchUserIdAndNavigate(childName)
        }
    }

    private fun fetchUserIdAndNavigate(childName: String) {
        // Assuming you already know the device_name, replace this with actual device name logic if needed
        val deviceName = "TECNO TECNO KL4" // Replace with the actual device name

        // Query Devices collection to get user_id based on child_name and device_name
        db.collection("Devices")
            .whereEqualTo("child_name", childName)
            .whereEqualTo("device_name", deviceName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents.first()
                    val userId = document.getString("user_id") // Get user_id from the document

                    if (userId != null) {
                        navigateToDeviceActivity(userId)
                    } else {
                        Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "No matching device found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToDeviceActivity(userId: String) {
        // Create an Intent and pass the user_id to DeviceActivity
        val intent = Intent(this, DeviceActivity::class.java)
        intent.putExtra("USER_ID", userId)
        startActivity(intent)
    }
}
