package com.menu.app.calling

import com.menu.app.BuildConfig
import com.menu.app.domain.auth.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException

class VoiceApi {
    private val mutex = Mutex()
    suspend fun action(action: String, target: String? = null, call: String? = null): JSONObject = request(
        "/rest/v1/rpc/voice_action", JSONObject().put("p_action", action)
            .put("p_target", target ?: JSONObject.NULL).put("p_call", call ?: JSONObject.NULL))
    suspend fun token(call: String): JSONObject = request("/functions/v1/agora-token", JSONObject().put("call_id", call))

    private suspend fun request(path: String, body: JSONObject): JSONObject = mutex.withLock {
        withContext(Dispatchers.IO) {
            var token = SessionStore.loadAccessToken() ?: throw IOException("Please sign in again.")
            var result = http(path, body, token)
            if (result.first == 401) {
                val refresh = SessionStore.loadRefreshToken() ?: throw IOException("Session expired. Sign in again.")
                val renewed = http("/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refresh), null)
                if (renewed.first !in 200..299) throw IOException("Session expired. Sign in again.")
                token = renewed.second.getString("access_token")
                SessionStore.saveTokens(token, renewed.second.getString("refresh_token"), renewed.second.getJSONObject("user").getString("id"))
                result = http(path, body, token)
            }
            if (result.first !in 200..299) throw IOException(result.second.optString("message", "Call request failed. Please retry."))
            result.second
        }
    }
    private fun http(path: String, body: JSONObject, token: String?): Pair<Int, JSONObject> {
        val connection = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Content-Type", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            connection.doOutput = true
            connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            return status to if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }
}
