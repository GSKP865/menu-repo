package com.menu.app.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.menu.app.R
import com.menu.app.domain.auth.AuthServices
import com.menu.app.home.HomeActivity

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        val destination = if (AuthServices.repository.currentSession() != null) HomeActivity::class.java else LoginActivity::class.java
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, destination))
            finish()
        }, SPLASH_DISPLAY_MS)
    }

    companion object {
        private const val SPLASH_DISPLAY_MS = 2000L
    }
}
