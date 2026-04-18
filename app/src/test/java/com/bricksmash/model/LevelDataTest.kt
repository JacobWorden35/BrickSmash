/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */

package com.bricksmash.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the LevelData model class. Verifies that breakable
 * brick counting handles all brick types correctly and that serialization
 * defaults behave as expected.
 */
class LevelDataTest {

    @Test
    fun `breakableBrickCount returns 0 for empty grid`() {
        val level = LevelData(grid = emptyList())
        assertEquals(0, level.breakableBrickCount())
    }

    @Test
    fun `breakableBrickCount counts only types 1 and 2`() {
        // A grid with one of each brick type including empty (0) and indestructible (3)
        val grid = listOf(
            listOf(
                BrickData(type = 0), // empty — should not count
                BrickData(type = 1), // normal — counts
                BrickData(type = 2), // hardened — counts
                BrickData(type = 3)  // indestructible — should not count
            )
        )
        val level = LevelData(grid = grid)
        assertEquals(2, level.breakableBrickCount())
    }

    @Test
    fun `breakableBrickCount sums across multiple rows`() {
        val grid = listOf(
            listOf(BrickData(type = 1), BrickData(type = 1), BrickData(type = 1)),
            listOf(BrickData(type = 2), BrickData(type = 2), BrickData(type = 0)),
            listOf(BrickData(type = 3), BrickData(type = 3), BrickData(type = 3))
        )
        val level = LevelData(grid = grid)
        // 3 normals + 2 hardened + 0 indestructible-counted = 5
        assertEquals(5, level.breakableBrickCount())
    }

    @Test
    fun `default values produce a valid built-in level`() {
        val level = LevelData()
        assertTrue(level.isBuiltIn)
        assertEquals("Untitled", level.name)
        assertEquals(1, level.difficulty)
        assertEquals(1.0f, level.ballSpeedMultiplier, 0.001f)
    }
}
