package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

/**
 * Represents a single brick in the game grid.
 *
 * Brick types:
 *  0 = empty (no brick)
 *  1 = normal (1 hit to destroy)
 *  2 = hardened (2 hits to destroy)
 *  3 = indestructible (cannot be destroyed)
 */
class Brick(
    val row: Int,
    val col: Int,
    var type: Int,
    var color: Int = Color.RED,
    val rect: RectF = RectF()
) {
    var hitsRemaining: Int = when (type) {
        1 -> 1
        2 -> 2
        3 -> Int.MAX_VALUE
        else -> 0
    }

    var isAlive: Boolean = type != 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        color = Color.argb(80, 0, 0, 0)
    }

    private val crackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.argb(120, 255, 255, 255)
    }

    /**
     * Calculates the brick's screen position based on the grid layout.
     */
    fun calculateBounds(
        gridLeft: Float,
        gridTop: Float,
        brickWidth: Float,
        brickHeight: Float,
        padding: Float = 3f
    ) {
        rect.set(
            gridLeft + col * brickWidth + padding,
            gridTop + row * brickHeight + padding,
            gridLeft + (col + 1) * brickWidth - padding,
            gridTop + (row + 1) * brickHeight - padding
        )
    }

    /**
     * Applies a hit to the brick. Returns true if the brick was destroyed.
     */
    fun hit(): Boolean {
        if (!isAlive || type == 3) return false
        hitsRemaining--
        if (hitsRemaining <= 0) {
            isAlive = false
            return true
        }
        return false
    }

    /**
     * Returns the point value for destroying this brick.
     */
    fun getPoints(): Int = when (type) {
        1 -> 100
        2 -> 250
        else -> 0
    }

    fun draw(canvas: Canvas) {
        if (!isAlive) return

        // Set color based on type
        paint.color = when (type) {
            1 -> color
            2 -> if (hitsRemaining == 2) Color.rgb(255, 193, 7) else Color.rgb(255, 152, 0)
            3 -> Color.rgb(120, 120, 120)
            else -> Color.TRANSPARENT
        }

        val cornerRadius = 6f
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Draw crack lines on damaged hardened bricks
        if (type == 2 && hitsRemaining == 1) {
            val cx = rect.centerX()
            val cy = rect.centerY()
            canvas.drawLine(cx - 10f, cy - 5f, cx + 5f, cy + 8f, crackPaint)
            canvas.drawLine(cx + 5f, cy + 8f, cx - 3f, cy + 12f, crackPaint)
        }

        // Draw X pattern on indestructible bricks
        if (type == 3) {
            val inset = 6f
            crackPaint.color = Color.argb(100, 200, 200, 200)
            canvas.drawLine(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset, crackPaint)
            canvas.drawLine(rect.right - inset, rect.top + inset, rect.left + inset, rect.bottom - inset, crackPaint)
        }
    }

    companion object {
        /** Maps difficulty/row position to colors for visual variety */
        val ROW_COLORS = listOf(
            Color.rgb(244, 67, 54),    // Red
            Color.rgb(233, 30, 99),    // Pink
            Color.rgb(156, 39, 176),   // Purple
            Color.rgb(63, 81, 181),    // Indigo
            Color.rgb(33, 150, 243),   // Blue
            Color.rgb(0, 188, 212),    // Cyan
            Color.rgb(76, 175, 80),    // Green
            Color.rgb(255, 235, 59),   // Yellow
            Color.rgb(255, 152, 0),    // Orange
            Color.rgb(255, 87, 34)     // Deep Orange
        )
    }
}
