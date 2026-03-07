package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Represents the ball in the game. Handles its position, velocity,
 * and rendering. The ball bounces off walls, the paddle, and bricks.
 */
class Ball(
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 15f,
    var dx: Float = 0f,  // velocity x
    var dy: Float = 0f,  // velocity y
    var speed: Float = 12f,
    var isActive: Boolean = false
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(8f, 0f, 0f, Color.WHITE)
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 255, 255, 255)
        style = Paint.Style.FILL
    }

    var isFireball: Boolean = false

    val bounds: RectF
        get() = RectF(x - radius, y - radius, x + radius, y + radius)

    /**
     * Launches the ball upward at a slight angle from the paddle.
     */
    fun launch(paddleCenterX: Float, paddleTopY: Float) {
        x = paddleCenterX
        y = paddleTopY - radius - 2f
        // Launch at a slight random angle between -30 and 30 degrees
        val angle = Math.toRadians((-30..30).random().toDouble())
        dx = (speed * Math.sin(angle)).toFloat()
        dy = -speed
        isActive = true
    }

    /**
     * Updates ball position based on current velocity.
     */
    fun update() {
        if (!isActive) return
        x += dx
        y += dy
    }

    /**
     * Handles bouncing off the left, right, and top walls.
     * Returns true if the ball fell below the screen (life lost).
     */
    fun handleWallCollisions(screenWidth: Float, screenHeight: Float): Boolean {
        // Left wall
        if (x - radius <= 0) {
            x = radius
            dx = Math.abs(dx)
        }
        // Right wall
        if (x + radius >= screenWidth) {
            x = screenWidth - radius
            dx = -Math.abs(dx)
        }
        // Top wall
        if (y - radius <= 0) {
            y = radius
            dy = Math.abs(dy)
        }
        // Bottom — ball lost
        return y - radius > screenHeight
    }

    /**
     * Handles bouncing off the paddle. Adjusts the angle based on
     * where the ball hits the paddle (edges = steeper angle).
     */
    fun handlePaddleCollision(paddle: Paddle): Boolean {
        if (!isActive) return false

        val paddleBounds = paddle.bounds
        if (bounds.intersect(paddleBounds) && dy > 0) {
            // Calculate hit position relative to paddle center (-1 to 1)
            val hitPos = (x - paddle.x) / (paddle.width / 2f)
            val clampedHit = hitPos.coerceIn(-1f, 1f)

            // Reflect upward with angle based on hit position
            val maxAngle = Math.toRadians(60.0)
            val angle = clampedHit * maxAngle

            val currentSpeed = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
            dx = (currentSpeed * Math.sin(angle)).toFloat()
            dy = -(currentSpeed * Math.cos(angle)).toFloat()

            // Push ball above paddle to prevent sticking
            y = paddleBounds.top - radius - 1f
            return true
        }
        return false
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return

        // Glow effect
        canvas.drawCircle(x, y, radius * 1.8f, glowPaint)

        // Change color if fireball
        if (isFireball) {
            paint.color = Color.rgb(255, 100, 0)
        } else {
            paint.color = Color.WHITE
        }
        canvas.drawCircle(x, y, radius, paint)
    }

    fun reset() {
        isActive = false
        dx = 0f
        dy = 0f
        isFireball = false
    }

    /**
     * Creates a copy of this ball for multi-ball power-up.
     */
    fun clone(): Ball {
        return Ball(x, y, radius, -dx, dy, speed, isActive).also {
            it.isFireball = isFireball
        }
    }
}
