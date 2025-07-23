package com.iconbiztechnologies1.mynetcape

import AppUsageModel
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DeviceActivity : AppCompatActivity() {

    // --- UI and Firebase ---
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppUsageAdapter
    private lateinit var emptyStateView: LinearLayout
    private lateinit var tvTotalTime: TextView
    private lateinit var tvMostUsedApp: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvDeviceName: TextView

    // --- Data ---
    private val appUsageList = mutableListOf<AppUsageModel>()
    private lateinit var userId: String
    private lateinit var physicalDeviceId: String
    private lateinit var deviceName: String

    private val popularApps = mapOf(
        "com.whatsapp" to R.drawable.ic_generic_chat,
        "com.google.android.youtube" to R.drawable.ic_generic_video,
        "com.facebook.katana" to R.drawable.ic_generic_social,
        "com.instagram.android" to R.drawable.ic_generic_social,
        "com.twitter.android" to R.drawable.ic_generic_social,
        "com.spotify.music" to R.drawable.ic_generic_music,
        "com.netflix.mediaclient" to R.drawable.ic_generic_video,
        "com.google.android.gm" to R.drawable.ic_generic_mail,
        "com.android.chrome" to R.drawable.ic_generic_browser
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        userId = intent.getStringExtra("USER_ID") ?: ""
        physicalDeviceId = intent.getStringExtra("PHYSICAL_DEVICE_ID") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: "Device"

        initializeViews()
        setupNavigationDrawer()

        tvDeviceName.text = deviceName
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        tvDate.text = dateFormat.format(Date())

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AppUsageAdapter(this, appUsageList)
        recyclerView.adapter = adapter

        if (userId.isNotEmpty() && physicalDeviceId.isNotEmpty()) {
            fetchAppUsageData()
        } else {
            Toast.makeText(this, "Required device information is missing.", Toast.LENGTH_LONG).show()
            showEmptyState()
        }

        val setRules = findViewById<TextView>(R.id.tvSetRules)
        setRules.setOnClickListener {
            navigateToNextActivity(ScreenTimeActivity::class.java)
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateView = findViewById(R.id.emptyStateView)
        tvTotalTime = findViewById(R.id.tvTotalTime)
        tvMostUsedApp = findViewById(R.id.tvMostUsedApp)
        tvDate = findViewById(R.id.tvDate)
        tvDeviceName = findViewById(R.id.tvDeviceName)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        menuIcon = findViewById(R.id.menu_icon)
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

    private fun handleNavigationItemClick(itemId: Int): Boolean {
        return when (itemId) {
            R.id.appmonitoring -> true
            R.id.setScreenTime -> {
                navigateToNextActivity(ScreenTimeActivity::class.java)
                true
            }
            R.id.block -> {
                navigateToNextActivity(DownloadedApp::class.java)
                true
            }
            R.id.help -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://p-961168.vercel.app/")))
                true
            }
            R.id.logout -> {
                handleLogout()
                true
            }
            else -> false
        }.also {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun navigateToNextActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        intent.putExtra("USER_ID", userId)
        intent.putExtra("PHYSICAL_DEVICE_ID", physicalDeviceId)
        intent.putExtra("DEVICE_NAME", deviceName)
        startActivity(intent)
    }

    private fun handleLogout() {
        auth.signOut()
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchAppUsageData() {
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("AppUsage")
            .whereEqualTo("parent_user_id", userId)
            .whereEqualTo("physical_device_id", physicalDeviceId)
            .whereEqualTo("date", todayDate)
            .get()
            .addOnSuccessListener { documents ->
                appUsageList.clear()
                var totalUsageTime: Long = 0
                var mostUsedAppTime: Long = 0
                var mostUsedAppName = "-"

                if (documents.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                for (document in documents) {
                    val appName = document.getString("app_name") ?: "Unknown App"
                    val usageTime = document.getLong("usage_time_ms") ?: 0
                    val timestamp = document.getLong("timestamp") ?: 0
                    val packageName = document.getString("package_name") ?: ""
                    val appIcon = getAppIcon(packageName)

                    // --- THIS IS THE CORRECTED LINE ---
                    // Using named arguments to avoid constructor errors.
                    appUsageList.add(AppUsageModel(
                        appName = appName,
                        usageTime = usageTime,
                        timestamp = timestamp,
                        appIcon = appIcon,
                        packageName = packageName
                        // usagePercentage is automatically set to its default value of 0
                    ))

                    totalUsageTime += usageTime
                    if (usageTime > mostUsedAppTime) {
                        mostUsedAppTime = usageTime
                        mostUsedAppName = appName
                    }
                }
                appUsageList.sortByDescending { it.usageTime }
                val maxUsage = appUsageList.firstOrNull()?.usageTime ?: 0
                if (maxUsage > 0) {
                    for (app in appUsageList) {
                        app.usagePercentage = (app.usageTime * 100 / maxUsage).toInt()
                    }
                }
                updateUsageStats(totalUsageTime, mostUsedAppName)
                adapter.notifyDataSetChanged()
                showAppList()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }

    private fun getAppIcon(packageName: String): Drawable? {
        try {
            if (packageName.isNotEmpty()) {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                return packageManager.getApplicationIcon(appInfo)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not installed, proceed to fallbacks
        }

        if (packageName.isNotEmpty() && popularApps.containsKey(packageName)) {
            return ContextCompat.getDrawable(this, popularApps[packageName]!!)
        }
        return ContextCompat.getDrawable(this, R.drawable.ic_generic_app)
    }

    private fun updateUsageStats(totalUsageTime: Long, mostUsedAppName: String) {
        val hours = TimeUnit.MILLISECONDS.toHours(totalUsageTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(totalUsageTime) % 60
        tvTotalTime.text = "${hours}h ${minutes}m"
        tvMostUsedApp.text = mostUsedAppName
    }

    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateView.visibility = View.VISIBLE
        tvTotalTime.text = "0h 0m"
        tvMostUsedApp.text = "-"
    }

    private fun showAppList() {
        recyclerView.visibility = View.VISIBLE
        emptyStateView.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}