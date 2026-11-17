package com.menu.app.discover

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.menu.app.R
import com.menu.app.databinding.ActivityDiscoverBinding

class DiscoverActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDiscoverBinding
    private lateinit var usersAdapter: DiscoverUserAdapter
    
    private val users = listOf(
        DiscoverUser("Sarah", 24, "Hi, I love music and traveling", "English", true, 50),
        DiscoverUser("Emma", 22, "Looking for meaningful conversations", "English, Spanish", true, 45),
        DiscoverUser("Lisa", 26, "Let's talk about art and culture", "French", false, 55),
        DiscoverUser("Amy", 23, "Adventure seeker here!", "English", true, 40),
        DiscoverUser("Jennifer", 25, "Book lover and coffee enthusiast", "English", true, 60),
        DiscoverUser("Maria", 27, "Fitness and health coach", "Spanish", false, 55),
        DiscoverUser("Sophie", 24, "Photography and nature lover", "French", true, 50),
        DiscoverUser("Olivia", 23, "Tech and gaming enthusiast", "English", true, 45)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupFilters()
        setupSearch()
    }
    
    private fun setupRecyclerView() {
        usersAdapter = DiscoverUserAdapter(users) { user ->
            handleUserClick(user)
        }
        binding.rvUsers.apply {
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = usersAdapter
        }
    }
    
    private fun setupFilters() {
        binding.chipLanguage.setOnClickListener {
            // TODO: Show language filter dialog
        }
        
        binding.chipOnlineNow.setOnClickListener {
            // TODO: Toggle online filter
            val isFiltered = binding.chipOnlineNow.isSelected
            binding.chipOnlineNow.isSelected = !isFiltered
            filterUsers()
        }
    }
    
    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, _, _ ->
            filterUsers()
            true
        }
    }
    
    private fun filterUsers() {
        val searchTerm = binding.etSearch.text?.toString()?.lowercase() ?: ""
        val onlineOnly = binding.chipOnlineNow.isSelected
        
        val filteredUsers = users.filter { user ->
            val matchesSearch = user.name.lowercase().contains(searchTerm) ||
                               user.bio.lowercase().contains(searchTerm)
            val matchesOnline = !onlineOnly || user.isOnline
            matchesSearch && matchesOnline
        }
        
        usersAdapter.updateUsers(filteredUsers)
    }
    
    private fun handleUserClick(user: DiscoverUser) {
        // TODO: Navigate to user profile or initiate call
    }
    
    data class DiscoverUser(
        val name: String,
        val age: Int,
        val bio: String,
        val languages: String,
        val isOnline: Boolean,
        val coinRate: Int
    )
}
