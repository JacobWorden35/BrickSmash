/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */

package com.bricksmash.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the ScoreEntry model. Verifies round-trip conversion
 * between a Kotlin data class and the Firestore map representation.
 */
class ScoreEntryTest {

    @Test
    fun `toMap contains all expected fields`() {
        val entry = ScoreEntry(
            userId = "user123",
            displayName = "TestPlayer",
            score = 5000,
            levelId = "builtin_3",
            levelName = "Brick Wall",
            timestamp = 1234567890L
        )
        val map = entry.toMap()

        assertEquals("user123", map["userId"])
        assertEquals("TestPlayer", map["displayName"])
        assertEquals(5000, map["score"])
        assertEquals("builtin_3", map["levelId"])
        assertEquals("Brick Wall", map["levelName"])
        assertEquals(1234567890L, map["timestamp"])
    }

    @Test
    fun `fromMap reconstructs ScoreEntry correctly`() {
        // Firestore returns numeric fields as Long, so we simulate that
        val map = mapOf<String, Any?>(
            "userId" to "user123",
            "displayName" to "TestPlayer",
            "score" to 5000L,
            "levelId" to "builtin_3",
            "levelName" to "Brick Wall",
            "timestamp" to 1234567890L
        )
        val entry = ScoreEntry.fromMap("doc_abc", map)

        assertEquals("doc_abc", entry.odcId)
        assertEquals("user123", entry.userId)
        assertEquals(5000, entry.score)
        assertEquals("TestPlayer", entry.displayName)
    }

    @Test
    fun `fromMap applies sensible defaults for missing fields`() {
        val entry = ScoreEntry.fromMap("doc_xyz", emptyMap())

        assertEquals("doc_xyz", entry.odcId)
        assertEquals("", entry.userId)
        assertEquals(0, entry.score)
        assertEquals("Anonymous", entry.displayName)
        assertEquals(0L, entry.timestamp)
    }
}
