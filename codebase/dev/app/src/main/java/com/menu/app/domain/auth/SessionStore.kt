package com.menu.app.domain.auth

import android.content.Context
import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import com.menu.app.core.model.UserProfile

/** Stores the local session snapshot. Move tokens to EncryptedSharedPreferences before production release. */
object SessionStore {
    private const val FILE = "auth_session"
    private lateinit var context: Context

    fun initialize(applicationContext: Context) { context = applicationContext }
    fun save(profile: UserProfile, accessToken: String? = null, refreshToken: String? = null) {
        if (!::context.isInitialized) return
        val editor = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("id", profile.id).putString("name", profile.name).putString("phone", profile.phone)
            .putInt("age", profile.age).putString("gender", profile.gender.name).putString("language", profile.language.name)
            .putBoolean("available", profile.available)
        if (accessToken != null) editor.putString("access_token", accessToken)
        if (refreshToken != null) editor.putString("refresh_token", refreshToken)
        editor.apply()
    }
    fun load(): UserProfile? {
        if (!::context.isInitialized) return null
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val id = p.getString("id", null) ?: return null
        val gender = runCatching { Gender.valueOf(p.getString("gender", "") ?: "") }.getOrNull() ?: return null
        val language = LanguageCode.fromStableCode(p.getString("language", null)) ?: return null
        val name = p.getString("name", "") ?: return null
        val phone = p.getString("phone", "") ?: return null
        val age = p.getInt("age", 0)
        if (name.length < 2 || !phone.matches(Regex("^\\+91[6-9][0-9]{9}$")) || age !in 16..120) { clear(); return null }
        return UserProfile(id, name, phone, age, gender, language, p.getBoolean("available", false))
    }
    fun saveTokens(accessToken: String, refreshToken: String?, authUserId: String) {
        if (!::context.isInitialized) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .putString("auth_user_id", authUserId)
            .apply()
    }
    fun loadAccessToken(): String? {
        if (!::context.isInitialized) return null
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("access_token", null)
    }
    fun loadRefreshToken(): String? {
        if (!::context.isInitialized) return null
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("refresh_token", null)
    }
    fun loadAuthUserId(): String? {
        if (!::context.isInitialized) return null
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return p.getString("auth_user_id", null) ?: p.getString("id", null)
    }
    fun clear() { if (::context.isInitialized) context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply() }
}
