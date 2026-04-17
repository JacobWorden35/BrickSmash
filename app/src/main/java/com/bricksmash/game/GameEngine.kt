package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.bricksmash.model.LevelData

/**
 * The main game engine that manages all game state and objects.
 *
 * HUD layout:
 *  - Top center: Level title (large)
 *  - Right side (vertical stack above paddle): active power-up indicators with shrink bars
 *  - Bottom left: Score + combo multiplier
 *  - Bottom right: Lives as balls (visual)
 *
 * Scoring model:
 *  - Base points per brick (100 normal, 250 hardened)
 *  - Combo multiplier: +0.1x per consecutive hit within 2s, capped at 5.0x
 *  - Speed bonus at level end: max(0, 5000 - elapsed_seconds * 50)
 *  - Life bonus at level end: +1000 per remaining life
 */
class GameEngine {

    // Game objects
    val paddle = Paddle()
    private val balls = mutableListOf<Ball>()
    private val bricks = mutableListOf<Brick>()
    private val powerUps = mutableListOf<PowerUp>()
    private val particles = mutableListOf<ParticleEffect>()

    // Game state
    var score: Int = 0
        private set
    var lives: Int = 3
        private set
    var isPaused: Boolean = false
    var isGameOver: Boolean = false
        private set
    var isLevelComplete: Boolean = false
        private set

    // Screen dimensions
    private var screenWidth: Float = 0f
    private var screenHeight: Float = 0f

    // Status bar offset so HUD doesn't overlap system UI
    var statusBarHeight: Float = 80f

    // Level info
    private var currentLevel: LevelData? = null
    private var totalBreakableBricks: Int = 0
    private var levelStartTime: Long = 0L

    // Scoring
    private var comboMultiplier: Float = 1.0f
    private var lastBrickHitTime: Long = 0L
    private var comboPulseTime: Long = 0L
    private val comboResetMs = 2000L
    private val maxCombo = 5.0f

    // Power-up timers
    private data class ActivePowerUp(val type: PowerUp.Type, val startTime: Long, val endTime: Long)
    private val activePowerUps = mutableListOf<ActivePowerUp>()

