package com.iconbiztechnologies1.mynetcape

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Navigate to Onboarding Screen after 3 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                val intent = Intent(this, OnboardingActivity::class.java)
                startActivity(intent)
                finish() // Close SplashActivity
            }, 3000)
    }
}
