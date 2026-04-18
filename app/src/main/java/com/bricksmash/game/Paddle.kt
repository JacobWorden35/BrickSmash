/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * The player-controlled paddle. Supports stackable Wide power-up:
 * each stack level increases width by 30%, capped at the screen width.
 */
class Paddle(
    var x: Float = 0f,
    var y: Float = 0f,
    var width: Float = 200f,
    var height: Float = 24f
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 2f
    }

    private var screenWidth: Float = 0f
    private var normalWidth: Float = 200f

    val bounds: RectF
        get() = RectF(x - width / 2f, y - height / 2f, x + width / 2f, y + height / 2f)

    fun init(screenW: Float, screenH: Float) {
        screenWidth = screenW
        normalWidth = screenW * 0.22f
        width = normalWidth
        x = screenW / 2f
        y = screenH - 200f
    }

    fun moveTo(touchX: Float) {
        x = touchX.coerceIn(width / 2f, screenWidth - width / 2f)
    }

    /**
     * Applies the Wide power-up stack. Stack level 0 = normal width;
     * each level multiplies base width by (1 + 0.3 * level), capped at screenWidth.
     */
    fun applyWideStack(stackLevel: Int, screenW: Float) {
        screenWidth = screenW
        val multiplier = 1f + (0.3f * stackLevel)
        width = (normalWidth * multiplier).coerceAtMost(screenW)
        // Re-clamp x after width change
        moveTo(x)
    }

    fun draw(canvas: Canvas) {
        val left = x - width / 2f
        val top = y - height / 2f
        val right = x + width / 2f
        val bottom = y + height / 2f
        val cornerRadius = height / 2f

        paint.shader = LinearGradient(
            left, top, left, bottom,
            Color.rgb(100, 181, 246),
            Color.rgb(30, 136, 229),
            Shader.TileMode.CLAMP
        )
        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)
    }

    fun reset(screenW: Float, screenH: Float) {
        init(screenW, screenH)
    }
}