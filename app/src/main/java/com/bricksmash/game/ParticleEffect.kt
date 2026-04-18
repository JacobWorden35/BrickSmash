/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * A simple particle system for visual effects when bricks are destroyed.
 * Particles burst outward and fade over time.
 */
class ParticleEffect(
    private val originX: Float,
    private val originY: Float,
    private val color: Int,
    private val particleCount: Int = 12
) {
    private data class Particle(
        var x: Float,
        var y: Float,
        var dx: Float,
        var dy: Float,
        var life: Float,    // 1.0 → 0.0
        var size: Float,
        val color: Int
    )

    private val particles: List<Particle>
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val decayRate = 0.03f

    var isFinished: Boolean = false
        private set

    init {
        particles = List(particleCount) {
            val angle = Math.random() * 2 * Math.PI
            val speed = (2 + Math.random() * 6).toFloat()
            Particle(
                x = originX,
                y = originY,
                dx = (cos(angle) * speed).toFloat(),
                dy = (sin(angle) * speed).toFloat(),
                life = 1.0f,
                size = (3 + Math.random() * 5).toFloat(),
                color = varyColor(color)
            )
        }
    }

    fun update() {
        var allDead = true
        for (p in particles) {
            if (p.life <= 0) continue
            allDead = false
            p.x += p.dx
            p.y += p.dy
            p.dy += 0.15f  // gravity
            p.life -= decayRate
            p.size *= 0.97f
        }
        isFinished = allDead
    }

    fun draw(canvas: Canvas) {
        for (p in particles) {
            if (p.life <= 0) continue
            paint.color = p.color
            paint.alpha = (p.life * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
    }

    companion object {
        /**
         * Creates a slightly varied version of the given color for visual interest.
         */
        private fun varyColor(baseColor: Int): Int {
            val r = (Color.red(baseColor) + (-20..20).random()).coerceIn(0, 255)
            val g = (Color.green(baseColor) + (-20..20).random()).coerceIn(0, 255)
            val b = (Color.blue(baseColor) + (-20..20).random()).coerceIn(0, 255)
            return Color.rgb(r, g, b)
        }
    }
}
