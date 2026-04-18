/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Power-ups that drop from destroyed bricks and can be collected
 * by the paddle. Each type provides a temporary effect.
 */
class PowerUp(
    var x: Float,
    var y: Float,
    val type: Type,
    var isActive: Boolean = true
) {
    enum class Type(val label: String, val color: Int, val durationMs: Long) {
        MULTI_BALL("M", Color.rgb(76, 175, 80), 0),           // Instant: spawns extra balls
        WIDE_PADDLE("W", Color.rgb(33, 150, 243), 10000),     // 10s wider paddle
        FIREBALL("F", Color.rgb(255, 87, 34), 8000),          // 8s pass-through
        SLOW_MOTION("S", Color.rgb(156, 39, 176), 6000)       // 6s slow ball
    }

    private val size = 28f
    private val fallSpeed = 4f

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = type.color
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    val bounds: RectF
        get() = RectF(x - size, y - size, x + size, y + size)

    /**
     * Updates the power-up position (falls downward).
     * Returns true if it has fallen off screen.
     */
    fun update(screenHeight: Float): Boolean {
        if (!isActive) return true
        y += fallSpeed
        return y - size > screenHeight
    }

    /**
     * Checks if this power-up collides with the paddle.
     */
    fun checkPaddleCollision(paddle: Paddle): Boolean {
        if (!isActive) return false
        return bounds.intersect(paddle.bounds)
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return

        // Draw capsule background
        val rect = RectF(x - size, y - size * 0.7f, x + size, y + size * 0.7f)
        canvas.drawRoundRect(rect, size * 0.7f, size * 0.7f, bgPaint)
        canvas.drawRoundRect(rect, size * 0.7f, size * 0.7f, borderPaint)

        // Draw type label
        canvas.drawText(type.label, x, y + 8f, textPaint)
    }

    companion object {
        private const val DROP_CHANCE = 0.20f // 20% chance per brick destroyed

        /**
         * Randomly decides if a power-up should spawn and picks a random type.
         */
        fun maybeSpawn(brickCenterX: Float, brickCenterY: Float): PowerUp? {
            if (Math.random() > DROP_CHANCE) return null
            val type = Type.entries.random()
            return PowerUp(brickCenterX, brickCenterY, type)
        }
    }
}
