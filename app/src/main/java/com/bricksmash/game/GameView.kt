/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import com.bricksmash.model.LevelData

/**
 * SurfaceView that hosts the game. Handles touch and keyboard input.
 * Arrow keys move the paddle (useful for emulator testing).
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val engine = GameEngine()
    private var gameThread: GameThread? = null
    private var isSurfaceReady = false
    private var pendingLevel: LevelData? = null

    // Keyboard paddle speed (pixels per key event)
    private val keyboardPaddleSpeed = 30f

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        engine.statusBarHeight =
            rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top?.toFloat() ?: 80f
        engine.init(width.toFloat(), height.toFloat())
        pendingLevel?.let {
            engine.loadLevel(it)
            pendingLevel = null
        }
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
        gameThread = GameThread(holder, this).apply { isRunning = true; start() }
    }

    private fun stopThread() {
        gameThread?.let { thread ->
            thread.isRunning = false
            var retry = true
            while (retry) {
                try { thread.join(500); retry = false }
                catch (e: InterruptedException) { e.printStackTrace() }
            }
        }
        gameThread = null
    }

    fun updateAndDraw(canvas: Canvas) {
        engine.update()
        engine.draw(canvas)
    }

    fun loadLevel(level: LevelData) {
        if (isSurfaceReady) engine.loadLevel(level) else pendingLevel = level
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        requestFocus()
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                engine.launchBall()
                engine.onTouchMove(event.x)
                if (engine.isPaused) engine.isPaused = false
            }
            MotionEvent.ACTION_MOVE -> engine.onTouchMove(event.x)
        }
        return true
    }

    /**
     * Keyboard input support — left/right arrows move the paddle.
     * Primarily useful for testing on an emulator without a touch screen.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                engine.movePaddleBy(-keyboardPaddleSpeed); true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                engine.movePaddleBy(keyboardPaddleSpeed); true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    fun pause() { engine.isPaused = true }
    fun resume() {
        if (isSurfaceReady && (gameThread == null || gameThread?.isRunning == false)) startThread()
    }
    fun cleanup() { stopThread() }
}