package com.menu.app.domain.auth

import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentAuthRepositoryTest {
    @Test fun `underage signup is rejected`() = runBlocking {
        val result = DevelopmentAuthRepository().createAccount("Underage Test", "+919900000005", 15, Gender.FEMALE, LanguageCode.HI)
        assertTrue(result is AuthResult.Failure)
    }

    @Test fun `boundary age signup is allowed`() = runBlocking {
        val result = DevelopmentAuthRepository().createAccount("Boundary Test", "+919900000006", 16, Gender.MALE, LanguageCode.TE)
        assertTrue(result is AuthResult.Success)
    }

    @Test fun `wrong otp is rejected`() = runBlocking {
        val repository = DevelopmentAuthRepository()
        repository.requestOtp("+919900000001")
        assertTrue(repository.verifyOtp("+919900000001", "000000") is AuthResult.Failure)
    }

    @Test fun `a used otp cannot be replayed`() = runBlocking {
        val repository = DevelopmentAuthRepository()
        repository.requestOtp("+919900000001")
        assertTrue(repository.verifyOtp("+919900000001", DevelopmentAuthRepository.DEV_OTP) is AuthResult.Success)
        assertTrue(repository.verifyOtp("+919900000001", DevelopmentAuthRepository.DEV_OTP) is AuthResult.Failure)
    }

    @Test fun `verified unregistered number does not authenticate`() = runBlocking {
        val repository = DevelopmentAuthRepository()
        repository.requestOtp("+919900000009")
        val result = repository.verifyOtp("+919900000009", DevelopmentAuthRepository.DEV_OTP)
        assertTrue(result is AuthResult.Success && result.profile == null)
        assertTrue(repository.currentSession() == null)
    }

    @Test fun `impossible age is rejected by repository`() = runBlocking {
        val result = DevelopmentAuthRepository().createAccount("Test User", "+919900000009", 121, Gender.MALE, LanguageCode.EN)
        assertTrue(result is AuthResult.Failure)
    }
}
