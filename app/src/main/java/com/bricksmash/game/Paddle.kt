package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * The player-controlled paddle at the bottom of the screen.
 * Moves horizontally based on touch input.
 */
class Paddle(
    var x: Float = 0f,       // center x
    var y: Float = 0f,       // center y
    var width: Float = 200f,
    var height: Float = 24f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var screenWidth: Float = 0f
    var isWide: Boolean = false
        set(value) {
            field = value
            width = if (value) normalWidth * 1.6f else normalWidth
        }

    private var normalWidth: Float = 200f

    val bounds: RectF
        get() = RectF(
            x - width / 2f,
            y - height / 2f,
            x + width / 2f,
            y + height / 2f
        )

    /**
     * Initializes the paddle position on screen.
     */
    fun init(screenW: Float, screenH: Float) {
        screenWidth = screenW
        normalWidth = screenW * 0.22f // ~22% of screen width
        width = normalWidth
        x = screenW / 2f
        y = screenH - 120f  // Position near the bottom with some margin
    }

    /**
     * Moves the paddle to follow the user's touch x position.
     * Clamps to screen edges.
     */
    fun moveTo(touchX: Float) {
        x = touchX.coerceIn(width / 2f, screenWidth - width / 2f)
    }

    fun draw(canvas: Canvas) {
        val left = x - width / 2f
        val top = y - height / 2f
        val right = x + width / 2f
        val bottom = y + height / 2f
        val cornerRadius = height / 2f

        // Gradient fill
        paint.shader = LinearGradient(
            left, top, left, bottom,
            Color.rgb(100, 181, 246),  // Light blue
            Color.rgb(30, 136, 229),   // Darker blue
            Shader.TileMode.CLAMP
        )

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    fun reset(screenW: Float, screenH: Float) {
        isWide = false
        init(screenW, screenH)
    }
}
