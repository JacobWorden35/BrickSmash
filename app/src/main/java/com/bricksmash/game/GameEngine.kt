package com.bricksmash.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.bricksmash.model.BrickData
import com.bricksmash.model.LevelData

/**
 * The main game engine that manages all game state and objects.
 * It is responsible for updating positions, handling collisions,
 * managing power-ups, tracking score/lives, and drawing everything.
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

    // Level info
    private var currentLevel: LevelData? = null
    private var totalBreakableBricks: Int = 0

    // Power-up timers
    private var widePaddleEndTime: Long = 0
    private var fireballEndTime: Long = 0
    private var slowMotionEndTime: Long = 0

    // Paints for HUD
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val hudSmallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(180, 255, 255, 255)
        textSize = 28f
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

    // Callback for game events
    var onLevelComplete: ((score: Int) -> Unit)? = null
    var onGameOver: ((score: Int) -> Unit)? = null

    /**
     * Initializes the game engine with screen dimensions.
     */
    fun init(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        paddle.init(width, height)
    }

    /**
     * Loads a level into the engine, creating brick objects from the LevelData grid.
     */
    fun loadLevel(level: LevelData) {
        currentLevel = level
        bricks.clear()
        balls.clear()
        powerUps.clear()
        particles.clear()

        score = 0
        lives = 3
        isGameOver = false
        isLevelComplete = false
        isPaused = false

        paddle.reset(screenWidth, screenHeight)

        // Create bricks from level data
        val gridTop = 140f  // Space for HUD
        val gridHeight = screenHeight * 0.4f
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

        // Create initial ball sitting on paddle
        val ball = Ball(
            radius = 12f,
            speed = 10f * level.ballSpeedMultiplier
        )
        balls.add(ball)
    }

    /**
     * Main update loop — called every frame by the game thread.
     */
    fun update() {
        if (isPaused || isGameOver || isLevelComplete) return

        val now = System.currentTimeMillis()
        checkPowerUpTimers(now)

        // If ball not launched yet, attach it to paddle
        val primaryBall = balls.firstOrNull() ?: return
        if (!primaryBall.isActive) {
            primaryBall.x = paddle.x
            primaryBall.y = paddle.bounds.top - primaryBall.radius - 2f
            return
        }

        // Update all balls
        val ballsToRemove = mutableListOf<Ball>()
        for (ball in balls) {
            if (!ball.isActive) continue

            // Apply slow motion
            if (now < slowMotionEndTime) {
                ball.x += ball.dx * 0.5f
                ball.y += ball.dy * 0.5f
            } else {
                ball.update()
            }

            // Wall collisions
            if (ball.handleWallCollisions(screenWidth, screenHeight)) {
                ballsToRemove.add(ball)
            }

            // Paddle collision
            ball.handlePaddleCollision(paddle)

            // Brick collisions
            val collision = CollisionDetector.checkBallBrickCollision(ball, bricks)
            if (collision != null) {
                val destroyed = CollisionDetector.applyCollision(ball, collision)
                if (destroyed) {
                    score += collision.brick.getPoints()

                    // Particle effect
                    particles.add(
                        ParticleEffect(
                            collision.brick.rect.centerX(),
                            collision.brick.rect.centerY(),
                            collision.brick.color
                        )
                    )

                    // Maybe spawn power-up
                    PowerUp.maybeSpawn(
                        collision.brick.rect.centerX(),
                        collision.brick.rect.centerY()
                    )?.let { powerUps.add(it) }
                }
            }
        }

        // Remove lost balls
        balls.removeAll(ballsToRemove)

        // If all balls are gone, lose a life
        if (balls.none { it.isActive }) {
            lives--
            if (lives <= 0) {
                isGameOver = true
                onGameOver?.invoke(score)
            } else {
                // Spawn a new ball on the paddle
                val newBall = Ball(
                    radius = 12f,
                    speed = 10f * (currentLevel?.ballSpeedMultiplier ?: 1.0f)
                )
                balls.add(newBall)
            }
        }

        // Update power-ups
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

        // Update particles
        particles.forEach { it.update() }
        particles.removeAll { it.isFinished }

        // Check level completion
        val remainingBreakable = bricks.count { it.isAlive && it.type in 1..2 }
        if (remainingBreakable == 0 && totalBreakableBricks > 0) {
            isLevelComplete = true
            onLevelComplete?.invoke(score)
        }
    }

    /**
     * Applies a collected power-up effect.
     */
    private fun applyPowerUp(powerUp: PowerUp, now: Long) {
        when (powerUp.type) {
            PowerUp.Type.MULTI_BALL -> {
                // Clone existing active balls (up to 2 new balls)
                val activeBalls = balls.filter { it.isActive }.take(1)
                for (ball in activeBalls) {
                    balls.add(ball.clone())
                    balls.add(ball.clone().also { it.dx = -it.dx })
                }
            }
            PowerUp.Type.WIDE_PADDLE -> {
                paddle.isWide = true
                widePaddleEndTime = now + powerUp.type.durationMs
            }
            PowerUp.Type.FIREBALL -> {
                balls.forEach { it.isFireball = true }
                fireballEndTime = now + powerUp.type.durationMs
            }
            PowerUp.Type.SLOW_MOTION -> {
                slowMotionEndTime = now + powerUp.type.durationMs
            }
        }
    }

    /**
     * Checks if timed power-ups have expired.
     */
    private fun checkPowerUpTimers(now: Long) {
        if (now >= widePaddleEndTime && paddle.isWide) {
            paddle.isWide = false
        }
        if (now >= fireballEndTime) {
            balls.forEach { it.isFireball = false }
        }
    }

    /**
     * Launches the ball from the paddle (called on first tap).
     */
    fun launchBall() {
        val primaryBall = balls.firstOrNull() ?: return
        if (!primaryBall.isActive) {
            primaryBall.launch(paddle.x, paddle.bounds.top)
        }
    }

    /**
     * Handles touch events for paddle movement.
     */
    fun onTouchMove(x: Float) {
        paddle.moveTo(x)
    }

    /**
     * Draws all game objects and the HUD.
     */
    fun draw(canvas: Canvas) {
        // Background
        canvas.drawColor(Color.rgb(18, 18, 32))

        // Draw bricks
        bricks.forEach { it.draw(canvas) }

        // Draw particles
        particles.forEach { it.draw(canvas) }

        // Draw power-ups
        powerUps.forEach { it.draw(canvas) }

        // Draw paddle
        paddle.draw(canvas)

        // Draw balls
        balls.forEach { it.draw(canvas) }

        // Draw HUD
        drawHUD(canvas)

        // Draw overlays
        if (isPaused) drawOverlay(canvas, "PAUSED", "Tap to resume")
        if (isGameOver) drawOverlay(canvas, "GAME OVER", "Score: $score")
        if (isLevelComplete) drawOverlay(canvas, "LEVEL CLEAR!", "Score: $score")

        // Draw "Tap to launch" hint
        val primaryBall = balls.firstOrNull()
        if (primaryBall != null && !primaryBall.isActive && !isGameOver && !isLevelComplete) {
            drawOverlay(canvas, "TAP TO LAUNCH", "Move paddle with touch")
        }
    }

    private fun drawHUD(canvas: Canvas) {
        // Score
        canvas.drawText("Score: $score", 20f, 60f, hudPaint)

        // Lives
        val livesText = "Lives: $lives"
        val livesWidth = hudPaint.measureText(livesText)
        canvas.drawText(livesText, screenWidth - livesWidth - 20f, 60f, hudPaint)

        // Level name
        currentLevel?.let {
            canvas.drawText(it.name, 20f, 100f, hudSmallPaint)
        }

        // Active power-up indicators
        val now = System.currentTimeMillis()
        var indicatorX = screenWidth - 20f
        if (paddle.isWide && now < widePaddleEndTime) {
            val text = "WIDE"
            indicatorX -= hudSmallPaint.measureText(text)
            hudSmallPaint.color = PowerUp.Type.WIDE_PADDLE.color
            canvas.drawText(text, indicatorX, 100f, hudSmallPaint)
            indicatorX -= 16f
        }
        if (now < fireballEndTime) {
            val text = "FIRE"
            indicatorX -= hudSmallPaint.measureText(text)
            hudSmallPaint.color = PowerUp.Type.FIREBALL.color
            canvas.drawText(text, indicatorX, 100f, hudSmallPaint)
            indicatorX -= 16f
        }
        if (now < slowMotionEndTime) {
            val text = "SLOW"
            indicatorX -= hudSmallPaint.measureText(text)
            hudSmallPaint.color = PowerUp.Type.SLOW_MOTION.color
            canvas.drawText(text, indicatorX, 100f, hudSmallPaint)
        }
        hudSmallPaint.color = Color.argb(180, 255, 255, 255) // reset
    }

    private fun drawOverlay(canvas: Canvas, title: String, subtitle: String) {
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)
        canvas.drawText(title, screenWidth / 2f, screenHeight / 2f - 20f, overlayTextPaint)
        canvas.drawText(subtitle, screenWidth / 2f, screenHeight / 2f + 40f, overlaySubPaint)
    }
}
