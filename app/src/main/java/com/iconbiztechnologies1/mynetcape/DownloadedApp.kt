package com.iconbiztechnologies1.mynetcape

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class DownloadedApp : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: DownloadedAppsAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val activityScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // --- Data ---
    private val appsList = mutableListOf<AppInfo>()
    private val blockedApps = mutableSetOf<String>()
    private val originallyBlockedApps = mutableSetOf<String>()
    private lateinit var userId: String
    private lateinit var physicalDeviceId: String

    companion object {
        private const val TAG = "DownloadedApp"
        // Firestore Collection Names
        private const val INSTALLED_APPS_COLLECTION = "DeviceInstalledApps"
        private const val BLOCKED_APPS_COLLECTION = "blocked_apps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_downloaded_app)

        userId = intent.getStringExtra("USER_ID") ?: ""
        physicalDeviceId = intent.getStringExtra("PHYSICAL_DEVICE_ID") ?: ""

        initViews()
        setupNavigationDrawer()

        if (userId.isNotEmpty() && physicalDeviceId.isNotEmpty()) {
            // Load data in sequence: first blocked apps, then all apps
            loadBlockedAppsAndThenChildApps()
        } else {
            Toast.makeText(this, "Required device information is missing.", Toast.LENGTH_LONG).show()
            finish() // Can't function without this data
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityScope.cancel() // Prevent memory leaks
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerViewDownloadedApps)
        saveButton = findViewById(R.id.btnSaveBlockedApps)
        progressBar = findViewById(R.id.progressBarDownloaded)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        menuIcon = findViewById(R.id.menu_icon)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DownloadedAppsAdapter(appsList) { packageName, isBlocked ->
            if (isBlocked) blockedApps.add(packageName) else blockedApps.remove(packageName)
            updateSaveButtonText()
        }
        recyclerView.adapter = adapter

        saveButton.setOnClickListener { saveBlockedApps() }
        updateSaveButtonText() // Initial state
    }

    private fun setupNavigationDrawer() {
        menuIcon.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START)
            } else {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemClick(menuItem.itemId)
        }
    }

    // --- REFACTORED: Simplified Navigation ---
    private fun handleNavigationItemClick(itemId: Int): Boolean {
        drawerLayout.closeDrawer(GravityCompat.START)
        return when (itemId) {
            R.id.appmonitoring -> {
                navigateToNextActivity(DeviceActivity::class.java)
                true
            }
            R.id.setScreenTime -> {
                navigateToNextActivity(ScreenTimeActivity::class.java)
                true
            }
            R.id.block -> true // Already on this screen, do nothing
            R.id.help -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://p-961168.vercel.app/")))
                true
            }
            R.id.logout -> {
                handleLogout()
                true
            }
            else -> false
        }
    }

    // --- REFACTORED: New reusable navigation function ---
    private fun navigateToNextActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass).apply {
            putExtra("USER_ID", userId)
            putExtra("PHYSICAL_DEVICE_ID", physicalDeviceId)
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

    private fun loadBlockedAppsAndThenChildApps() {
        showLoading(true)
        // Correct path: /blocked_apps/{userId}/devices/{physicalDeviceId}/apps/{packageName}
        firestore.collection(BLOCKED_APPS_COLLECTION)
            .document(userId)
            .collection("devices")
            .document(physicalDeviceId)
            .collection("apps")
            .get()
            .addOnSuccessListener { documents ->
                blockedApps.clear()
                originallyBlockedApps.clear()
                for (document in documents) {
                    blockedApps.add(document.id)
                    originallyBlockedApps.add(document.id)
                }
                Log.d(TAG, "Loaded ${blockedApps.size} blocked apps for device $physicalDeviceId")
                // Now that we know which apps are blocked, load the full list
                loadChildApps()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading blocked apps", e)
                Toast.makeText(this, "Could not load blocked app rules.", Toast.LENGTH_SHORT).show()
                // Still try to load the apps list even if rules fail
                loadChildApps()
            }
    }

    private fun loadChildApps() {
        // Path: /DeviceInstalledApps/{physicalDeviceId}/apps/{packageName}
        firestore.collection(INSTALLED_APPS_COLLECTION)
            .document(physicalDeviceId)
            .collection("apps")
            .get()
            .addOnSuccessListener { snapshots ->
                appsList.clear()
                for (document in snapshots.documents) {
                    appsList.add(AppInfo(
                        packageName = document.id,
                        appName = document.getString("appName") ?: document.id,
                        iconUrl = document.getString("iconUrl") ?: "",
                        isBlocked = blockedApps.contains(document.id) // Check against our loaded set
                    ))
                }
                adapter.updateBlockedApps(blockedApps) // Ensure adapter has the latest blocked set
                adapter.notifyDataSetChanged()
                Log.d(TAG, "Loaded ${appsList.size} apps from child device $physicalDeviceId")
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading child apps", e)
                showLoading(false)
                Toast.makeText(this, "Error loading apps: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- CRITICAL FIX: Corrected Firestore path and logic ---
    private fun saveBlockedApps() {
        showLoading(true)
        activityScope.launch {
            try {
                val batch = firestore.batch()
                // Define the correct reference to the "apps" sub-collection for this specific device
                val deviceBlockedAppsCollectionRef = firestore.collection(BLOCKED_APPS_COLLECTION)
                    .document(userId)
                    .collection("devices")
                    .document(physicalDeviceId)
                    .collection("apps")

                // Apps to unblock: in original set but not in new set
                val appsToUnblock = originallyBlockedApps - blockedApps
                for (packageName in appsToUnblock) {
                    batch.delete(deviceBlockedAppsCollectionRef.document(packageName))
                }

                // Apps to block: in new set but not in original set
                val appsToBlock = blockedApps - originallyBlockedApps
                for (packageName in appsToBlock) {
                    val data = hashMapOf("blockedAt" to com.google.firebase.Timestamp.now())
                    batch.set(deviceBlockedAppsCollectionRef.document(packageName), data)
                }

                batch.commit().await()

                // Update original state to match the newly saved state for future comparisons
                originallyBlockedApps.clear()
                originallyBlockedApps.addAll(blockedApps)

                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@DownloadedApp, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                    updateSaveButtonText()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@DownloadedApp, "Error saving changes: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error saving blocked apps", e)
                }
            }
        }
    }

    private fun updateSaveButtonText() {
        val changes = (blockedApps - originallyBlockedApps).size + (originallyBlockedApps - blockedApps).size
        saveButton.text = if (changes > 0) "Save Changes ($changes)" else "No Changes"
        saveButton.isEnabled = changes > 0
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        // Disable save button while loading to prevent multiple clicks
        if (show) {
            saveButton.isEnabled = false
        } else {
            updateSaveButtonText() // Re-evaluate if save button should be enabled
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}