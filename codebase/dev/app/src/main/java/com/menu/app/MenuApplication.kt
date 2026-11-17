package com.menu.app

import android.app.Application
import com.menu.app.domain.auth.AuthServices

class MenuApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthServices.initialize(applicationContext)
    }
}
