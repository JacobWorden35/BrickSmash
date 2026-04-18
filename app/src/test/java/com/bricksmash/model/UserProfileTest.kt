/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */

package com.bricksmash.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the UserProfile model. Verifies conversion between
 * the Kotlin data class and the Firestore map representation, including
 * handling of default values for missing fields.
 */
class UserProfileTest {

    @Test
    fun `toMap preserves all profile fields`() {
        val profile = UserProfile(
            uid = "uid_abc",
            displayName = "Bee",
            email = "test@example.com",
            cumulativeScore = 50000L,
            levelsCompleted = 8,
            highestLevelUnlocked = 9
        )
        val map = profile.toMap()

        assertEquals("uid_abc", map["uid"])
        assertEquals("Bee", map["displayName"])
        assertEquals(50000L, map["cumulativeScore"])
        assertEquals(8, map["levelsCompleted"])
        assertEquals(9, map["highestLevelUnlocked"])
    }

    @Test
    fun `fromMap handles Firestore Long-to-Int conversion`() {
        // Firestore stores integers as Long, so we must unbox them correctly
        val map = mapOf<String, Any?>(
            "uid" to "uid_abc",
            "displayName" to "Bee",
            "cumulativeScore" to 50000L,
            "levelsCompleted" to 8L,
            "highestLevelUnlocked" to 9L
        )
        val profile = UserProfile.fromMap(map)

        assertEquals(50000L, profile.cumulativeScore)
        assertEquals(8, profile.levelsCompleted)
        assertEquals(9, profile.highestLevelUnlocked)
    }

    @Test
    fun `fromMap provides defaults for empty input`() {
        val profile = UserProfile.fromMap(emptyMap())

        assertEquals("", profile.uid)
        assertEquals("Player", profile.displayName)
        assertEquals(0L, profile.cumulativeScore)
        assertEquals(0, profile.levelsCompleted)
        assertEquals(1, profile.highestLevelUnlocked)
    }
}
