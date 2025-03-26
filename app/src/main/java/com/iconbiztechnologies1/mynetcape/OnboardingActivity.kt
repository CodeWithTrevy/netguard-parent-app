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

        // Set Adapter
        val images = listOf(R.drawable.kid1, R.drawable.kid2, R.drawable.kid3, R.drawable.imge4)
        viewPager.adapter = OnboardingAdapter(images, this)

        // Link TabLayout with ViewPager
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Listen for Get Started Button
        findViewById<Button>(R.id.btnGetStarted).setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
