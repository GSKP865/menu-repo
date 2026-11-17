package com.menu.app.admin
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
class AdminRulesTest {
    @Test fun dateUsesIndiaMidnight() {
        assertEquals("2026-10-10", AdminRules.day(Instant.parse("2026-10-10T18:29:59Z").toEpochMilli()))
        assertEquals("2026-10-11", AdminRules.day(Instant.parse("2026-10-10T18:30:00Z").toEpochMilli()))
    }
    @Test fun moneyRetainsPaise() { assertEquals("INR 123.45", AdminRules.money(12345)); assertEquals("INR 0.01", AdminRules.money(1)) }
    @Test fun calendarRejectsImpossibleOrTrailingDates() {
        for (date in listOf("2026-02-30","2026-10-11junk","2026-1-1")) assertTrue(runCatching { AdminRules.filterDay(date) }.isFailure)
        AdminRules.filterDay("2024-02-29")
    }
    @Test fun onlyPendingPayoutsAreReviewable() {
        assertTrue(AdminRules.canReview("pending_review"))
        for (status in listOf("paid","approved","rejected","processing","failed")) assertFalse(AdminRules.canReview(status))
    }
    @Test fun reasonsAndStatusesAreValidated() {
        for(reason in listOf("", "    ", "abcd", "x".repeat(501))) assertTrue(runCatching { AdminRules.requireReason(reason) }.isFailure)
        AdminRules.requireReason("Verified report")
        assertTrue(runCatching { AdminRules.requireStatus("admin") }.isFailure)
    }
}
