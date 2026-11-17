package com.menu.app.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.BuildConfig
import com.menu.app.R
import com.menu.app.databinding.ActivityMaleDashboardBinding
import com.menu.app.databinding.ItemOnlinePersonBinding

class MaleDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMaleDashboardBinding
    // Preview fixtures only. Release stays empty until live presence is connected.
    private val people = if (BuildConfig.DEBUG) listOf(
        User("Sarah", 24, "Online", 50),
        User("Emma", 22, "Online", 45),
        User("Lisa", 26, "Busy", 55),
        User("Amy", 23, "Online", 40),
        User("Maria", 27, "Offline", 55)
    ) else emptyList()
    private val peopleAdapter = PeopleAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaleDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvOnlineCount.text = if (BuildConfig.DEBUG) "Preview · 3 online · sample profiles" else "Live availability is not connected yet"
        binding.rvPeople.layoutManager = LinearLayoutManager(this)
        binding.rvPeople.adapter = peopleAdapter
        binding.etSearch.doAfterTextChanged { filterPeople() }
        binding.switchOnline.setOnCheckedChangeListener { _, _ -> filterPeople() }
        binding.bottomNavigation.setOnItemSelectedListener {
            val destination = when (it.itemId) {
                R.id.nav_discover -> com.menu.app.discover.DiscoverActivity::class.java
                R.id.nav_wallet -> com.menu.app.wallet.WalletActivity::class.java
                R.id.nav_profile -> com.menu.app.profile.ProfileActivity::class.java
                else -> null
            }
            destination?.let { screen -> startActivity(Intent(this, screen)) }
            destination == null
        }
        filterPeople()
    }

    private fun filterPeople() {
        val query = binding.etSearch.text.toString().trim()
        peopleAdapter.rows = people.filter {
            (!binding.switchOnline.isChecked || it.status == "Online") && it.name.contains(query, ignoreCase = true)
        }
        peopleAdapter.notifyDataSetChanged()
        binding.tvEmpty.visibility = if (peopleAdapter.rows.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class PeopleAdapter : RecyclerView.Adapter<PeopleAdapter.Holder>() {
        var rows: List<User> = emptyList()
        override fun getItemCount() = rows.size
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemOnlinePersonBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(rows[position])
        inner class Holder(private val item: ItemOnlinePersonBinding) : RecyclerView.ViewHolder(item.root) {
            fun bind(user: User) {
                item.tvAvatar.text = user.name.take(1)
                item.tvName.text = "${user.name}, ${user.age}"
                item.tvStatus.text = "● ${user.status}"
                item.tvStatus.setTextColor(getColor(when (user.status) {
                    "Online" -> R.color.success
                    else -> R.color.text_secondary
                }))
                item.tvRate.text = "${user.coinRate} coins / min"
                item.btnCall.isEnabled = user.status == "Online"
                item.btnCall.contentDescription = "Call ${user.name}"
                item.btnCall.setOnClickListener {
                    Toast.makeText(this@MaleDashboardActivity, "Sample profile — live calling is not connected yet.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class User(val name: String, val age: Int, val status: String, val coinRate: Int)
}
