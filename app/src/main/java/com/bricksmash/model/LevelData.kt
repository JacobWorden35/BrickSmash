package com.bricksmash.model

import kotlinx.serialization.Serializable

/**
 * Represents a brick in a level grid.
 * type: 0 = empty, 1 = normal, 2 = hardened (2 hits), 3 = indestructible
 */
@Serializable
data class BrickData(
    val type: Int = 0,
    val color: String = "#FF5722" // Default orange
)

/**
 * Represents a complete level definition.
 * The grid is stored as a 2D list of BrickData objects.
 */
@Serializable
data class LevelData(
    val id: String = "",
    val name: String = "Untitled",
    val author: String = "BrickSmash",
    val difficulty: Int = 1, // 1-5
    val rows: Int = 8,
    val cols: Int = 10,
    val grid: List<List<BrickData>> = emptyList(),
    val targetScore: Int = 1000,
    val ballSpeedMultiplier: Float = 1.0f,
    val isBuiltIn: Boolean = true,
    val isCommunity: Boolean = false,
    val playCount: Int = 0,
    val rating: Float = 0f
) {
    /**
     * Counts the number of breakable bricks in this level.
     */
    fun breakableBrickCount(): Int {
        return grid.sumOf { row ->
            row.count { it.type in 1..2 }
        }
    }
}
