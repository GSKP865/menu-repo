package com.menu.app.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.R
import com.menu.app.databinding.ItemDiscoverUserBinding

class DiscoverUserAdapter(
    private var users: List<DiscoverActivity.DiscoverUser>,
    private val onUserClick: (DiscoverActivity.DiscoverUser) -> Unit
) : RecyclerView.Adapter<DiscoverUserAdapter.DiscoverUserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoverUserViewHolder {
        val binding = ItemDiscoverUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiscoverUserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscoverUserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<DiscoverActivity.DiscoverUser>) {
        users = newUsers
        notifyDataSetChanged()
    }

    inner class DiscoverUserViewHolder(private val binding: ItemDiscoverUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: DiscoverActivity.DiscoverUser) {
            binding.tvName.text = user.name
            binding.tvAge.text = "${user.age} yrs"
            binding.tvBio.text = user.bio
            binding.tvLanguages.text = user.languages
            binding.tvCoinRate.text = "${user.coinRate} ${binding.root.context.getString(R.string.coin_rate)}"
            
            binding.vOnlineIndicator.visibility = if (user.isOnline) android.view.View.VISIBLE else android.view.View.GONE
            
            binding.btnCall.setOnClickListener {
                onUserClick(user)
            }
            
            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}