    // Paints
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 44f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255)
        textSize = 22f
        letterSpacing = 0.1f
    }

    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val lifeBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val lifeBallGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255)
        style = Paint.Style.FILL
    }

    private val powerUpLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val powerUpBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(100, 0, 0, 0)
        style = Paint.Style.FILL
    }

    private val powerUpBarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val overlayPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
    }

    private val overlayTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 56f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val overlaySubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // Callbacks
    var onLevelComplete: ((score: Int) -> Unit)? = null
    var onGameOver: ((score: Int) -> Unit)? = null

    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        paddle.init(width, height)
    }

    fun loadLevel(level: LevelData) {
        currentLevel = level
        bricks.clear()
        balls.clear()
        powerUps.clear()
        particles.clear()
        activePowerUps.clear()

        score = 0
        lives = 3
        isGameOver = false
        isLevelComplete = false
        isPaused = false
        comboMultiplier = 1.0f
        lastBrickHitTime = 0L
        comboPulseTime = 0L
        levelStartTime = System.currentTimeMillis()

        paddle.reset(screenWidth, screenHeight)

        // Grid offset — leave room for large title at top
        val gridTop = statusBarHeight + 140f
        val gridHeight = screenHeight * 0.38f
        val brickWidth = screenWidth / level.cols
        val brickHeight = gridHeight / level.rows

        for (row in level.grid.indices) {
            for (col in level.grid[row].indices) {
                val brickData = level.grid[row][col]
                if (brickData.type != 0) {
                    val brick = Brick(
                        row = row,
                        col = col,
                        type = brickData.type,
                        color = Brick.ROW_COLORS[row % Brick.ROW_COLORS.size]
                    )
                    brick.calculateBounds(0f, gridTop, brickWidth, brickHeight)
                    bricks.add(brick)
                }
            }
        }

        totalBreakableBricks = bricks.count { it.type in 1..2 }

        val ball = Ball(radius = 12f, speed = 10f * level.ballSpeedMultiplier)
        balls.add(ball)
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

            val slowMotionActive = activePowerUps.any { it.type == PowerUp.Type.SLOW_MOTION && now < it.endTime }
            if (slowMotionActive) {
                ball.x += ball.dx * 0.5f
                ball.y += ball.dy * 0.5f
            } else {
                ball.update()
            }

            if (ball.handleWallCollisions(screenWidth, screenHeight)) {
                ballsToRemove.add(ball)
            }

            if (ball.handlePaddleCollision(paddle)) {
                resetCombo()
            }

            val collision = CollisionDetector.checkBallBrickCollision(ball, bricks)
            if (collision != null) {
                val destroyed = CollisionDetector.applyCollision(ball, collision)
                if (destroyed) {
                    registerBrickHit(now)
                    val points = (collision.brick.getPoints() * comboMultiplier).toInt()
                    score += points

                    particles.add(
                        ParticleEffect(
                            collision.brick.rect.centerX(),
                            collision.brick.rect.centerY(),
                            collision.brick.color
                        )
                    )

                    PowerUp.maybeSpawn(
                        collision.brick.rect.centerX(),
                        collision.brick.rect.centerY()
                    )?.let { powerUps.add(it) }
                }
            }
        }

        balls.removeAll(ballsToRemove)

        if (balls.none { it.isActive }) {
            lives--
            resetCombo()
            if (lives <= 0) {
                isGameOver = true
                onGameOver?.invoke(score)
            } else {
                val newBall = Ball(
                    radius = 12f,
                    speed = 10f * (currentLevel?.ballSpeedMultiplier ?: 1.0f)
                )
                balls.add(newBall)
            }
        }

        val powerUpsToRemove = mutableListOf<PowerUp>()
        for (pu in powerUps) {
            if (pu.update(screenHeight)) {
                powerUpsToRemove.add(pu)
                continue
            }
            if (pu.checkPaddleCollision(paddle)) {
                applyPowerUp(pu, now)
                powerUpsToRemove.add(pu)
            }
        }
        powerUps.removeAll(powerUpsToRemove)

        particles.forEach { it.update() }
        particles.removeAll { it.isFinished }

        val remainingBreakable = bricks.count { it.isAlive && it.type in 1..2 }
        if (remainingBreakable == 0 && totalBreakableBricks > 0) {
            val elapsedSeconds = (now - levelStartTime) / 1000L
            val speedBonus = maxOf(0L, 5000L - elapsedSeconds * 50L).toInt()
            val lifeBonus = lives * 1000
            score += speedBonus + lifeBonus

            isLevelComplete = true
            onLevelComplete?.invoke(score)
        }
    }

    // -- Combo handling -------------------------------------------------------

    private fun registerBrickHit(now: Long) {
        comboMultiplier = if (lastBrickHitTime != 0L && (now - lastBrickHitTime) <= comboResetMs) {
            (comboMultiplier + 0.1f).coerceAtMost(maxCombo)
        } else {
            1.1f
        }
        lastBrickHitTime = now
        comboPulseTime = now
    }

    private fun checkComboTimeout(now: Long) {
        if (lastBrickHitTime != 0L && (now - lastBrickHitTime) > comboResetMs) {
            resetCombo()
        }
    }

    private fun resetCombo() {
        comboMultiplier = 1.0f
        lastBrickHitTime = 0L
    }

    // -- Power-up handling ----------------------------------------------------

    private fun applyPowerUp(powerUp: PowerUp, now: Long) {
        when (powerUp.type) {
            PowerUp.Type.MULTI_BALL -> {
                val activeBalls = balls.filter { it.isActive }.take(1)
                for (ball in activeBalls) {
                    balls.add(ball.clone())
                    balls.add(ball.clone().also { it.dx = -it.dx })
                }
            }
            PowerUp.Type.WIDE_PADDLE -> {
                paddle.isWide = true
                addOrRefreshPowerUp(powerUp.type, now)
            }
            PowerUp.Type.FIREBALL -> {
                balls.forEach { it.isFireball = true }
                addOrRefreshPowerUp(powerUp.type, now)
            }
            PowerUp.Type.SLOW_MOTION -> {
                addOrRefreshPowerUp(powerUp.type, now)
            }
        }
    }

    private fun addOrRefreshPowerUp(type: PowerUp.Type, now: Long) {
        activePowerUps.removeAll { it.type == type }
        activePowerUps.add(ActivePowerUp(type, now, now + type.durationMs))
    }

    private fun checkPowerUpTimers(now: Long) {
        val expired = activePowerUps.filter { now >= it.endTime }
        for (exp in expired) {
            when (exp.type) {
                PowerUp.Type.WIDE_PADDLE -> paddle.isWide = false
                PowerUp.Type.FIREBALL -> balls.forEach { it.isFireball = false }
                PowerUp.Type.SLOW_MOTION -> { }
                PowerUp.Type.MULTI_BALL -> { }
            }
        }
        activePowerUps.removeAll(expired)
    }

    fun launchBall() {
        val primaryBall = balls.firstOrNull() ?: return
        if (!primaryBall.isActive) {
            primaryBall.launch(paddle.x, paddle.bounds.top)
        }
    }

    fun onTouchMove(x: Float) {
        paddle.moveTo(x)
    }

    // -- Drawing --------------------------------------------------------------

    fun draw(canvas: Canvas) {
        canvas.drawColor(Color.rgb(18, 18, 32))

        bricks.forEach { it.draw(canvas) }
        particles.forEach { it.draw(canvas) }
        powerUps.forEach { it.draw(canvas) }
        paddle.draw(canvas)
        balls.forEach { it.draw(canvas) }

        drawTitle(canvas)
        drawSidePowerUps(canvas)
        drawBottomBar(canvas)

        if (isPaused) drawOverlay(canvas, "PAUSED", "Tap to resume")
        if (isGameOver) drawOverlay(canvas, "GAME OVER", "Score: $score")
        if (isLevelComplete) {
            val elapsedSeconds = (System.currentTimeMillis() - levelStartTime) / 1000L
            drawOverlay(canvas, "LEVEL CLEAR!", "Score: $score  •  Time: ${elapsedSeconds}s")
        }

        val primaryBall = balls.firstOrNull()
        if (primaryBall != null && !primaryBall.isActive && !isGameOver && !isLevelComplete) {
            drawOverlay(canvas, "TAP TO LAUNCH", "Move paddle with touch")
        }
    }

    /** Draws the level title at the top center. */
    private fun drawTitle(canvas: Canvas) {
        val titleY = statusBarHeight + 80f
        currentLevel?.let {
            canvas.drawText(it.name, screenWidth / 2f, titleY, titlePaint)
        }
    }

    /**
     * Draws active power-up indicators as a vertical stack on the right
     * side of the screen. Each has a shrinking progress bar.
     */
    private fun drawSidePowerUps(canvas: Canvas) {
        if (activePowerUps.isEmpty()) return

        val now = System.currentTimeMillis()
        val indicatorWidth = 100f
        val indicatorHeight = 36f
        val spacing = 10f
        val rightMargin = 16f
        val startX = screenWidth - indicatorWidth - rightMargin
        // Start stacking from the middle of the screen downward
        var y = screenHeight * 0.45f

        for (active in activePowerUps) {
            val progress = ((active.endTime - now).toFloat() / active.type.durationMs.toFloat())
                .coerceIn(0f, 1f)

            // Background capsule
            val bgRect = RectF(startX, y, startX + indicatorWidth, y + indicatorHeight)
            canvas.drawRoundRect(bgRect, 8f, 8f, powerUpBarBgPaint)

            // Shrinking fill bar
            powerUpBarFillPaint.color = active.type.color
            val fillRect = RectF(startX, y, startX + indicatorWidth * progress, y + indicatorHeight)
            canvas.drawRoundRect(fillRect, 8f, 8f, powerUpBarFillPaint)

            // Label
            val label = when (active.type) {
                PowerUp.Type.WIDE_PADDLE -> "WIDE"
                PowerUp.Type.FIREBALL -> "FIRE"
                PowerUp.Type.SLOW_MOTION -> "SLOW"
                else -> ""
            }
            canvas.drawText(label, startX + indicatorWidth / 2f, y + indicatorHeight * 0.7f, powerUpLabelPaint)

            y += indicatorHeight + spacing
        }
    }

    /**
     * Draws the bottom bar below the paddle — score+combo on the left,
     * lives as ball icons on the right.
     */
    private fun drawBottomBar(canvas: Canvas) {
        val bottomY = screenHeight - 50f
        val sideMargin = 24f

        // --- LEFT: Score + combo ---
        canvas.drawText("SCORE", sideMargin, bottomY - 48f, scoreLabelPaint)

        // Pulse the score if we just got a combo hit
        val timeSincePulse = System.currentTimeMillis() - comboPulseTime
        val scoreScale = if (timeSincePulse < 200 && comboMultiplier > 1.0f) {
            1.0f + (200 - timeSincePulse) / 2000f
        } else 1.0f
        scorePaint.textSize = 44f * scoreScale
        canvas.drawText("$score", sideMargin, bottomY - 10f, scorePaint)

        // Combo multiplier under score
        if (comboMultiplier > 1.0f) {
            val multiplierText = "x%.1f combo".format(comboMultiplier)
            comboPaint.color = colorForCombo(comboMultiplier)
            val scoreWidth = scorePaint.measureText("$score")
            canvas.drawText(multiplierText, sideMargin + scoreWidth + 16f, bottomY - 16f, comboPaint)
        }

        // --- RIGHT: Lives as balls ---
        val ballRadius = 14f
        val ballSpacing = 10f
        val totalLivesWidth = (lives * (ballRadius * 2f)) + ((lives - 1).coerceAtLeast(0) * ballSpacing)
        var ballX = screenWidth - sideMargin - totalLivesWidth + ballRadius
        val ballY = bottomY - 20f

        for (i in 0 until lives) {
            canvas.drawCircle(ballX, ballY, ballRadius * 1.6f, lifeBallGlowPaint)
            canvas.drawCircle(ballX, ballY, ballRadius, lifeBallPaint)
            ballX += ballRadius * 2f + ballSpacing
        }
    }

    private fun colorForCombo(mult: Float): Int = when {
        mult >= 4.0f -> Color.rgb(244, 67, 54)
        mult >= 3.0f -> Color.rgb(255, 152, 0)
        mult >= 2.0f -> Color.rgb(255, 235, 59)
        else -> Color.WHITE
    }

    private fun drawOverlay(canvas: Canvas, title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        canvas.drawText(title, screenWidth / 2f, screenHeight / 2f - 20f, overlayTextPaint)
        canvas.drawText(subtitle, screenWidth / 2f, screenHeight / 2f + 40f, overlaySubPaint)
    }
}