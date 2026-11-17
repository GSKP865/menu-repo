package com.menu.app.admin

import android.content.Context
import com.menu.app.BuildConfig
import com.menu.app.domain.auth.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Demo uses a separate local store. It never reads or writes the live database. */
class AdminRepository(context: Context, val demo: Boolean) {
    private val prefs = context.applicationContext.getSharedPreferences("admin_demo_v1", Context.MODE_PRIVATE)
    init { check(!demo || BuildConfig.DEBUG) { "Demo admin is disabled in release builds." } }
    suspend fun load(section: String, date: String, search: String, status: String, page: Int): JSONObject {
        AdminRules.filterDay(date)
        return if (demo) demoLoad(section, date, search, status, page)
        else rpc("admin_mobile_snapshot", JSONObject().put("p_section", section).put("p_day", date)
            .put("p_search", search.trim()).put("p_status", status).put("p_page", page))
    }
    suspend fun change(kind: String, id: String, decision: String, reason: String) {
        AdminRules.requireReason(reason)
        if (kind == "user") AdminRules.requireStatus(decision)
        if (demo) demoChange(kind, id, decision, reason.trim())
        else {
            val name = when(kind) {
                "user" -> "admin_mobile_user_status"
                "payout" -> "admin_mobile_review_payout"
                "report" -> "admin_mobile_resolve_report"
                else -> error("Unsupported action.")
            }
            rpc(name, JSONObject().put("p_id", id).put("p_decision", decision).put("p_reason", reason.trim()))
        }
    }
    private suspend fun rpc(name: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val token = SessionStore.loadAccessToken() ?: error("Sign in with an authorized admin account first.")
        check(BuildConfig.SUPABASE_URL.isNotBlank()) { "Live backend is not configured. Use the labelled demo." }
        val connection = URL(BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1/rpc/" + name).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                if (status == 401 || status == 403) error("Admin access denied or session expired. Sign in again.")
                val message = runCatching { JSONObject(response).optString("message") }.getOrDefault("")
                error(message.ifBlank { "Request failed. Refresh before retrying an action." })
            }
            if (response.isBlank() || response == "null") JSONObject() else JSONObject(response)
        } finally { connection.disconnect() }
    }
    private fun state(): JSONObject = prefs.getString("state", null)?.let(::JSONObject) ?: seed().also(::save)
    private fun save(state: JSONObject) { prefs.edit().putString("state", state.toString()).apply() }
    private fun rows(array: JSONArray) = (0 until array.length()).map { array.getJSONObject(it) }
    private fun seed(): JSONObject {
        val users = JSONArray()
        listOf("Arjun", "Priya", "Kavya", "Rahul", "Neha", "Ishaan", "Meera", "Dev", "Anika", "Rohan", "Tara", "Ayaan")
            .forEachIndexed { i, name ->
                users.put(JSONObject().put("id", "demo-user-" + (i+1)).put("display_name", name + " (sample)")
                    .put("status", if(i==3) "suspended" else "active").put("phone", "Not a real phone number")
                    .put("available_minor", 10000L + i * 1700L).put("pending_minor", 2000L)
                    .put("frozen_minor", if(i==3) 5000L else 0L).put("earned_minor", 55000L + i*1700L))
            }
        val calls = JSONArray()
        repeat(84) { i ->
            val time = System.currentTimeMillis() - (i % 7) * 86400000L - (i % 8) * 900000L
            calls.put(JSONObject().put("id", "demo-call-" + (i+1)).put("caller_id", "demo-user-" + (i%12+1))
                .put("receiver_id", "demo-user-" + ((i+3)%12+1)).put("day", AdminRules.day(time))
                .put("status", if(i%9==0) "failed" else "completed").put("duration_seconds", if(i%9==0) 0 else 180+i*5))
        }
        val payouts = JSONArray()
        repeat(4) { i -> payouts.put(JSONObject().put("id","demo-payout-" + (i+1)).put("user_id","demo-user-" + (i+1))
            .put("amount_minor",5000L).put("status","pending_review").put("requested_at",AdminRules.day(System.currentTimeMillis()))) }
        val reports = JSONArray().put(JSONObject().put("id","demo-report-1").put("user_id","demo-user-4")
            .put("category","Harassment").put("description","Sample report for testing moderation.").put("status","open"))
        return JSONObject().put("users",users).put("calls",calls).put("payouts",payouts).put("reports",reports).put("audit",JSONArray())
    }
    private fun demoLoad(section: String, date: String, search: String, status: String, page: Int): JSONObject {
        val data = state()
        val daily = rows(data.getJSONArray("calls")).filter { it.getString("day") == date }
        val users = rows(data.getJSONArray("users"))
        val payouts = rows(data.getJSONArray("payouts"))
        users.forEach { user ->
            val id = user.getString("id")
            val own = daily.filter { it.getString("caller_id")==id || it.getString("receiver_id")==id }
            user.put("calls_day",own.size).put("outgoing_day",own.count { it.getString("caller_id")==id })
                .put("incoming_day",own.count { it.getString("receiver_id")==id })
                .put("completed_day",own.count { it.getString("status")=="completed" })
                .put("duration_day",own.sumOf { it.getLong("duration_seconds") })
        }
        val items = when(section) {
            "users" -> users
            "calls" -> daily
            "payouts" -> payouts
            "reports" -> rows(data.getJSONArray("reports"))
            "audit" -> rows(data.getJSONArray("audit")).reversed()
            else -> emptyList()
        }.filter { (status == "all" || it.optString("status")==status) &&
            (search.isBlank() || it.toString().contains(search.trim(), ignoreCase=true)) }
        val metrics = JSONObject().put("users",users.size).put("blocked",users.count { it.getString("status")=="blocked" })
            .put("calls_day",daily.size).put("completed_day",daily.count { it.getString("status")=="completed" })
            .put("failed_day",daily.count { it.getString("status") in setOf("failed","dropped") })
            .put("duration_day",daily.sumOf { it.getLong("duration_seconds") })
            .put("available_minor",users.sumOf { it.getLong("available_minor") })
            .put("earned_minor",users.sumOf { it.getLong("earned_minor") })
            .put("pending_payout_minor",payouts.filter { it.getString("status")=="pending_review" }.sumOf { it.getLong("amount_minor") })
            .put("open_reports",rows(data.getJSONArray("reports")).count { it.getString("status")=="open" })
        return JSONObject().put("metrics",metrics).put("items",JSONArray(items.drop(page*20).take(20)))
            .put("total",items.size).put("actor","Demo Admin · admin-demo-001")
    }
    private fun demoChange(kind: String, id: String, decision: String, reason: String) {
        val data = state()
        val key = when(kind) { "user" -> "users"; "payout" -> "payouts"; "report" -> "reports"; else -> error("Invalid action.") }
        val row = rows(data.getJSONArray(key)).firstOrNull { it.getString("id")==id } ?: error("Record not found.")
        val before = row.getString("status")
        when(kind) {
            "user" -> { require(before != decision) { "User already has that status." }; row.put("status",decision) }
            "payout" -> {
                require(AdminRules.canReview(before)) { "This withdrawal was already reviewed." }
                require(decision in setOf("approve","reject")) { "Invalid decision." }
                val user = rows(data.getJSONArray("users")).first { it.getString("id")==row.getString("user_id") }
                if(decision=="approve") require(user.getString("status")=="active") { "Restore the user before approving." }
                row.put("status",if(decision=="approve") "approved" else "rejected")
                // Sample requests are already reserved outside the available balance.
                if(decision=="reject") user.put("available_minor",user.getLong("available_minor")+row.getLong("amount_minor"))
            }
            "report" -> { require(before=="open") { "Report already resolved." }; row.put("status","resolved") }
        }
        data.getJSONArray("audit").put(JSONObject().put("id","demo-audit-" + System.nanoTime())
            .put("action",kind + ":" + decision).put("target_id",id).put("reason",reason).put("before",before)
            .put("after",row.getString("status")).put("created_at",java.util.Date().toString()).put("admin_id","admin-demo-001"))
        save(data)
    }
}