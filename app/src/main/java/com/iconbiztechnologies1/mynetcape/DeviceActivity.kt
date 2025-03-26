package com.iconbiztechnologies1.mynetcape

import AppUsageModel
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class DeviceActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private val appUsageList = mutableListOf<AppUsageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device)

        db = FirebaseFirestore.getInstance()

        // Get USER_ID from Intent
        val userId = intent.getStringExtra("USER_ID") ?: ""

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppUsageAdapter(this, appUsageList)
        recyclerView.adapter = adapter

        if (userId.isNotEmpty()) {
            fetchAppUsageData(userId) // Use user_id to fetch data
        } else {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAppUsageData(userId: String) {
        db.collection("AppTracking")
            .whereEqualTo("user_id", userId) // Fetch data using user_id
            .get()
            .addOnSuccessListener { documents ->
                appUsageList.clear()
                for (document in documents) {
                    val appName = document.getString("app_name") ?: "Unknown App"
                    val usageTime = document.getLong("usage_time") ?: 0
                    val timestamp = document.getLong("timestamp") ?: 0
                    val packageName = document.getString("package_name") ?: "" // Get package name

                    // Get app icon using the package name
                    val appIcon = getAppIcon(packageName)

                    appUsageList.add(AppUsageModel(appName, usageTime, timestamp, appIcon))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to get app icon from package name
    private fun getAppIcon(packageName: String): Drawable? {
        return try {
            val packageManager: PackageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }
}
