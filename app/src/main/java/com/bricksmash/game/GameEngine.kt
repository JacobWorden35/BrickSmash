package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.bricksmash.model.LevelData

/**
 * Main game engine. Handles all state, collisions, scoring, and HUD drawing.
 *
 * Power-up stacking rules:
 *  - WIDE_PADDLE: each pickup bumps the paddle width by +30% (capped at screen width),
 *    and refreshes the timer. Multiple pickups stack the "level" (1.3x, 1.6x, 1.9x...).
 *  - MULTI_BALL: each pickup spawns 2 extra balls AND is tracked in the HUD with a
 *    count + shared timer; on expiry extra balls are removed down to 1.
 *  - FIREBALL / SLOW_MOTION: refresh-on-pickup, single-instance.
 *
 * Scoring: base points * combo multiplier (+0.1x per consecutive hit, cap 5x,
 * resets on paddle touch or 2s gap). End-of-level bonuses: speed + life.
 */
class GameEngine {

    val paddle = Paddle()
    private val balls = mutableListOf<Ball>()
    private val bricks = mutableListOf<Brick>()
    private val powerUps = mutableListOf<PowerUp>()
    private val particles = mutableListOf<ParticleEffect>()

    var score: Int = 0; private set
    var lives: Int = 3; private set
    var isPaused: Boolean = false
    var isGameOver: Boolean = false; private set
    var isLevelComplete: Boolean = false; private set

    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f
    var statusBarHeight: Float = 80f

    private var currentLevel: LevelData? = null
    private var totalBreakableBricks: Int = 0
    private var levelStartTime: Long = 0L

    // Scoring
    private var comboMultiplier: Float = 1.0f
    private var lastBrickHitTime: Long = 0L
    private var comboPulseTime: Long = 0L
    private val comboResetMs = 2000L
    private val maxCombo = 5.0f

    // Power-up tracking: each entry holds stack count and most recent end time
    private data class ActivePowerUp(
        val type: PowerUp.Type,
        var count: Int,
        var endTime: Long
    )
    private val activePowerUps = mutableListOf<ActivePowerUp>()

