package com.iconbiztechnologies1.mynetcape

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class ScreenTimeActivity : AppCompatActivity() {

    private val TAG = "ScreenTimeActivity"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- Data Properties ---
    private lateinit var userId: String
    private lateinit var physicalDeviceId: String
    private lateinit var screenTimeDocRef: DocumentReference

    // --- UI Elements ---
    private lateinit var hourPicker: NumberPicker
    private lateinit var minutePicker: NumberPicker
    private lateinit var tvSelectedTime: TextView
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView

    private val isUpdating = AtomicBoolean(false)

    // --- Screen Time Settings ---
    private var selectedHours = 2 // Default value
    private var selectedMinutes = 0 // Default value

    companion object {
        const val KEY_USER_ID = "USER_ID"
        const val KEY_PHYSICAL_DEVICE_ID = "PHYSICAL_DEVICE_ID"
        const val KEY_SCREEN_TIME_HOURS = "screen_time_hours"
        const val KEY_SCREEN_TIME_MINUTES = "screen_time_minutes"
        const val KEY_TOTAL_MINUTES = "screen_time_total_minutes"
        const val KEY_RESET_TIME = "reset_screen_time"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen_time)

        // This activity is unusable without the child's IDs. Validate them immediately.
        val receivedUserId = intent.getStringExtra(KEY_USER_ID)
        val receivedDeviceId = intent.getStringExtra(KEY_PHYSICAL_DEVICE_ID)

        if (receivedUserId.isNullOrEmpty() || receivedDeviceId.isNullOrEmpty()) {
            Log.e(TAG, "FATAL: Device information (UserID or DeviceID) is missing in intent.")
            Toast.makeText(this, "Error: Could not identify the target device.", Toast.LENGTH_LONG).show()
            finish() // Exit the activity
            return
        }
        userId = receivedUserId
        physicalDeviceId = receivedDeviceId

        initializeViews()
        setupNavigationDrawer()

        // Build the direct reference to the child's screen time document in Firestore.
        screenTimeDocRef = db.collection("ScreenTime")
            .document(userId)
            .collection("devices")
            .document(physicalDeviceId)

        loadScreenTimeSettings()

        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun initializeViews() {
        hourPicker = findViewById(R.id.hourPicker)
        minutePicker = findViewById(R.id.minutePicker)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        menuIcon = findViewById(R.id.menu_icon)
    }

    private fun setupNavigationDrawer() {
        menuIcon.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.appmonitoring -> navigateToActivity(DeviceActivity::class.java)
                R.id.setScreenTime -> { /* Already on this screen */ }
                R.id.block -> navigateToActivity(DownloadedApp::class.java)
                R.id.help -> Toast.makeText(this, "Help is coming soon!", Toast.LENGTH_SHORT).show()
                R.id.logout -> handleLogout()
                else -> false
            }.let { true }
        }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            // Pass the critical IDs to the next activity
            putExtra(KEY_USER_ID, userId)
            putExtra(KEY_PHYSICAL_DEVICE_ID, physicalDeviceId)
        }
        startActivity(intent)
    }

    private fun handleLogout() {
        auth.signOut()
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun loadScreenTimeSettings() {
        Log.d(TAG, "Loading settings for user: $userId, device: $physicalDeviceId")
        showLoading(true)

        screenTimeDocRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                Log.d(TAG, "Found existing screen time document in Firestore.")
                selectedHours = document.getLong(KEY_SCREEN_TIME_HOURS)?.toInt() ?: 2
                selectedMinutes = document.getLong(KEY_SCREEN_TIME_MINUTES)?.toInt() ?: 0
            } else {
                Log.d(TAG, "No settings found in Firestore. Using local defaults (2h 0m).")
                // Keep the default values: 2 hours, 0 minutes
            }
            setupNumberPickers()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error loading screen time settings from Firestore", e)
            Toast.makeText(this, "Failed to load settings. Using default values.", Toast.LENGTH_SHORT).show()
            setupNumberPickers()
        }.addOnCompleteListener {
            showLoading(false)
        }
    }

    private fun setupNumberPickers() {
        hourPicker.minValue = 0
        hourPicker.maxValue = 23
        minutePicker.minValue = 0
        minutePicker.maxValue = 59

        hourPicker.value = selectedHours
        minutePicker.value = selectedMinutes

        val listener = NumberPicker.OnValueChangeListener { _, _, _ ->
            selectedHours = hourPicker.value
            selectedMinutes = minutePicker.value
            updateTimeDisplay()
        }
        hourPicker.setOnValueChangedListener(listener)
        minutePicker.setOnValueChangedListener(listener)

        updateTimeDisplay()
    }

    private fun updateTimeDisplay() {
        tvSelectedTime.text = String.format(Locale.getDefault(), "%d hours, %d minutes", selectedHours, selectedMinutes)
    }

    private fun saveSettings() {
        // Prevent multiple simultaneous save operations
        if (isUpdating.getAndSet(true)) {
            Toast.makeText(this, "Save already in progress...", Toast.LENGTH_SHORT).show()
            return
        }
        showLoading(true)

        val totalMinutes = (selectedHours * 60) + selectedMinutes

        // This is the data structure the child app's service is expecting.
        val settingsData = hashMapOf(
            "user_id" to userId,
            "device_id" to physicalDeviceId,
            KEY_SCREEN_TIME_HOURS to selectedHours,
            KEY_SCREEN_TIME_MINUTES to selectedMinutes,
            KEY_TOTAL_MINUTES to totalMinutes,
            // We explicitly set the reset time to "00:00" to match the child app's hardcoded value.
            // This ensures perfect consistency.
            KEY_RESET_TIME to "00:00",
            "timestamp" to System.currentTimeMillis()
        )

        Log.d(TAG, "Saving to Firestore: $settingsData")

        // Use `set` with `SetOptions.merge()` to create the document if it doesn't exist,
        // or update the specified fields if it does. This is safer than a blind `set`.
        screenTimeDocRef.set(settingsData, SetOptions.merge())
            .addOnSuccessListener {
                Log.i(TAG, "Settings saved successfully to Firestore.")
                Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to save settings to Firestore", e)
                Toast.makeText(this, "Error: Could not save settings.", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                // This runs regardless of success or failure
                showLoading(false)
                isUpdating.set(false)
            }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
        hourPicker.isEnabled = !isLoading
        minutePicker.isEnabled = !isLoading
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}