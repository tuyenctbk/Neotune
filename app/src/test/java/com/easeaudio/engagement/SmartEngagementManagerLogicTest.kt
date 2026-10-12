package com.easeaudio.engagement

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartEngagementManagerLogicTest {

    @Test
    fun testRatingNotPromptedIfAlreadyRated() {
        val shouldPrompt = EngagementPolicy.shouldPromptRating(
            hasRatedApp = true,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 10,
            sessionCount = 10,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should never prompt rating if user already rated", shouldPrompt)
    }

    @Test
    fun testRatingNotPromptedIfDismissedTwice() {
        val shouldPrompt = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 2,
            daysSinceLastRate = 30,
            daysSinceAnyPrompt = 30,
            daysSinceFirstLaunch = 10,
            sessionCount = 10,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should never prompt rating if user dismissed 2 times", shouldPrompt)
    }

    @Test
    fun testRatingNotPromptedWithinGlobalCooldown() {
        val shouldPrompt = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = 3, // only 3 days since another prompt
            daysSinceFirstLaunch = 10,
            sessionCount = 10,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should not prompt rating within 7-day global cooldown", shouldPrompt)
    }

    @Test
    fun testRatingNotPromptedWithinRateCooldown() {
        val shouldPrompt = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 1,
            daysSinceLastRate = 14, // only 14 days, needs 21
            daysSinceAnyPrompt = 14,
            daysSinceFirstLaunch = 20,
            sessionCount = 15,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should not prompt rating within 21-day rate cooldown", shouldPrompt)
    }

    @Test
    fun testRatingNotPromptedIfInsufficientExperience() {
        // Less than 3 days
        val tooEarlyDays = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 1, // only 1 day
            sessionCount = 5,
            listeningTimeMinutes = 45,
            favoritesCount = 2
        )
        assertFalse("Should not prompt if installed for less than 3 days", tooEarlyDays)

        // Less than 4 sessions
        val tooFewSessions = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 5,
            sessionCount = 2, // only 2 sessions
            listeningTimeMinutes = 45,
            favoritesCount = 2
        )
        assertFalse("Should not prompt if fewer than 4 sessions", tooFewSessions)

        // Insufficient listening or favorites
        val lowEngagement = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 5,
            sessionCount = 5,
            listeningTimeMinutes = 5, // only 5 min
            favoritesCount = 0 // 0 favorites
        )
        assertFalse("Should not prompt if listening < 30min and favorites < 2", lowEngagement)
    }

    @Test
    fun testRatingPromptedWhenCriteriaSatisfied() {
        val shouldPromptWithListening = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 0,
            daysSinceLastRate = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 4,
            sessionCount = 5,
            listeningTimeMinutes = 35,
            favoritesCount = 0
        )
        assertTrue("Should prompt rating when user listened >= 30 min and days/sessions met", shouldPromptWithListening)

        val shouldPromptWithFavorites = EngagementPolicy.shouldPromptRating(
            hasRatedApp = false,
            rateDismissCount = 1,
            daysSinceLastRate = 25, // passed 21 days
            daysSinceAnyPrompt = 25, // passed 7 days
            daysSinceFirstLaunch = 7,
            sessionCount = 6,
            listeningTimeMinutes = 10,
            favoritesCount = 2
        )
        assertTrue("Should prompt rating when user added >= 2 favorites and days/sessions met", shouldPromptWithFavorites)
    }

    @Test
    fun testShareNotPromptedIfAlreadyShared() {
        val shouldPrompt = EngagementPolicy.shouldPromptShare(
            hasSharedApp = true,
            shareDismissCount = 0,
            daysSinceLastShare = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 10,
            sessionCount = 10,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should never prompt share if user already shared", shouldPrompt)
    }

    @Test
    fun testShareNotPromptedIfDismissedTwice() {
        val shouldPrompt = EngagementPolicy.shouldPromptShare(
            hasSharedApp = false,
            shareDismissCount = 2,
            daysSinceLastShare = 30,
            daysSinceAnyPrompt = 30,
            daysSinceFirstLaunch = 10,
            sessionCount = 10,
            listeningTimeMinutes = 120,
            favoritesCount = 5
        )
        assertFalse("Should never prompt share if user dismissed 2 times", shouldPrompt)
    }

    @Test
    fun testShareRequiresHigherLoyalty() {
        // Only 3 days installed (needs 5)
        val notEnoughDays = EngagementPolicy.shouldPromptShare(
            hasSharedApp = false,
            shareDismissCount = 0,
            daysSinceLastShare = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 3,
            sessionCount = 8,
            listeningTimeMinutes = 80,
            favoritesCount = 4
        )
        assertFalse("Should not prompt share with < 5 days installed", notEnoughDays)

        // Only 4 sessions (needs 6)
        val notEnoughSessions = EngagementPolicy.shouldPromptShare(
            hasSharedApp = false,
            shareDismissCount = 0,
            daysSinceLastShare = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 7,
            sessionCount = 4,
            listeningTimeMinutes = 80,
            favoritesCount = 4
        )
        assertFalse("Should not prompt share with < 6 sessions", notEnoughSessions)

        // Eligible user
        val eligible = EngagementPolicy.shouldPromptShare(
            hasSharedApp = false,
            shareDismissCount = 0,
            daysSinceLastShare = null,
            daysSinceAnyPrompt = null,
            daysSinceFirstLaunch = 6,
            sessionCount = 7,
            listeningTimeMinutes = 65,
            favoritesCount = 1
        )
        assertTrue("Should prompt share for loyal user (>= 5 days, >= 6 sessions, >= 60 min)", eligible)
    }

    @Test
    fun testOptionalUpdateInSessionLimit() {
        val shouldPrompt = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = true,
            isForceUpdate = false,
            isPromptShownThisSession = true, // already showed a prompt this session
            targetVersionCode = 200063,
            lastDismissedVersionCode = 0,
            dismissCountForVersion = 0,
            daysSinceLastUpdate = null,
            daysSinceAnyPrompt = null
        )
        assertFalse("Should not prompt optional update if prompt was already shown this session", shouldPrompt)
    }

    @Test
    fun testOptionalUpdateDismissLimitForSameVersion() {
        val shouldPromptSameVersion = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = true,
            isForceUpdate = false,
            isPromptShownThisSession = false,
            targetVersionCode = 200063,
            lastDismissedVersionCode = 200063,
            dismissCountForVersion = 2, // dismissed 2 times
            daysSinceLastUpdate = 10,
            daysSinceAnyPrompt = 10
        )
        assertFalse("Should not prompt optional update if dismissed 2 times for this version", shouldPromptSameVersion)

        // A brand new version comes out
        val shouldPromptNewVersion = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = true,
            isForceUpdate = false,
            isPromptShownThisSession = false,
            targetVersionCode = 200064, // new version!
            lastDismissedVersionCode = 200063,
            dismissCountForVersion = 2,
            daysSinceLastUpdate = 10,
            daysSinceAnyPrompt = 10
        )
        assertTrue("Should prompt optional update for newly released version", shouldPromptNewVersion)
    }

    @Test
    fun testOptionalUpdateCooldowns() {
        // Within 7-day update cooldown
        val withinUpdateCooldown = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = true,
            isForceUpdate = false,
            isPromptShownThisSession = false,
            targetVersionCode = 200063,
            lastDismissedVersionCode = 0,
            dismissCountForVersion = 0,
            daysSinceLastUpdate = 4, // only 4 days
            daysSinceAnyPrompt = 4
        )
        assertFalse("Should not prompt optional update within 7 days of last update prompt", withinUpdateCooldown)

        // Within 7-day global cooldown
        val withinGlobalCooldown = EngagementPolicy.shouldPromptOptionalUpdate(
            isUpdateAvailable = true,
            isForceUpdate = false,
            isPromptShownThisSession = false,
            targetVersionCode = 200063,
            lastDismissedVersionCode = 0,
            dismissCountForVersion = 0,
            daysSinceLastUpdate = null,
            daysSinceAnyPrompt = 2 // e.g. rated 2 days ago
        )
        assertFalse("Should not prompt optional update within 7 days of any other prompt", withinGlobalCooldown)
    }
}
