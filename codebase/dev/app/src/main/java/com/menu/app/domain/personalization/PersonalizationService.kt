package com.menu.app.domain.personalization

import com.menu.app.core.model.Gender
import com.menu.app.core.model.UserProfile

data class HomeContent(val greeting: String, val headline: String, val actionLabel: String)

/** Keeps role-dependent copy out of layouts and activities. */
object PersonalizationService {
    fun resolve(profile: UserProfile): HomeContent = when (profile.gender) {
        Gender.MALE -> HomeContent("Good evening, ${profile.name.substringBefore(' ')}", "Find someone to talk to.", "Start a conversation")
        Gender.FEMALE -> HomeContent("Good evening, ${profile.name.substringBefore(' ')}", "You are online and ready to connect.", "See people waiting")
    }
}