    // Paints ------------------------------------------------------------------
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 56f; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255); textSize = 22f; letterSpacing = 0.1f
    }
    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val lifeBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val lifeBallGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255); style = Paint.Style.FILL
    }
    private val powerUpLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 20f; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val powerUpBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 0, 0); style = Paint.Style.FILL
    }
    private val powerUpBarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val overlayPaint = Paint().apply { color = Color.argb(160, 0, 0, 0) }
    private val overlayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 56f; textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val overlaySubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255); textSize = 32f; textAlign = Paint.Align.CENTER
    }
    private val pauseInfoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(220, 255, 255, 255); textSize = 24f; textAlign = Paint.Align.CENTER
    }

    var onLevelComplete: ((score: Int) -> Unit)? = null
    var onGameOver: ((score: Int) -> Unit)? = null

    fun init(width: Float, height: Float) {
        screenWidth = width; screenHeight = height
        paddle.init(width, height)
    }

    fun loadLevel(level: LevelData) {
        currentLevel = level
        bricks.clear(); balls.clear(); powerUps.clear()
        particles.clear(); activePowerUps.clear()

        score = 0; lives = 3
        isGameOver = false; isLevelComplete = false; isPaused = false
        comboMultiplier = 1.0f; lastBrickHitTime = 0L; comboPulseTime = 0L
        levelStartTime = System.currentTimeMillis()

        paddle.reset(screenWidth, screenHeight)

        val gridTop = statusBarHeight + 140f
        val gridHeight = screenHeight * 0.38f
        val brickWidth = screenWidth / level.cols
        val brickHeight = gridHeight / level.rows

        for (row in level.grid.indices) {
            for (col in level.grid[row].indices) {
                val bd = level.grid[row][col]
                if (bd.type != 0) {
                    val brick = Brick(row, col, bd.type, Brick.ROW_COLORS[row % Brick.ROW_COLORS.size])
                    brick.calculateBounds(0f, gridTop, brickWidth, brickHeight)
                    bricks.add(brick)
                }
            }
        }
        totalBreakableBricks = bricks.count { it.type in 1..2 }
        balls.add(Ball(radius = 12f, speed = 10f * level.ballSpeedMultiplier))
    }

    fun update() {
        if (isPaused || isGameOver || isLevelComplete) return
        val now = System.currentTimeMillis()
        checkPowerUpTimers(now)
        checkComboTimeout(now)

        val primaryBall = balls.firstOrNull() ?: return
        if (!primaryBall.isActive) {
            primaryBall.x = paddle.x
            primaryBall.y = paddle.bounds.top - primaryBall.radius - 2f
            return
        }

        val ballsToRemove = mutableListOf<Ball>()
        for (ball in balls) {
            if (!ball.isActive) continue
            val slowActive = activePowerUps.any { it.type == PowerUp.Type.SLOW_MOTION && now < it.endTime }
            if (slowActive) { ball.x += ball.dx * 0.5f; ball.y += ball.dy * 0.5f } else ball.update()

            if (ball.handleWallCollisions(screenWidth, screenHeight)) ballsToRemove.add(ball)
            if (ball.handlePaddleCollision(paddle)) resetCombo()

            val collision = CollisionDetector.checkBallBrickCollision(ball, bricks)
            if (collision != null) {
                val destroyed = CollisionDetector.applyCollision(ball, collision)
                if (destroyed) {
                    registerBrickHit(now)
                    score += (collision.brick.getPoints() * comboMultiplier).toInt()
                    particles.add(ParticleEffect(
                        collision.brick.rect.centerX(),
                        collision.brick.rect.centerY(),
                        collision.brick.color))
                    PowerUp.maybeSpawn(
                        collision.brick.rect.centerX(),
                        collision.brick.rect.centerY())?.let { powerUps.add(it) }
                }
            }
        }
        balls.removeAll(ballsToRemove)

        if (balls.none { it.isActive }) {
            lives--; resetCombo()
            if (lives <= 0) { isGameOver = true; onGameOver?.invoke(score) }
            else balls.add(Ball(radius = 12f, speed = 10f * (currentLevel?.ballSpeedMultiplier ?: 1.0f)))
        }

        val puRemove = mutableListOf<PowerUp>()
        for (pu in powerUps) {
            if (pu.update(screenHeight)) { puRemove.add(pu); continue }
            if (pu.checkPaddleCollision(paddle)) { applyPowerUp(pu, now); puRemove.add(pu) }
        }
        powerUps.removeAll(puRemove)

        particles.forEach { it.update() }
        particles.removeAll { it.isFinished }

        val remaining = bricks.count { it.isAlive && it.type in 1..2 }
        if (remaining == 0 && totalBreakableBricks > 0) {
            val elapsedSeconds = (now - levelStartTime) / 1000L
            val speedBonus = maxOf(0L, 5000L - elapsedSeconds * 50L).toInt()
            score += speedBonus + (lives * 1000)
            isLevelComplete = true
            onLevelComplete?.invoke(score)
        }
    }

    // -- Combo ----------------------------------------------------------------
    private fun registerBrickHit(now: Long) {
        comboMultiplier = if (lastBrickHitTime != 0L && (now - lastBrickHitTime) <= comboResetMs)
            (comboMultiplier + 0.1f).coerceAtMost(maxCombo) else 1.1f
        lastBrickHitTime = now; comboPulseTime = now
    }
    private fun checkComboTimeout(now: Long) {
        if (lastBrickHitTime != 0L && (now - lastBrickHitTime) > comboResetMs) resetCombo()
    }
    private fun resetCombo() { comboMultiplier = 1.0f; lastBrickHitTime = 0L }

    // -- Power-ups ------------------------------------------------------------
    private fun applyPowerUp(pu: PowerUp, now: Long) {
        when (pu.type) {
            PowerUp.Type.MULTI_BALL -> {
                // Spawn 2 extra balls per pickup
                val src = balls.firstOrNull { it.isActive } ?: return
                balls.add(src.clone())
                balls.add(src.clone().also { it.dx = -it.dx })
                // Track with a timer (8s shared) — expiry prunes extra balls
                stackPowerUp(pu.type, now, 8000L)
            }
            PowerUp.Type.WIDE_PADDLE -> {
                stackPowerUp(pu.type, now, pu.type.durationMs)
                // Paddle width grows per stack, capped at screen width
                val stack = activePowerUps.firstOrNull { it.type == PowerUp.Type.WIDE_PADDLE }?.count ?: 1
                paddle.applyWideStack(stack, screenWidth)
            }
            PowerUp.Type.FIREBALL -> {
                balls.forEach { it.isFireball = true }
                stackPowerUp(pu.type, now, pu.type.durationMs)
            }
            PowerUp.Type.SLOW_MOTION -> stackPowerUp(pu.type, now, pu.type.durationMs)
        }
    }

    /** Increments stack count and refreshes the end time. */
    private fun stackPowerUp(type: PowerUp.Type, now: Long, durationMs: Long) {
        val existing = activePowerUps.firstOrNull { it.type == type }
        if (existing != null) {
            existing.count++
            existing.endTime = now + durationMs
        } else {
            activePowerUps.add(ActivePowerUp(type, 1, now + durationMs))
        }
    }

    private fun checkPowerUpTimers(now: Long) {
        val expired = activePowerUps.filter { now >= it.endTime }
        for (e in expired) {
            when (e.type) {
                PowerUp.Type.WIDE_PADDLE -> paddle.applyWideStack(0, screenWidth)
                PowerUp.Type.FIREBALL -> balls.forEach { it.isFireball = false }
                PowerUp.Type.MULTI_BALL -> {
                    // Collapse back to a single active ball
                    val keeper = balls.firstOrNull { it.isActive }
                    balls.clear()
                    if (keeper != null) balls.add(keeper)
                    else balls.add(Ball(radius = 12f, speed = 10f * (currentLevel?.ballSpeedMultiplier ?: 1.0f)))
                }
                PowerUp.Type.SLOW_MOTION -> { }
            }
        }
        activePowerUps.removeAll(expired)
    }

    fun launchBall() {
        val b = balls.firstOrNull() ?: return
        if (!b.isActive) b.launch(paddle.x, paddle.bounds.top)
    }

    fun onTouchMove(x: Float) { paddle.moveTo(x) }

    /** For keyboard control: move paddle by a delta amount. */
    fun movePaddleBy(delta: Float) { paddle.moveTo(paddle.x + delta) }

    // -- Drawing --------------------------------------------------------------
    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(18, 18, 32))
        bricks.forEach { it.draw(canvas) }
        particles.forEach { it.draw(canvas) }
        powerUps.forEach { it.draw(canvas) }
        paddle.draw(canvas)
        balls.forEach { it.draw(canvas) }
        drawTitle(canvas)
        drawPowerUpsUnderPaddle(canvas)
        drawBottomBar(canvas)

        if (isPaused) drawPauseOverlay(canvas)
        if (isGameOver) drawOverlay(canvas, "GAME OVER", "Score: $score")
        if (isLevelComplete) {
            val es = (System.currentTimeMillis() - levelStartTime) / 1000L
            drawOverlay(canvas, "LEVEL CLEAR!", "Score: $score  •  Time: ${es}s")
        }
        val b = balls.firstOrNull()
        if (b != null && !b.isActive && !isGameOver && !isLevelComplete && !isPaused)
            drawOverlay(canvas, "TAP TO LAUNCH", "Move paddle with touch or arrow keys")
    }

    private fun drawTitle(canvas: Canvas) {
        currentLevel?.let {
            canvas.drawText(it.name, screenWidth / 2f, statusBarHeight + 80f, titlePaint)
        }
    }

    private fun drawPowerUpsUnderPaddle(canvas: Canvas) {
        if (activePowerUps.isEmpty()) return
        val now = System.currentTimeMillis()
        val w = 100f; val h = 36f; val spacing = 10f
        val total = activePowerUps.size * w + (activePowerUps.size - 1).coerceAtLeast(0) * spacing
        var x = (screenWidth - total) / 2f
        val y = paddle.y + paddle.height + 20f

        for (a in activePowerUps) {
            val progress = ((a.endTime - now).toFloat() / a.type.durationMs.toFloat()).coerceIn(0f, 1f)
            canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, powerUpBarBgPaint)
            powerUpBarFillPaint.color = a.type.color
            canvas.drawRoundRect(RectF(x, y, x + w * progress, y + h), 8f, 8f, powerUpBarFillPaint)

            val baseLabel = when (a.type) {
                PowerUp.Type.WIDE_PADDLE -> "WIDE"
                PowerUp.Type.FIREBALL -> "FIRE"
                PowerUp.Type.SLOW_MOTION -> "SLOW"
                PowerUp.Type.MULTI_BALL -> "MULTI"
            }
            val label = if (a.count > 1) "$baseLabel ${a.count}x" else baseLabel
            canvas.drawText(label, x + w / 2f, y + h * 0.7f, powerUpLabelPaint)
            x += w + spacing
        }
    }

    private fun drawBottomBar(canvas: Canvas) {
        val bottomY = screenHeight - 50f
        val sideMargin = 24f

        canvas.drawText("SCORE", sideMargin, bottomY - 48f, scoreLabelPaint)
        val since = System.currentTimeMillis() - comboPulseTime
        val scale = if (since < 200 && comboMultiplier > 1.0f) 1.0f + (200 - since) / 2000f else 1.0f
        scorePaint.textSize = 44f * scale
        canvas.drawText("$score", sideMargin, bottomY - 10f, scorePaint)

        if (comboMultiplier > 1.0f) {
            val m = "x%.1f combo".format(comboMultiplier)
            comboPaint.color = colorForCombo(comboMultiplier)
            val w = scorePaint.measureText("$score")
            canvas.drawText(m, sideMargin + w + 16f, bottomY - 16f, comboPaint)
        }

        // Lives — with "LIVES" label to the left of the balls
        val ballRadius = 14f; val ballSpacing = 10f
        val totalLivesWidth = (lives * (ballRadius * 2f)) + ((lives - 1).coerceAtLeast(0) * ballSpacing)
        val livesLabelText = "LIVES"
        val livesLabelWidth = scoreLabelPaint.measureText(livesLabelText)

        val livesRightEdge = screenWidth - sideMargin
        val livesLabelX = livesRightEdge - totalLivesWidth - 10f - livesLabelWidth

        canvas.drawText(livesLabelText, livesLabelX, bottomY - 14f, scoreLabelPaint)

        var ballX = livesRightEdge - totalLivesWidth + ballRadius
        val ballY = bottomY - 20f
        for (i in 0 until lives) {
            canvas.drawCircle(ballX, ballY, ballRadius * 1.6f, lifeBallGlowPaint)
            canvas.drawCircle(ballX, ballY, ballRadius, lifeBallPaint)
            ballX += ballRadius * 2f + ballSpacing
        }
    }

    private fun colorForCombo(m: Float): Int = when {
        m >= 4.0f -> Color.rgb(244, 67, 54)
        m >= 3.0f -> Color.rgb(255, 152, 0)
        m >= 2.0f -> Color.rgb(255, 235, 59)
        else -> Color.WHITE
    }

    private fun drawOverlay(canvas: Canvas, title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        canvas.drawText(title, screenWidth / 2f, screenHeight / 2f - 20f, overlayTextPaint)
        canvas.drawText(subtitle, screenWidth / 2f, screenHeight / 2f + 40f, overlaySubPaint)
    }

    /**
     * Pause overlay with quick game info so players can review controls
     * and power-up effects without leaving the game.
     */
    private fun drawPauseOverlay(canvas: Canvas) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        val cx = screenWidth / 2f
        var y = screenHeight * 0.25f
        canvas.drawText("PAUSED", cx, y, overlayTextPaint); y += 60f
        canvas.drawText("Tap to resume", cx, y, overlaySubPaint); y += 80f

        val lines = listOf(
            "— QUICK INFO —",
            "",
            "Controls: Drag or use arrow keys",
            "Combo: Chain brick hits (+0.1x each, max 5x)",
            "Combo resets on paddle hit or 2s gap",
            "",
            "Power-ups:",
            "WIDE — paddle grows (stacks)",
            "FIRE — ball passes through bricks",
            "SLOW — ball slows down",
            "MULTI — extra balls (stacks)",
            "",
            "Level bonus: +1000 per life, speed bonus"
        )
        for (line in lines) {
            canvas.drawText(line, cx, y, pauseInfoPaint)
            y += 32f
        }
    }
}