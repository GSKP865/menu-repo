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

// Rule 7 & 28: State Management and explicit State Machines
sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

// Single-shot events (Effects)
sealed class LoginUiEvent {
    data class NavigateToOTP(val mobileNumber: String) : LoginUiEvent()
    data class ShowToast(val message: String) : LoginUiEvent()
}

// Rule 1: ViewModel handles Business Logic (Validation)
class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent: SharedFlow<LoginUiEvent> = _uiEvent.asSharedFlow()

    fun submitMobileNumber(number: String?) {
        if (_uiState.value is LoginUiState.Loading) return
        val trimmedNumber = number?.trim()
        
        if (trimmedNumber.isNullOrEmpty()) {
            emitEvent(LoginUiEvent.ShowToast("Please enter your mobile number"))
            return
        }

        if (trimmedNumber == null || !PHONE_PATTERN.matches(trimmedNumber)) {
            emitEvent(LoginUiEvent.ShowToast("Enter a valid 10-digit Indian mobile number"))
            return
        }
        _uiState.value = LoginUiState.Loading
        
        viewModelScope.launch {
            when (val result = AuthServices.repository.requestOtp("+91$trimmedNumber")) {
                is AuthResult.Success -> emitEvent(LoginUiEvent.NavigateToOTP("+91$trimmedNumber"))
                is AuthResult.Failure -> emitEvent(LoginUiEvent.ShowToast(result.message))
            }
            _uiState.value = LoginUiState.Idle
        }
    }

    private fun emitEvent(event: LoginUiEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    private companion object { val PHONE_PATTERN = Regex("^[6-9][0-9]{9}$") }
}
