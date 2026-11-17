package com.menu.app.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.menu.app.auth.LoginActivity
import com.menu.app.core.model.Gender
import com.menu.app.domain.auth.AuthServices
import com.menu.app.reference.ReferenceActivity

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val profile = AuthServices.repository.currentSession()
        if (com.menu.app.BuildConfig.TABLE_TEST_LOGIN && profile != null) {
            startActivity(Intent(this, com.menu.app.calling.VoiceLobbyActivity::class.java))
            finish()
            return
        }
        val destination = if (profile == null) Intent(this, LoginActivity::class.java)
            else Intent(this, ReferenceActivity::class.java).putExtra("female", profile.gender == Gender.FEMALE)
        startActivity(destination)
        finish()
    }
}
