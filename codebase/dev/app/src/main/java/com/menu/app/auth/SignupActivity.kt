package com.menu.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.menu.app.R
import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import com.menu.app.databinding.ActivitySignupBinding
import com.menu.app.domain.auth.AuthResult
import com.menu.app.domain.auth.AuthServices
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(EXTRA_PHONE)?.removePrefix("+91")?.let { binding.etPhone.setText(it) }
        binding.tvLogin.setOnClickListener { finish() }
        binding.btnCreate.setOnClickListener { submit() }
    }

    private fun submit() {
        if (!binding.btnCreate.isEnabled) return
        val name = binding.etName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val age = binding.etAge.text.toString().toIntOrNull()
        val gender = when (binding.rgGender.checkedRadioButtonId) {
            R.id.rbMale -> Gender.MALE; R.id.rbFemale -> Gender.FEMALE; else -> null
        }
        val language = LanguageCode.EN
        if (name.length < 2 || !Regex("^[6-9][0-9]{9}$").matches(phone) || age == null || gender == null) {
            toast("Complete each field with valid details."); return
        }
        if (age !in 16..120) {
            toast("You must be between 16 and 120 to join Me'n'U."); return
        }
        lifecycleScope.launch {
            binding.btnCreate.isEnabled = false
            val formattedPhone = "+91$phone"
            when (val result = AuthServices.repository.requestOtp(formattedPhone)) {
                is AuthResult.Success -> {
                    startActivity(Intent(this@SignupActivity, OTPVerificationActivity::class.java).apply {
                        putExtra(OTPVerificationActivity.EXTRA_PHONE, formattedPhone)
                        putExtra(OTPVerificationActivity.EXTRA_SIGNUP, true)
                        putExtra(OTPVerificationActivity.EXTRA_NAME, name)
                        putExtra(OTPVerificationActivity.EXTRA_AGE, age)
                        putExtra(OTPVerificationActivity.EXTRA_GENDER, gender.name)
                        putExtra(OTPVerificationActivity.EXTRA_LANGUAGE, language.name)
                    })
                }
                is AuthResult.Failure -> toast(result.message)
            }
            binding.btnCreate.isEnabled = true
        }
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object { const val EXTRA_PHONE = "extra_phone" }
}
