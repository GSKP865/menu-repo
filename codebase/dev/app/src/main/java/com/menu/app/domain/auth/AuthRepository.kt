package com.menu.app.domain.auth

import com.menu.app.BuildConfig
import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import com.menu.app.core.model.UserProfile

sealed interface AuthResult {
    data class Success(val profile: UserProfile? = null, val isAdmin: Boolean = false) : AuthResult
    data class Failure(val message: String) : AuthResult
}

interface AuthRepository {
    suspend fun requestOtp(phone: String): AuthResult
    suspend fun verifyOtp(phone: String, code: String): AuthResult
    suspend fun createAccount(name: String, phone: String, age: Int, gender: Gender, language: LanguageCode): AuthResult
    fun currentSession(): UserProfile?
    fun logout()
}

/** Development fixture only. Production wiring must use the approved server API. */
class DevelopmentAuthRepository : AuthRepository {
    private data class OtpChallenge(val issuedAtMillis: Long, var attempts: Int = 0)
    private val users = linkedMapOf(
        "+919900000001" to UserProfile("USER-001", "Arjun Test", "+919900000001", 25, Gender.MALE, LanguageCode.HI, true),
        "+919900000002" to UserProfile("USER-002", "Priya Test", "+919900000002", 24, Gender.FEMALE, LanguageCode.EN, true),
        "+919900000003" to UserProfile("USER-003", "Kavya Test", "+919900000003", 27, Gender.FEMALE, LanguageCode.KN, true),
        "+919900000004" to UserProfile("USER-004", "Rahul Test", "+919900000004", 29, Gender.MALE, LanguageCode.TA, false)
    )
    private val challenges = mutableMapOf<String, OtpChallenge>()
    private var session: UserProfile? = null

    init { check(BuildConfig.DEBUG) { "Development authentication must never run in release." } }

    override suspend fun requestOtp(phone: String): AuthResult {
        val existing = challenges[phone]
        if (existing != null && System.currentTimeMillis() - existing.issuedAtMillis < RESEND_COOLDOWN_MS) {
            return AuthResult.Failure("Please wait before requesting another code.")
        }
        challenges[phone] = OtpChallenge(System.currentTimeMillis())
        return AuthResult.Success()
    }

    override suspend fun verifyOtp(phone: String, code: String): AuthResult {
        val challenge = challenges[phone] ?: return AuthResult.Failure("Request a new code and try again.")
        if (System.currentTimeMillis() - challenge.issuedAtMillis > OTP_TTL_MS) return AuthResult.Failure("This code has expired.")
        if (++challenge.attempts > MAX_ATTEMPTS) return AuthResult.Failure("Too many attempts. Request a new code.")
        if (code != DEV_OTP) return AuthResult.Failure("That code is incorrect.")
        challenges.remove(phone)
        // A verified, unregistered number may proceed to the explicit signup flow.
        val profile = users[phone] ?: return AuthResult.Success()
        session = profile
        SessionStore.save(profile)
        return AuthResult.Success(profile)
    }

    override suspend fun createAccount(name: String, phone: String, age: Int, gender: Gender, language: LanguageCode): AuthResult {
        if (name.trim().length < MINIMUM_NAME_LENGTH || !PHONE_PATTERN.matches(phone)) return AuthResult.Failure("We could not complete that registration. Check your details and try again.")
        if (age !in MINIMUM_AGE..MAXIMUM_AGE) return AuthResult.Failure("You must be between 16 and 120 to join Me'n'U.")
        if (users.containsKey(phone)) return AuthResult.Failure("We could not complete that registration. Try signing in instead.")
        val profile = UserProfile("USER-DEV-${users.size + 1}", name.trim(), phone, age, gender, language, true)
        users[phone] = profile
        session = profile
        SessionStore.save(profile)
        return AuthResult.Success(profile)
    }

    override fun currentSession(): UserProfile? = session ?: SessionStore.load()
    override fun logout() { session = null; SessionStore.clear() }

    companion object {
        const val DEV_OTP = "123456"
        const val MINIMUM_AGE = 16
        private const val MAXIMUM_AGE = 120
        private const val MINIMUM_NAME_LENGTH = 2
        private val PHONE_PATTERN = Regex("^\\+91[6-9][0-9]{9}$")
        private const val OTP_TTL_MS = 2 * 60 * 1000L
        private const val RESEND_COOLDOWN_MS = 30 * 1000L
        private const val MAX_ATTEMPTS = 5
    }
}

object AuthServices {
    fun initialize(context: android.content.Context) {
        SessionStore.initialize(context.applicationContext)
        if (SessionStore.loadAccessToken() == null) SessionStore.clear()
    }
    val repository: AuthRepository by lazy {
        val supabaseUrl = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
        val supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()
        when {
            supabaseUrl.isNotBlank() && supabaseKey.isNotBlank() -> SupabaseAuthRepository(
                supabaseUrl = supabaseUrl,
                publishableKey = supabaseKey,
                otpChannel = BuildConfig.SUPABASE_AUTH_CHANNEL.ifBlank { "whatsapp" }
            )
            else -> MissingAuthConfigurationRepository()
        }
    }
}

private class MissingAuthConfigurationRepository : AuthRepository {
    override suspend fun requestOtp(phone: String): AuthResult = failure()
    override suspend fun verifyOtp(phone: String, code: String): AuthResult = failure()
    override suspend fun createAccount(name: String, phone: String, age: Int, gender: Gender, language: LanguageCode): AuthResult = failure()
    override fun currentSession(): UserProfile? = null
    override fun logout() = Unit

    private fun failure() = AuthResult.Failure("Authentication is not configured for this build.")
}
