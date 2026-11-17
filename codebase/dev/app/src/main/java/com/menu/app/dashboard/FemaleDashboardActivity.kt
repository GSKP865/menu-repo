package com.menu.app.dashboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.menu.app.R
import com.menu.app.databinding.ActivityFemaleDashboardBinding

class FemaleDashboardActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityFemaleDashboardBinding
    private lateinit var recentCallersAdapter: CallerCardAdapter
    private lateinit var upcomingCallsAdapter: ScheduledCallAdapter
    
    private val recentCallers = listOf(
        Caller("John", 28, "+120 Coins", "2 min ago"),
        Caller("Mike", 30, "+100 Coins", "5 min ago"),
        Caller("David", 25, "+150 Coins", "10 min ago"),
        Caller("Alex", 27, "+80 Coins", "15 min ago")
    )
    
    private val upcomingCalls = listOf(
        ScheduledCall("James", 32, "Today, 3:00 PM"),
        ScheduledCall("Robert", 29, "Today, 5:30 PM"),
        ScheduledCall("William", 35, "Tomorrow, 10:00 AM")
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFemaleDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerViews()
        setupBottomNavigation()
    }
    
    private fun setupRecyclerViews() {
        // Recent callers (horizontal)
        recentCallersAdapter = CallerCardAdapter(recentCallers) { caller ->
            handleCallerClick(caller)
        }
        binding.rvRecentCallers.apply {
            layoutManager = LinearLayoutManager(this@FemaleDashboardActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = recentCallersAdapter
        }
        
        // Upcoming calls (vertical)
        upcomingCallsAdapter = ScheduledCallAdapter(upcomingCalls) { call ->
            handleScheduledCallClick(call)
        }
        binding.rvUpcomingCalls.apply {
            layoutManager = LinearLayoutManager(this@FemaleDashboardActivity)
            adapter = upcomingCallsAdapter
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_discover -> {
                    // Navigate to discover
                    true
                }
                R.id.nav_wallet -> {
                    // Navigate to wallet
                    true
                }
                R.id.nav_profile -> {
                    startActivity(android.content.Intent(this, com.menu.app.profile.ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }
    }
    
    private fun handleCallerClick(caller: Caller) {
        // TODO: Navigate to caller profile or view call details
    }
    
    private fun handleScheduledCallClick(call: ScheduledCall) {
        // TODO: Navigate to scheduled call details
    }
    
    data class Caller(val name: String, val age: Int, val earnings: String, val timeAgo: String)
    data class ScheduledCall(val name: String, val age: Int, val scheduledTime: String)
}
