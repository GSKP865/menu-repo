package com.menu.app.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.menu.app.R
import com.menu.app.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val languages: List<LanguageSelectionActivity.Language>,
    private val onLanguageSelected: (LanguageSelectionActivity.Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val binding = ItemLanguageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: LanguageSelectionActivity.Language) {
            binding.tvLanguageName.text = language.name
            
            if (language.isSelected) {
                binding.ivCheck.visibility = android.view.View.VISIBLE
                binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.purple_light))
            } else {
                binding.ivCheck.visibility = android.view.View.GONE
                binding.root.setCardBackgroundColor(binding.root.context.getColor(R.color.background_secondary))
            }
            
            binding.root.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }
}
