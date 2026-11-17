package com.menu.app.core.model

enum class AuthState {
    AUTH_UNKNOWN, AUTH_CHECKING, AUTH_LOGGED_OUT, AUTH_OTP_REQUESTED,
    AUTH_OTP_VERIFYING, AUTH_AUTHENTICATED, AUTH_EXPIRED, AUTH_ERROR
}

enum class SignupState {
    SIGNUP_EMPTY, SIGNUP_EDITING, SIGNUP_VALIDATING, SIGNUP_OTP_REQUIRED,
    SIGNUP_CREATING, SIGNUP_COMPLETE, SIGNUP_REJECTED, SIGNUP_ERROR
}

enum class Gender { MALE, FEMALE }

enum class LanguageCode(val label: String) {
    EN("English"), HI("Hindi"), BN("Bengali"), TE("Telugu"), MR("Marathi"),
    TA("Tamil"), GU("Gujarati"), UR("Urdu"), KN("Kannada"), ML("Malayalam"),
    OR("Odia"), PA("Punjabi"), AS("Assamese"), MAI("Maithili"), SA("Sanskrit"),
    KOK("Konkani"), NE("Nepali"), SD("Sindhi");

    companion object {
        fun fromStableCode(code: String?): LanguageCode? = entries.firstOrNull {
            it.name.equals(code, ignoreCase = true)
        }
    }
}

data class UserProfile(
    val id: String,
    val name: String,
    val phone: String,
    val age: Int,
    val gender: Gender,
    val language: LanguageCode,
    val available: Boolean
)
