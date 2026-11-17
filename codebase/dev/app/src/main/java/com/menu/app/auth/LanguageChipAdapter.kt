package com.menu.app.auth

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.menu.app.R

class LanguageChipAdapter(
    private val languages: List<LanguageSelectionActivity.Language>,
    private val onLanguageSelected: (LanguageSelectionActivity.Language) -> Unit
) : RecyclerView.Adapter<LanguageChipAdapter.LanguageChipViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageChipViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_chip, parent, false) as Chip
        return LanguageChipViewHolder(chip)
    }

    override fun onBindViewHolder(holder: LanguageChipViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageChipViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(language: LanguageSelectionActivity.Language) {
            chip.text = language.name
            
            if (language.isSelected) {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    chip.context.getColor(R.color.purple_primary)
                )
                chip.setTextColor(chip.context.getColor(R.color.white))
            } else {
                chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    chip.context.getColor(R.color.gray_light)
                )
                chip.setTextColor(chip.context.getColor(R.color.text_primary))
            }
            
            chip.setOnClickListener {
                onLanguageSelected(language)
            }
        }
    }
}
