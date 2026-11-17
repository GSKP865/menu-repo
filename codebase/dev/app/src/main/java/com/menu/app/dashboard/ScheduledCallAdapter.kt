package com.menu.app.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.databinding.ItemScheduledCallBinding

class ScheduledCallAdapter(
    private val calls: List<FemaleDashboardActivity.ScheduledCall>,
    private val onCallClick: (FemaleDashboardActivity.ScheduledCall) -> Unit
) : RecyclerView.Adapter<ScheduledCallAdapter.ScheduledCallViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduledCallViewHolder {
        val binding = ItemScheduledCallBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduledCallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduledCallViewHolder, position: Int) {
        holder.bind(calls[position])
    }

    override fun getItemCount(): Int = calls.size

    inner class ScheduledCallViewHolder(private val binding: ItemScheduledCallBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(call: FemaleDashboardActivity.ScheduledCall) {
            binding.tvName.text = call.name
            binding.tvAge.text = "${call.age} yrs"
            binding.tvScheduledTime.text = call.scheduledTime
            
            binding.btnJoin.setOnClickListener {
                onCallClick(call)
            }
            
            binding.root.setOnClickListener {
                onCallClick(call)
            }
        }
    }
}
