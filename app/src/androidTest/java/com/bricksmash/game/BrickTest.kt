/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */

package com.bricksmash.game

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for the Brick class. These run on an Android
 * device/emulator because Brick depends on the android.graphics package
 * (Paint, RectF). The tests verify hit tracking, destruction logic,
 * and point values for each brick type.
 */
@RunWith(AndroidJUnit4::class)
class BrickTest {

    @Test
    fun normalBrickIsDestroyedInOneHit() {
        val brick = Brick(row = 0, col = 0, type = 1)
        assertTrue("Brick should start alive", brick.isAlive)
        val destroyed = brick.hit()
        assertTrue("Normal brick should be destroyed in one hit", destroyed)
        assertFalse("Brick should no longer be alive", brick.isAlive)
    }

    @Test
    fun hardenedBrickRequiresTwoHits() {
        val brick = Brick(row = 0, col = 0, type = 2)
        val firstHitDestroyed = brick.hit()
        assertFalse("First hit should damage but not destroy", firstHitDestroyed)
        assertTrue("Brick should still be alive after one hit", brick.isAlive)

        val secondHitDestroyed = brick.hit()
        assertTrue("Second hit should destroy the brick", secondHitDestroyed)
        assertFalse("Brick should no longer be alive", brick.isAlive)
    }

    @Test
    fun indestructibleBrickCannotBeDestroyed() {
        val brick = Brick(row = 0, col = 0, type = 3)
        // Hit it many times — should never be destroyed
        repeat(100) {
            val destroyed = brick.hit()
            assertFalse("Indestructible brick should never be destroyed", destroyed)
            assertTrue("Indestructible brick should remain alive", brick.isAlive)
        }
    }

    @Test
    fun pointsValuesMatchDesign() {
        assertEquals(100, Brick(row = 0, col = 0, type = 1).getPoints())
        assertEquals(250, Brick(row = 0, col = 0, type = 2).getPoints())
        assertEquals(0, Brick(row = 0, col = 0, type = 3).getPoints())
    }
}
