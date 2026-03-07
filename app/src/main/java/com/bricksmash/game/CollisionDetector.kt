package com.bricksmash.game

import android.graphics.RectF

/**
 * Handles collision detection between the ball and bricks.
 * Uses AABB (Axis-Aligned Bounding Box) intersection tests and
 * determines the correct reflection direction.
 */
object CollisionDetector {

    data class CollisionResult(
        val brick: Brick,
        val reflectX: Boolean,
        val reflectY: Boolean
    )

    /**
     * Checks if the ball collides with any brick in the list.
     * Returns the first collision found, or null if none.
     *
     * The reflection direction is determined by which side of the
     * brick the ball hit (top/bottom → reflect Y, left/right → reflect X).
     */
    fun checkBallBrickCollision(ball: Ball, bricks: List<Brick>): CollisionResult? {
        val ballBounds = ball.bounds

        for (brick in bricks) {
            if (!brick.isAlive) continue
            if (!RectF.intersects(ballBounds, brick.rect)) continue

            // Determine collision side
            val overlapLeft = ballBounds.right - brick.rect.left
            val overlapRight = brick.rect.right - ballBounds.left
            val overlapTop = ballBounds.bottom - brick.rect.top
            val overlapBottom = brick.rect.bottom - ballBounds.top

            val minOverlapX = minOf(overlapLeft, overlapRight)
            val minOverlapY = minOf(overlapTop, overlapBottom)

            return if (minOverlapX < minOverlapY) {
                // Side collision → reflect X
                CollisionResult(brick, reflectX = true, reflectY = false)
            } else {
                // Top/bottom collision → reflect Y
                CollisionResult(brick, reflectX = false, reflectY = true)
            }
        }

        return null
    }

    /**
     * Applies the collision result to the ball, reflecting its velocity
     * and hitting the brick.
     */
    fun applyCollision(ball: Ball, result: CollisionResult): Boolean {
        // If fireball, don't reflect — just pass through
        if (!ball.isFireball) {
            if (result.reflectX) ball.dx = -ball.dx
            if (result.reflectY) ball.dy = -ball.dy
        }

        return result.brick.hit()
    }
}
