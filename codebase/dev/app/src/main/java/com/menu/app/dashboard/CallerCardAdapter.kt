package com.menu.app.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.databinding.ItemCallerCardBinding

class CallerCardAdapter(
    private val callers: List<FemaleDashboardActivity.Caller>,
    private val onCallerClick: (FemaleDashboardActivity.Caller) -> Unit
) : RecyclerView.Adapter<CallerCardAdapter.CallerCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallerCardViewHolder {
        val binding = ItemCallerCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CallerCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CallerCardViewHolder, position: Int) {
        holder.bind(callers[position])
    }

    override fun getItemCount(): Int = callers.size

    inner class CallerCardViewHolder(private val binding: ItemCallerCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(caller: FemaleDashboardActivity.Caller) {
            binding.tvName.text = caller.name
            binding.tvAge.text = "${caller.age} yrs"
            binding.tvEarnings.text = caller.earnings
            binding.tvTimeAgo.text = caller.timeAgo
            
            binding.root.setOnClickListener {
                onCallerClick(caller)
            }
        }
    }
}
