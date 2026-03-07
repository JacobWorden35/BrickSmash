package com.bricksmash.game

import android.content.Context
import com.bricksmash.model.LevelData
import kotlinx.serialization.json.Json

/**
 * Manages loading levels from local assets (built-in) and
 * from Firestore (community levels).
 */
class LevelManager(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val builtInLevels = mutableListOf<LevelData>()

    /**
     * Loads all built-in levels from the assets/levels/ directory.
     */
    fun loadBuiltInLevels(): List<LevelData> {
        if (builtInLevels.isNotEmpty()) return builtInLevels

        try {
            val levelFiles = context.assets.list("levels") ?: emptyArray()
            for (fileName in levelFiles.sorted()) {
                if (fileName.endsWith(".json")) {
                    val jsonString = context.assets.open("levels/$fileName")
                        .bufferedReader().use { it.readText() }
                    val level = json.decodeFromString<LevelData>(jsonString)
                    builtInLevels.add(level)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // If loading fails, generate default levels
            if (builtInLevels.isEmpty()) {
                builtInLevels.addAll(generateDefaultLevels())
            }
        }

        if (builtInLevels.isEmpty()) {
            builtInLevels.addAll(generateDefaultLevels())
        }

        return builtInLevels
    }

    /**
     * Returns a specific level by index (0-based).
     */
    fun getLevel(index: Int): LevelData? {
        val levels = loadBuiltInLevels()
        return levels.getOrNull(index)
    }

    companion object {
        /**
         * Generates a set of default levels programmatically as a fallback.
         */
        fun generateDefaultLevels(): List<LevelData> {
            return listOf(
                generateLevel(1, "Getting Started", 6, 8, 1.0f, 500),
                generateLevel(2, "Warming Up", 7, 9, 1.0f, 800),
                generateLevel(3, "Brick Wall", 8, 10, 1.1f, 1000),
                generateLevel(4, "Checkerboard", 8, 10, 1.1f, 1200),
                generateLevel(5, "Diamond", 8, 10, 1.2f, 1500),
                generateLevel(6, "Pyramid", 8, 10, 1.2f, 1800),
                generateLevel(7, "Fortress", 8, 10, 1.3f, 2000),
                generateLevel(8, "The Gauntlet", 8, 10, 1.3f, 2500),
                generateLevel(9, "Hardened", 8, 10, 1.4f, 3000),
                generateLevel(10, "Indestructible Maze", 8, 10, 1.4f, 3500),
                generateLevel(11, "Speed Run", 6, 10, 1.6f, 2000),
                generateLevel(12, "Dense Pack", 10, 10, 1.3f, 4000),
                generateLevel(13, "Spiral", 8, 10, 1.5f, 3500),
                generateLevel(14, "Chaos", 10, 10, 1.5f, 5000),
                generateLevel(15, "Final Boss", 10, 10, 1.7f, 6000)
            )
        }

        private fun generateLevel(
            id: Int,
            name: String,
            rows: Int,
            cols: Int,
            speedMult: Float,
            targetScore: Int
        ): LevelData {
            val grid = generateGrid(id, rows, cols)
            return LevelData(
                id = "builtin_$id",
                name = name,
                author = "BrickSmash",
                difficulty = ((id - 1) / 3 + 1).coerceAtMost(5),
                rows = rows,
                cols = cols,
                grid = grid,
                targetScore = targetScore,
                ballSpeedMultiplier = speedMult,
                isBuiltIn = true
            )
        }

        private fun generateGrid(
            levelId: Int,
            rows: Int,
            cols: Int
        ): List<List<com.bricksmash.model.BrickData>> {
            return when (levelId) {
                1 -> {
                    // Simple rows of normal bricks in the top half
                    List(rows) { row ->
                        List(cols) { _ ->
                            if (row < rows / 2) com.bricksmash.model.BrickData(type = 1)
                            else com.bricksmash.model.BrickData(type = 0)
                        }
                    }
                }
                2 -> {
                    // More rows filled
                    List(rows) { row ->
                        List(cols) { _ ->
                            if (row < rows - 1) com.bricksmash.model.BrickData(type = 1)
                            else com.bricksmash.model.BrickData(type = 0)
                        }
                    }
                }
                3 -> {
                    // Full wall
                    List(rows) { _ ->
                        List(cols) { _ ->
                            com.bricksmash.model.BrickData(type = 1)
                        }
                    }
                }
                4 -> {
                    // Checkerboard pattern
                    List(rows) { row ->
                        List(cols) { col ->
                            if ((row + col) % 2 == 0) com.bricksmash.model.BrickData(type = 1)
                            else com.bricksmash.model.BrickData(type = 0)
                        }
                    }
                }
                5 -> {
                    // Diamond shape
                    val centerR = rows / 2
                    val centerC = cols / 2
                    List(rows) { row ->
                        List(cols) { col ->
                            val dist = Math.abs(row - centerR) + Math.abs(col - centerC)
                            if (dist <= (rows / 2)) com.bricksmash.model.BrickData(type = 1)
                            else com.bricksmash.model.BrickData(type = 0)
                        }
                    }
                }
                6 -> {
                    // Pyramid
                    List(rows) { row ->
                        List(cols) { col ->
                            val margin = row
                            if (col in margin until (cols - margin))
                                com.bricksmash.model.BrickData(type = 1)
                            else com.bricksmash.model.BrickData(type = 0)
                        }
                    }
                }
                7 -> {
                    // Mix of normal and hardened
                    List(rows) { row ->
                        List(cols) { col ->
                            if (row == 0 || row == rows - 1 || col == 0 || col == cols - 1)
                                com.bricksmash.model.BrickData(type = 2)
                            else com.bricksmash.model.BrickData(type = 1)
                        }
                    }
                }
                10 -> {
                    // With indestructible bricks creating a maze
                    List(rows) { row ->
                        List(cols) { col ->
                            when {
                                (row == 3 || row == 6) && col % 3 == 0 ->
                                    com.bricksmash.model.BrickData(type = 3)
                                else -> com.bricksmash.model.BrickData(type = 1)
                            }
                        }
                    }
                }
                else -> {
                    // Generic mixed level, more hardened bricks at higher levels
                    val hardenedChance = (levelId * 0.05).coerceAtMost(0.4)
                    val indestructibleChance = if (levelId > 9) 0.08 else 0.0
                    List(rows) { _ ->
                        List(cols) { _ ->
                            val rand = Math.random()
                            when {
                                rand < indestructibleChance ->
                                    com.bricksmash.model.BrickData(type = 3)
                                rand < indestructibleChance + hardenedChance ->
                                    com.bricksmash.model.BrickData(type = 2)
                                else -> com.bricksmash.model.BrickData(type = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}
