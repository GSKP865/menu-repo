package com.menu.app.auth

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.menu.app.home.HomeActivity
import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import com.menu.app.databinding.ActivityOtpVerificationBinding
import kotlinx.coroutines.launch

// Rule 1: UI handles presentation and user interaction
class OTPVerificationActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOtpVerificationBinding
    private val viewModel: OTPViewModel by viewModels()
    private var phone: String = ""
    private var resendTimer: CountDownTimer? = null
    private lateinit var otpBoxes: List<TextView>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (com.menu.app.BuildConfig.TABLE_TEST_LOGIN) binding.tvHeader.text = "Enter test code (no SMS)"
        
        phone = intent.getStringExtra(EXTRA_PHONE) ?: ""
        
        setupOtpInput()
        setupClickListeners()
        observeViewModel()
        startResendCountdown()
    }
    
    private fun setupClickListeners() {
        binding.tvResendOTP.setOnClickListener {
            if (!binding.tvResendOTP.isEnabled) return@setOnClickListener
            viewModel.resendOTP(phone)
            startResendCountdown()
        }
        
        binding.btnVerify.setOnClickListener {
            val signup = intent.getBooleanExtra(EXTRA_SIGNUP, false)
            if (signup) {
                val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
                val age = intent.getIntExtra(EXTRA_AGE, -1)
                val gender = runCatching { Gender.valueOf(intent.getStringExtra(EXTRA_GENDER).orEmpty()) }.getOrNull()
                val language = LanguageCode.fromStableCode(intent.getStringExtra(EXTRA_LANGUAGE))
                if (name.trim().length < 2 || age !in 16..120 || gender == null || language == null) {
                    Toast.makeText(this, "Your signup details are no longer valid. Please try again.", Toast.LENGTH_SHORT).show()
                } else viewModel.verifySignupOtp(phone, binding.etOtp.text?.toString(), name, age, gender, language)
            } else viewModel.verifyOTP(phone, binding.etOtp.text?.toString())
        }
    }

    private fun setupOtpInput() {
        otpBoxes = listOf(binding.tvOtp1, binding.tvOtp2, binding.tvOtp3, binding.tvOtp4, binding.tvOtp5, binding.tvOtp6)
        binding.otpBoxRow.setOnClickListener {
            binding.etOtp.requestFocus()
            showKeyboard()
        }
        otpBoxes.forEach { box ->
            box.setOnClickListener {
                binding.etOtp.requestFocus()
                showKeyboard()
            }
        }
        binding.etOtp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                renderOtpBoxes(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {
                val digitsOnly = s?.toString()?.filter { it.isDigit() }.orEmpty()
                if (digitsOnly != s?.toString()) {
                    binding.etOtp.setText(digitsOnly.take(OTP_LENGTH))
                    binding.etOtp.setSelection(binding.etOtp.text?.length ?: 0)
                }
            }
        })
        binding.etOtp.requestFocus()
        renderOtpBoxes("")
    }

    private fun renderOtpBoxes(code: String) {
        otpBoxes.forEachIndexed { index, box ->
            box.text = code.getOrNull(index)?.toString().orEmpty()
            box.isSelected = index == code.length.coerceAtMost(OTP_LENGTH - 1)
        }
        binding.btnVerify.isEnabled = code.length == OTP_LENGTH && viewModel.uiState.value !is OTPUiState.Verifying
    }

    private fun startResendCountdown() {
        resendTimer?.cancel()
        binding.tvResendOTP.isEnabled = false
        binding.tvResendOTP.setTextColor(getColor(com.menu.app.R.color.text_secondary))
        resendTimer = object : CountDownTimer(RESEND_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvResendOTP.text = "Resend in ${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                binding.tvResendOTP.text = "Resend OTP"
                binding.tvResendOTP.isEnabled = true
                binding.tvResendOTP.setTextColor(getColor(com.menu.app.R.color.brand_indigo))
            }
        }.start()
    }

    private fun showKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        inputMethodManager.showSoftInput(binding.etOtp, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Rule 7 & 28: Observe UI state 
                launch {
                    viewModel.uiState.collect { state ->
                        binding.btnVerify.isEnabled = state !is OTPUiState.Verifying && binding.etOtp.text?.length == OTP_LENGTH
                        binding.btnVerify.text = if (state is OTPUiState.Verifying) "Verifying..." else "Verify"
                    }
                }
                
                // Rule 13: Handle async operations and Side effects
                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is OTPUiEvent.NavigateToAdmin -> {
                                startActivity(Intent(this@OTPVerificationActivity, com.menu.app.admin.AdminActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                                finish()
                            }
                            is OTPUiEvent.ShowToast -> {
                                Toast.makeText(this@OTPVerificationActivity, event.message, Toast.LENGTH_SHORT).show()
                            }
                            is OTPUiEvent.NavigateToHome -> {
                                navigateToHome()
                            }
                            is OTPUiEvent.NavigateToSignup -> navigateToSignup(event.phone)
                        }
                    }
                }
            }
        }
    }
    
    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun navigateToSignup(phone: String) {
        startActivity(Intent(this, SignupActivity::class.java).putExtra(SignupActivity.EXTRA_PHONE, phone))
        finish()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        super.onDestroy()
    }
    
    companion object {
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_SIGNUP = "extra_signup"
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_AGE = "extra_age"
        const val EXTRA_GENDER = "extra_gender"
        const val EXTRA_LANGUAGE = "extra_language"
        private const val OTP_LENGTH = 6
        private const val RESEND_SECONDS = 30
    }
}
