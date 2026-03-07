package com.bricksmash.game

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Dedicated game thread that runs the game loop at ~60fps.
 * Uses a fixed timestep approach to ensure consistent game speed.
 */
class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    @Volatile
    var isRunning: Boolean = false

    companion object {
        const val TARGET_FPS = 60
        const val FRAME_PERIOD = (1000.0 / TARGET_FPS).toLong()
    }

    override fun run() {
        var canvas: Canvas?

        while (isRunning) {
            val startTime = System.currentTimeMillis()
            canvas = null

            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.updateAndDraw(canvas)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            // Frame rate limiter
            val elapsed = System.currentTimeMillis() - startTime
            val sleepTime = FRAME_PERIOD - elapsed
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (e: InterruptedException) {
                    // Thread interrupted, exit gracefully
                }
            }
        }
    }
}
