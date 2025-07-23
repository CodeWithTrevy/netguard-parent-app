package com.iconbiztechnologies1.mynetcape

import OnboardingAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        // Set Adapter
        val images = listOf(R.drawable.kid1, R.drawable.kid2, R.drawable.kid3, R.drawable.imge4)
        val descriptions = listOf(
            "First onboarding screen showing a child",
            "Second onboarding screen with another child",
            "Third onboarding screen with a third child",
            "Fourth onboarding screen with final image"
        )

        viewPager.adapter = OnboardingAdapter(images, this, descriptions)

        // Set content descriptions for better accessibility
        viewPager.importantForAccessibility = ViewPager2.IMPORTANT_FOR_ACCESSIBILITY_YES

        // Link TabLayout with ViewPager and add descriptive tab titles
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            // You can optionally set text for each tab
            // tab.text = "Page ${position + 1}"

            // Set content description for each tab
            tab.contentDescription = "Page ${position + 1}: ${descriptions[position]}"
        }.attach()

        // Register page change callbacks for announcements
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Announce page change for accessibility
                viewPager.announceForAccessibility("Showing ${descriptions[position]}")
            }
        })

        // Listen for Get Started Button
        btnGetStarted.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}