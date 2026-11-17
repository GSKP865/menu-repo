package com.menu.app.auth

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.menu.app.databinding.ActivityLanguageSelectionBinding

class LanguageSelectionActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLanguageSelectionBinding
    private lateinit var recommendedAdapter: LanguageChipAdapter
    private lateinit var allLanguagesAdapter: LanguageAdapter
    
    private val recommendedLanguages = mutableListOf(
        Language("English", true),
        Language("Hindi", false),
        Language("Español", false),
        Language("Français", false),
        Language("Deutsch", false)
    )
    
    private val allLanguageOptions = listOf(
        Language("English", true),
        Language("Hindi", false),
        Language("Español", false),
        Language("Français", false),
        Language("Deutsch", false),
        Language("Italiano", false),
        Language("Português", false),
        Language("Русский", false),
        Language("日本語", false),
        Language("한국어", false),
        Language("中文", false),
        Language("العربية", false)
    )
    private val allLanguages = allLanguageOptions.map { it.copy() }.toMutableList()
    private var selectedLanguage = "English"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerViews()
        setupClickListeners()
        setupSearch()
    }
    
    private fun setupRecyclerViews() {
        // Recommended languages (horizontal chips)
        recommendedAdapter = LanguageChipAdapter(recommendedLanguages) { language ->
            selectLanguage(language)
        }
        binding.rvRecommendedLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageSelectionActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = recommendedAdapter
        }
        
        // All languages (vertical list)
        allLanguagesAdapter = LanguageAdapter(allLanguages) { language ->
            selectLanguage(language)
        }
        binding.rvAllLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageSelectionActivity)
            adapter = allLanguagesAdapter
        }
    }
    
    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvClose.setOnClickListener { finish() }
        binding.btnConfirmLanguage.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra(EXTRA_SELECTED_LANGUAGE, selectedLanguage)
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun setupSearch() {
        binding.etLanguageSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLanguages(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
    
    private fun selectLanguage(language: Language) {
        selectedLanguage = language.name
        recommendedLanguages.forEach { it.isSelected = it.name == selectedLanguage }
        allLanguageOptions.forEach { it.isSelected = it.name == selectedLanguage }
        allLanguages.forEach { it.isSelected = it.name == selectedLanguage }
        
        recommendedAdapter.notifyDataSetChanged()
        allLanguagesAdapter.notifyDataSetChanged()
    }

    private fun filterLanguages(query: String) {
        allLanguages.clear()
        allLanguages.addAll(
            allLanguageOptions
                .filter { it.name.contains(query, ignoreCase = true) }
                .map { it.copy(isSelected = it.name == selectedLanguage) }
        )
        allLanguagesAdapter.notifyDataSetChanged()
    }
    
    data class Language(val name: String, var isSelected: Boolean)

    companion object {
        const val EXTRA_SELECTED_LANGUAGE = "extra_selected_language"
    }
}
