package com.menu.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.menu.app.domain.auth.AuthResult
import com.menu.app.domain.auth.AuthServices
import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode

sealed class OTPUiState {
    object Idle : OTPUiState()
    object Verifying : OTPUiState()
}

sealed class OTPUiEvent {
    object NavigateToHome : OTPUiEvent()
    object NavigateToAdmin : OTPUiEvent()
    data class NavigateToSignup(val phone: String) : OTPUiEvent()
    data class ShowToast(val message: String) : OTPUiEvent()
}

class OTPViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<OTPUiState>(OTPUiState.Idle)
    val uiState: StateFlow<OTPUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<OTPUiEvent>()
    val uiEvent: SharedFlow<OTPUiEvent> = _uiEvent.asSharedFlow()

    fun verifyOTP(phone: String, otpCode: String?) {
        if (_uiState.value is OTPUiState.Verifying) return
        val code = otpCode?.trim()

        if (code == null || !code.matches(Regex("[0-9]{6}"))) {
            emitEvent(OTPUiEvent.ShowToast("Please enter a valid 6-digit OTP"))
            return
        }

        _uiState.value = OTPUiState.Verifying
        
        viewModelScope.launch {
            when (val result = AuthServices.repository.verifyOtp(phone, code)) {
                is AuthResult.Success -> when {
                    result.isAdmin -> emitEvent(OTPUiEvent.NavigateToAdmin)
                    result.profile == null -> emitEvent(OTPUiEvent.NavigateToSignup(phone))
                    else -> emitEvent(OTPUiEvent.NavigateToHome)
                }
                is AuthResult.Failure -> emitEvent(OTPUiEvent.ShowToast(result.message))
            }
            _uiState.value = OTPUiState.Idle
        }
    }

    fun resendOTP(phone: String) {
        viewModelScope.launch {
            when (val result = AuthServices.repository.requestOtp(phone)) {
                is AuthResult.Success -> emitEvent(OTPUiEvent.ShowToast("A new code is ready."))
                is AuthResult.Failure -> emitEvent(OTPUiEvent.ShowToast(result.message))
            }
        }
    }

    fun verifySignupOtp(phone: String, code: String?, name: String, age: Int, gender: Gender, language: LanguageCode) {
        if (_uiState.value is OTPUiState.Verifying) return
        val trimmedCode = code?.trim()
        if (trimmedCode == null || !trimmedCode.matches(Regex("[0-9]{6}"))) {
            emitEvent(OTPUiEvent.ShowToast("Please enter a valid 6-digit OTP")); return
        }
        _uiState.value = OTPUiState.Verifying
        viewModelScope.launch {
            val verified = AuthServices.repository.verifyOtp(phone, trimmedCode)
            val result = if (verified is AuthResult.Success && verified.profile == null) AuthServices.repository.createAccount(name, phone, age, gender, language) else verified
            when (result) {
                is AuthResult.Success -> emitEvent(OTPUiEvent.NavigateToHome)
                is AuthResult.Failure -> emitEvent(OTPUiEvent.ShowToast(result.message))
            }
            _uiState.value = OTPUiState.Idle
        }
    }

    private fun emitEvent(event: OTPUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }
}
