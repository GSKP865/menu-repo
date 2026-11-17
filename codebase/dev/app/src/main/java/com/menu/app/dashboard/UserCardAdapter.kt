package com.menu.app.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.R
import com.menu.app.databinding.ItemUserCardBinding

class UserCardAdapter(
    private val users: List<MaleDashboardActivity.User>,
    private val onUserClick: (MaleDashboardActivity.User) -> Unit
) : RecyclerView.Adapter<UserCardAdapter.UserCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserCardViewHolder {
        val binding = ItemUserCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserCardViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserCardViewHolder(private val binding: ItemUserCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: MaleDashboardActivity.User) {
            binding.tvName.text = user.name
            binding.tvAge.text = "${user.age} yrs"
            binding.tvStatus.text = user.status
            binding.tvCoinRate.text = "${user.coinRate} ${binding.root.context.getString(R.string.coin_rate)}"
            
            binding.btnQuickCall.setOnClickListener {
                onUserClick(user)
            }
            
            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}
