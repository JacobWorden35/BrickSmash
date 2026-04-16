package com.bricksmash.data

import com.bricksmash.model.BrickData
import com.bricksmash.model.LevelData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for community level operations.
 * Handles publishing, browsing, and downloading user-created levels.
 *
 * Grid storage: The 2D brick grid is flattened to a 1D list for Firestore
 * (Firestore does not support nested arrays). Rows/cols metadata is used
 * to reconstruct the 2D grid on read.
 */
class LevelRepository {

    private val db = FirebaseFirestore.getInstance()
    private val levelsCollection = db.collection("community_levels")
    private val auth = FirebaseAuth.getInstance()

    /**
     * Publishes a user-created level to the community library.
     * Flattens the 2D grid into a 1D list to comply with Firestore's
     * restriction against nested arrays.
     */
    suspend fun publishLevel(level: LevelData): Result<String> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

        return try {
            // Flatten 2D grid to 1D list — Firestore does not support nested arrays
            val flatGrid = level.grid.flatMap { row ->
                row.map { brick -> mapOf("type" to brick.type, "color" to brick.color) }
            }

            val levelMap = mapOf(
                "name" to level.name,
                "author" to (user.displayName ?: "Anonymous"),
                "authorId" to user.uid,
                "difficulty" to level.difficulty,
                "rows" to level.rows,
                "cols" to level.cols,
                "grid" to flatGrid,
                "targetScore" to level.targetScore,
                "ballSpeedMultiplier" to level.ballSpeedMultiplier,
                "playCount" to 0,
                "rating" to 0f,
                "timestamp" to System.currentTimeMillis()
            )

            val docRef = levelsCollection.add(levelMap).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches community levels sorted by the given criteria.
     * Reconstructs the 2D grid from the flattened 1D list using rows/cols.
     */
    suspend fun getCommunityLevels(
        sortBy: SortOption = SortOption.NEWEST,
        limit: Long = 20
    ): Result<List<LevelData>> {
        return try {
            val query = when (sortBy) {
                SortOption.NEWEST -> levelsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
                SortOption.MOST_PLAYED -> levelsCollection.orderBy("playCount", Query.Direction.DESCENDING)
                SortOption.TOP_RATED -> levelsCollection.orderBy("rating", Query.Direction.DESCENDING)
            }.limit(limit)

            val snapshot = query.get().await()

            val levels = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val rows = (data["rows"] as? Long)?.toInt() ?: 8
                    val cols = (data["cols"] as? Long)?.toInt() ?: 10

                    // Read flat grid and chunk back into 2D
                    @Suppress("UNCHECKED_CAST")
                    val flatGrid = data["grid"] as? List<Map<String, Any>> ?: return@mapNotNull null

                    val brickList = flatGrid.map { brick ->
                        BrickData(
                            type = (brick["type"] as? Long)?.toInt() ?: 0,
                            color = brick["color"] as? String ?: "#FF5722"
                        )
                    }
                    val grid = brickList.chunked(cols)

                    LevelData(
                        id = doc.id,
                        name = data["name"] as? String ?: "Untitled",
                        author = data["author"] as? String ?: "Unknown",
                        difficulty = (data["difficulty"] as? Long)?.toInt() ?: 1,
                        rows = rows,
                        cols = cols,
                        grid = grid,
                        targetScore = (data["targetScore"] as? Long)?.toInt() ?: 1000,
                        ballSpeedMultiplier = (data["ballSpeedMultiplier"] as? Double)?.toFloat() ?: 1.0f,
                        isBuiltIn = false,
                        isCommunity = true,
                        playCount = (data["playCount"] as? Long)?.toInt() ?: 0,
                        rating = (data["rating"] as? Double)?.toFloat() ?: 0f
                    )
                } catch (e: Exception) {
                    null
                }
            }

            Result.success(levels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Increments the play count for a community level.
     */
    suspend fun incrementPlayCount(levelId: String) {
        try {
            val docRef = levelsCollection.document(levelId)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentCount = snapshot.getLong("playCount") ?: 0
                transaction.update(docRef, "playCount", currentCount + 1)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class SortOption { NEWEST, MOST_PLAYED, TOP_RATED }
}