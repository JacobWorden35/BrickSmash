package com.bricksmash.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bricksmash.model.LevelData

/**
 * Custom SurfaceView that hosts the game.
 * Handles touch input and manages the game thread lifecycle.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val engine = GameEngine()
    private var gameThread: GameThread? = null
    private var isSurfaceReady = false

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        engine.init(width.toFloat(), height.toFloat())
        startThread()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        engine.init(width.toFloat(), height.toFloat())
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        stopThread()
    }

    private fun startThread() {
        gameThread?.isRunning = false
        gameThread = GameThread(holder, this).apply {
            isRunning = true
            start()
        }
    }

    private fun stopThread() {
        gameThread?.let { thread ->
            thread.isRunning = false
            var retry = true
            while (retry) {
                try {
                    thread.join(500)
                    retry = false
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }
        gameThread = null
    }

    /**
     * Called by the GameThread each frame to update logic and render.
     */
    fun updateAndDraw(canvas: Canvas) {
        engine.update()
        engine.draw(canvas)
    }

    /**
     * Loads a level into the game engine and starts gameplay.
     */
    fun loadLevel(level: LevelData) {
        engine.loadLevel(level)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Launch ball if not yet active, otherwise just move paddle
                engine.launchBall()
                engine.onTouchMove(event.x)

                // Resume from pause
                if (engine.isPaused) {
                    engine.isPaused = false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                engine.onTouchMove(event.x)
            }
        }
        return true
    }

    fun pause() {
        engine.isPaused = true
    }

    fun resume() {
        if (isSurfaceReady && (gameThread == null || gameThread?.isRunning == false)) {
            startThread()
        }
    }

    fun cleanup() {
        stopThread()
    }
}
