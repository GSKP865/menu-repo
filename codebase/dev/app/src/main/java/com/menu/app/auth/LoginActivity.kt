package com.menu.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.menu.app.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

// Rule 1: UI handles presentation and user interaction
class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (com.menu.app.BuildConfig.TABLE_TEST_LOGIN) binding.tvSignup.visibility = android.view.View.GONE
        
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupClickListeners() {
        binding.btnGetConnected.setOnClickListener {
            // UI delegates logic to ViewModel
            viewModel.submitMobileNumber(binding.etNickname.text?.toString())
        }
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Rule 7 & 28: Observe UI state 
                launch {
                    viewModel.uiState.collect { state ->
                        binding.btnGetConnected.isEnabled = state !is LoginUiState.Loading
                        binding.btnGetConnected.text = if (com.menu.app.BuildConfig.TABLE_TEST_LOGIN) {
                            if (state is LoginUiState.Loading) "Checking..." else "Continue to test code"
                        } else if (state is LoginUiState.Loading) "Sending..." else "Send OTP"
                    }
                }
                
                // Rule 13: Handle async operations and Side effects
                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is LoginUiEvent.ShowToast -> {
                                Toast.makeText(this@LoginActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is LoginUiEvent.NavigateToOTP -> {
                                navigateToOTPVerification(event.mobileNumber)
                            }
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToOTPVerification(phone: String) {
        val intent = Intent(this, OTPVerificationActivity::class.java)
        intent.putExtra(OTPVerificationActivity.EXTRA_PHONE, phone)
        startActivity(intent)
    }
    
    companion object {
    }
}
