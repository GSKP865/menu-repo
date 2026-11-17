package com.menu.app.notifications

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class NotificationsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "Notifications Activity"
            textSize = 24f
            setPadding(32, 32, 32, 32)
        }
        setContentView(textView)
    }
}
