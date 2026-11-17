package com.menu.app.calling

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.menu.app.R
import com.menu.app.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCallBinding
    private var callDuration = 0
    private var isCallActive = false
    private val handler = Handler(Looper.getMainLooper())
    private val runnable = object : Runnable {
        override fun run() {
            if (isCallActive) {
                callDuration++
                updateCallDuration()
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val callerName = intent.getStringExtra(EXTRA_CALLER_NAME) ?: "Unknown"
        binding.tvCallerName.text = callerName
        
        setupCallControls()
        startCall()
    }
    
    private fun setupCallControls() {
        binding.btnMessage.setOnClickListener {
            // TODO: Open message interface
        }
        
        binding.btnSpeaker.setOnClickListener {
            toggleSpeaker()
        }
        
        binding.btnEndCall.setOnClickListener {
            endCall()
        }
        
        binding.btnVideo.setOnClickListener {
            // TODO: Video call feature (disabled for audio-only platform)
        }
    }
    
    private fun startCall() {
        isCallActive = true
        binding.tvCallStatus.text = "Connected"
        handler.post(runnable)
        
        // TODO: Initialize WebRTC audio call
    }
    
    private fun endCall() {
        isCallActive = false
        handler.removeCallbacks(runnable)
        
        // TODO: End WebRTC call
        
        finish()
    }
    
    private fun toggleSpeaker() {
        // TODO: Toggle speaker on/off
        val isSpeakerOn = binding.btnSpeaker.isSelected
        binding.btnSpeaker.isSelected = !isSpeakerOn
    }
    
    private fun updateCallDuration() {
        val minutes = callDuration / 60
        val seconds = callDuration % 60
        binding.tvCallDuration.text = String.format("%02d:%02d", minutes, seconds)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
    
    companion object {
        const val EXTRA_CALLER_NAME = "extra_caller_name"
    }
}
