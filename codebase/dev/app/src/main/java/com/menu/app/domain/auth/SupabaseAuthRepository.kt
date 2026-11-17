package com.menu.app.domain.auth

import com.menu.app.core.model.Gender
import com.menu.app.core.model.LanguageCode
import com.menu.app.core.model.UserProfile
import com.menu.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SupabaseAuthRepository(
    private val supabaseUrl: String,
    private val publishableKey: String,
    private val otpChannel: String
) : AuthRepository {
    private var session: UserProfile? = null

    override suspend fun requestOtp(phone: String): AuthResult = withContext(Dispatchers.IO) { authRequest {
        if (BuildConfig.TABLE_TEST_LOGIN) {
            val response = request("/functions/v1/test-login", "POST",
                JSONObject().put("phone", phone).put("action", "request"), null)
            return@authRequest if (response.isSuccessful) AuthResult.Success() else AuthResult.Failure(response.message)
        }
        val body = JSONObject()
            .put("phone", phone)
            .put("channel", otpChannel.lowercase())
            .put("create_user", true)
        val response = request("/auth/v1/otp", method = "POST", body = body, bearer = null)
        if (response.isSuccessful) AuthResult.Success() else AuthResult.Failure(response.message)
    } }

    override suspend fun verifyOtp(phone: String, code: String): AuthResult = withContext(Dispatchers.IO) { authRequest {
        val body = JSONObject()
            .put("phone", phone)
            .put("token", code)
            .put("type", "sms")
        val response = if (BuildConfig.TABLE_TEST_LOGIN) request("/functions/v1/test-login", "POST",
            JSONObject().put("phone", phone).put("code", code).put("action", "verify"), null)
        else request("/auth/v1/verify", method = "POST", body = body, bearer = null)
        if (!response.isSuccessful || response.body.isBlank()) return@authRequest AuthResult.Failure(response.message)

        val json = JSONObject(response.body)
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token").takeIf { it.isNotBlank() }
        val userId = json.optJSONObject("user")?.optString("id").orEmpty()
        if (accessToken.isBlank() || userId.isBlank()) {
            return@authRequest AuthResult.Failure("We could not verify this session. Please request a new OTP.")
        }

        val adminResponse = request("/rest/v1/rpc/is_admin", "POST", JSONObject(), accessToken)
        if (!adminResponse.isSuccessful) return@authRequest AuthResult.Failure(adminResponse.message)
        val isAdmin = adminResponse.body.trim() == "true"
        val profile = if (isAdmin) null else fetchProfile(userId, accessToken)
        session = null
        SessionStore.clear()
        SessionStore.saveTokens(accessToken, refreshToken, userId)
        if (profile != null) {
            session = profile
            SessionStore.save(profile, accessToken, refreshToken)
        }
        AuthResult.Success(profile, isAdmin)
    } }

    override suspend fun createAccount(
        name: String,
        phone: String,
        age: Int,
        gender: Gender,
        language: LanguageCode
    ): AuthResult = withContext(Dispatchers.IO) { authRequest {
        if (BuildConfig.TABLE_TEST_LOGIN) return@authRequest AuthResult.Failure("This test build uses pre-created accounts only.")
        if (name.trim().length < MINIMUM_NAME_LENGTH || !PHONE_PATTERN.matches(phone)) {
            return@authRequest AuthResult.Failure("We could not complete that registration. Check your details and try again.")
        }
        if (age !in MINIMUM_AGE..MAXIMUM_AGE) {
            return@authRequest AuthResult.Failure("You must be between 16 and 120 to join Me'n'U.")
        }

        val accessToken = SessionStore.loadAccessToken()
            ?: return@authRequest AuthResult.Failure("Please verify your OTP again before creating a profile.")
        val userId = SessionStore.loadAuthUserId()
            ?: return@authRequest AuthResult.Failure("Please verify your OTP again before creating a profile.")

        val profileJson = JSONObject()
            .put("id", userId)
            .put("display_name", name.trim())
            .put("phone", phone)
            .put("gender", gender.name.lowercase())
            .put("date_of_birth", "${currentYear() - age}-01-01")
            .put("primary_language", language.name.lowercase())
            .put("available_for_calls", true)

        val response = request(
            path = "/rest/v1/profiles",
            method = "POST",
            body = profileJson,
            bearer = accessToken,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=representation")
        )
        if (!response.isSuccessful) return@authRequest AuthResult.Failure(response.message)

        val profile = parseProfileArray(response.body).firstOrNull()
            ?: fetchProfile(userId, accessToken)
            ?: UserProfile(userId, name.trim(), phone, age, gender, language, true)
        session = profile
        SessionStore.save(profile)
        AuthResult.Success(profile)
    } }

    override fun currentSession(): UserProfile? = session ?: SessionStore.load()

    override fun logout() {
        session = null
        SessionStore.clear()
    }

    private fun fetchProfile(userId: String, accessToken: String): UserProfile? {
        val response = request(
            path = "/rest/v1/profiles?id=eq.$userId&select=id,display_name,phone,gender,date_of_birth,primary_language,available_for_calls",
            method = "GET",
            body = null,
            bearer = accessToken
        )
        if (!response.isSuccessful) throw java.io.IOException(response.message)
        return parseProfileArray(response.body).firstOrNull()
    }

    private fun parseProfileArray(body: String): List<UserProfile> {
        if (body.isBlank()) return emptyList()
        val array = JSONArray(body)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("display_name")
                val phone = item.optString("phone")
                val gender = runCatching { Gender.valueOf(item.optString("gender").uppercase()) }.getOrNull()
                val language = LanguageCode.fromStableCode(item.optString("primary_language"))
                val age = ageFromDateOfBirth(item.optString("date_of_birth"))
                if (id.isNotBlank() && name.isNotBlank() && phone.isNotBlank() && gender != null && language != null && age != null) {
                    add(UserProfile(id, name, phone, age, gender, language, item.optBoolean("available_for_calls", false)))
                }
            }
        }
    }

    private fun ageFromDateOfBirth(dateOfBirth: String): Int? {
        val birthYear = dateOfBirth.take(4).toIntOrNull() ?: return null
        return (currentYear() - birthYear).takeIf { it in MINIMUM_AGE..MAXIMUM_AGE }
    }

    private fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    private fun request(
        path: String,
        method: String,
        body: JSONObject?,
        bearer: String?,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponse {
        val connection = (URL("$supabaseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("apikey", publishableKey)
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            if (bearer != null) setRequestProperty("Authorization", "Bearer $bearer")
            extraHeaders.forEach { (key, value) -> setRequestProperty(key, value) }
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream).use { it.write(body.toString()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val message = parseErrorMessage(responseBody).ifBlank {
                if (status in 200..299) "OK" else "Request failed with status $status."
            }
            return HttpResponse(status, responseBody, message)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseErrorMessage(body: String): String {
        if (body.isBlank()) return ""
        return runCatching {
            val json = JSONObject(body)
            json.optString("msg")
                .ifBlank { json.optString("message") }
                .ifBlank { json.optString("error_description") }
                .ifBlank { json.optString("error") }
        }.getOrDefault("")
    }

    private data class HttpResponse(val status: Int, val body: String, val message: String) {
        val isSuccessful: Boolean = status in 200..299
    }

    private companion object {
        const val MINIMUM_AGE = 16
        private const val MAXIMUM_AGE = 120
        private const val MINIMUM_NAME_LENGTH = 2
        private const val TIMEOUT_MS = 15_000
        private val PHONE_PATTERN = Regex("^\\+91[6-9][0-9]{9}$")
    }
}
