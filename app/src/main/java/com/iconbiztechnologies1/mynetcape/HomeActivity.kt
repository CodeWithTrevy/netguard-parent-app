package com.iconbiztechnologies1.mynetcape

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
// This import is now required because the class is in a separate file
import com.iconbiztechnologies1.mynetcape.ChildDevice
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


// THE DATA CLASS DEFINITION IS REMOVED FROM THIS FILE

class HomeActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuIcon: ImageView

    // --- UI Elements from your XML ---
    private lateinit var addChildCard: CardView
    private lateinit var childCard: CardView
    private lateinit var addChildText: TextView
    private lateinit var childNameTextView: TextView
    private lateinit var childInitialTextView: TextView

    // --- REFACTORED: From single variables to a list and a current selection ---
    private val childDeviceList = mutableListOf<ChildDevice>()
    private var currentChild: ChildDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        initializeComponents()
        setupNavigationDrawer()
        checkUserAuthentication()
    }

    private fun initializeComponents() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        menuIcon = findViewById(R.id.menu_icon)

        // --- Initialize cards and text views from your layout ---
        addChildCard = findViewById(R.id.add_child_card)
        childCard = findViewById(R.id.child_card)
        addChildText = findViewById(R.id.add_child_text)
        childNameTextView = findViewById(R.id.child_name_text)
        childInitialTextView = findViewById(R.id.child_initial)

        // Set initial visibility state
        childCard.visibility = View.GONE
        addChildCard.visibility = View.GONE
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
        // --- UPDATED: All actions now refer to the 'currentChild' ---
        if (currentChild == null) {
            Toast.makeText(this, "Please select a child's profile first.", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.START)
            return false
        }

        val selectedChild = currentChild!! // We know it's not null here
        return when (itemId) {
            R.id.appmonitoring -> {
                navigateToChildActivity(DeviceActivity::class.java, selectedChild)
                true
            }
            R.id.setScreenTime -> {
                navigateToChildActivity(ScreenTimeActivity::class.java, selectedChild)
                true
            }
            R.id.block -> {
                navigateToChildActivity(DownloadedApp::class.java, selectedChild)
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

    /**
     * UPDATED: Navigates to a specified activity, passing the selected child's details.
     */
    private fun navigateToChildActivity(activityClass: Class<*>, device: ChildDevice) {
        val intent = Intent(this, activityClass)
        intent.putExtra("USER_ID", device.userId)
        intent.putExtra("DEVICE_NAME", device.deviceName)
        intent.putExtra("PHYSICAL_DEVICE_ID", device.physicalDeviceId) // Pass this too
        startActivity(intent)
    }

    private fun handleLogout() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun checkUserAuthentication() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            fetchDeviceAndChildInfo(currentUser.uid)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * REFACTORED: Fetches ALL children/devices linked to the user ID.
     */
    private fun fetchDeviceAndChildInfo(userId: String) {
        db.collection("Devices")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                childDeviceList.clear() // Clear any old data
                if (!documents.isEmpty) {
                    for (doc in documents) {
                        // Create a ChildDevice object from the Firestore document
                        val child = ChildDevice(
                            userId = doc.getString("user_id") ?: "",
                            deviceName = doc.getString("device_name") ?: "",
                            childName = doc.getString("child_name") ?: "No Name",
                            physicalDeviceId = doc.getString("physical_device_id") ?: ""
                        )
                        // Add only if data is valid
                        if (child.userId.isNotEmpty() && child.physicalDeviceId.isNotEmpty()) {
                            childDeviceList.add(child)
                        }
                    }
                }
                // After fetching, update the entire UI based on the list
                updateUiWithChildren()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching device data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * NEW & REFINED: Central function to control UI visibility and set up click listeners.
     */
    private fun updateUiWithChildren() {
        when {
            // Case 1: No children are linked to the account
            childDeviceList.isEmpty() -> {
                childCard.visibility = View.GONE
                addChildCard.visibility = View.VISIBLE
                addChildText.text = "Add a Child Profile"
                addChildCard.setOnClickListener {
                    // TODO: Navigate to your "Add Child" activity
                    Toast.makeText(this, "Navigate to Add Child screen", Toast.LENGTH_SHORT).show()
                }
            }

            // Case 2: Exactly one child is linked
            childDeviceList.size == 1 -> {
                currentChild = childDeviceList.first()
                displayChildOnCard(currentChild!!)
                addChildCard.visibility = View.GONE // Nothing to switch to
            }

            // Case 3: Multiple children are linked
            else -> {
                // Default to the first child in the list
                currentChild = childDeviceList.first()
                displayChildOnCard(currentChild!!)

                // Configure the "Add/Switch" card's behavior
                addChildCard.visibility = View.VISIBLE
                addChildText.text = "Switch Profile"
                addChildCard.setOnClickListener {
                    showChildSelectionDialog()
                }
            }
        }
    }

    /**
     * NEW: Displays a specific child's info on the main child card.
     */
    private fun displayChildOnCard(device: ChildDevice) {
        childNameTextView.text = device.childName
        childInitialTextView.text = if (device.childName.isNotEmpty()) {
            device.childName.first().toString().uppercase()
        } else {
            "?" // Use a placeholder for no name
        }
        childCard.visibility = View.VISIBLE

        // Make the main card clickable to navigate to the default dashboard
        childCard.setOnClickListener {
            navigateToChildActivity(DeviceActivity::class.java, device)
        }
    }

    /**
     * NEW: Shows an alert dialog to let the user select a child from the list.
     */
    private fun showChildSelectionDialog() {
        val childNames = childDeviceList.map { it.childName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select a Profile")
            .setItems(childNames) { dialog, which ->
                // 'which' is the index of the selected item
                currentChild = childDeviceList[which]
                // Update the UI to show the newly selected child
                displayChildOnCard(currentChild!!)
                Toast.makeText(this, "${currentChild?.childName}'s profile is now active", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}