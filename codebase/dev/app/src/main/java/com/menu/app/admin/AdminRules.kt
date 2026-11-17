package com.menu.app.admin

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object AdminRules {
    val zone: TimeZone get() = TimeZone.getTimeZone("Asia/Kolkata")
    fun day(time: Long): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = zone }.format(Date(time))
    fun money(minor: Long): String = "INR " + BigDecimal.valueOf(minor, 2).toPlainString()
    fun requireReason(reason: String) {
        require(reason.trim().length in 5..500) { "Enter a reason between 5 and 500 characters." }
    }
    fun canReview(status: String) = status == "pending_review"
    fun requireStatus(status: String) {
        require(status in setOf("active", "suspended", "blocked")) { "Invalid user status." }
    }
    fun filterDay(value: String) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false; timeZone = zone }
        require(runCatching { parser.parse(value)?.let { day(it.time) == value } }.getOrDefault(false) == true) { "Choose a valid date." }
    }
}