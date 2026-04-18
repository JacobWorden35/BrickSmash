/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.data

import com.bricksmash.model.ScoreEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repository for leaderboard operations.
 * Handles writing scores to Firestore and providing real-time
 * snapshot listeners for live leaderboard updates.
 */
class LeaderboardRepository {

    private val db = FirebaseFirestore.getInstance()
    private val scoresCollection = db.collection("scores")
    private val auth = FirebaseAuth.getInstance()

    private var leaderboardListener: ListenerRegistration? = null

    /**
     * Submits a score to the leaderboard.
     * Only writes if the user is authenticated.
     */
    suspend fun submitScore(score: Int, levelId: String, levelName: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("Not logged in"))

        return try {
            val entry = ScoreEntry(
                userId = user.uid,
                displayName = user.displayName ?: "Anonymous",
                score = score,
                levelId = levelId,
                levelName = levelName,
                timestamp = System.currentTimeMillis()
            )
            scoresCollection.add(entry.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the top scores for a specific level (one-time read).
     */
    suspend fun getTopScoresForLevel(levelId: String, limit: Long = 50): Result<List<ScoreEntry>> {
        return try {
            val snapshot = scoresCollection
                .whereEqualTo("levelId", levelId)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val scores = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { ScoreEntry.fromMap(doc.id, it) }
            }
            Result.success(scores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the global top scores across all levels.
     */
    suspend fun getGlobalTopScores(limit: Long = 50): Result<List<ScoreEntry>> {
        return try {
            val snapshot = scoresCollection
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val scores = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { ScoreEntry.fromMap(doc.id, it) }
            }
            Result.success(scores)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Starts a real-time listener on the leaderboard for live updates.
     * The callback is invoked whenever scores change.
     */
    fun listenToLeaderboard(
        levelId: String? = null,
        limit: Long = 50,
        onUpdate: (List<ScoreEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Remove previous listener
        leaderboardListener?.remove()

        var query: Query = scoresCollection
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(limit)

        if (levelId != null) {
            query = scoresCollection
                .whereEqualTo("levelId", levelId)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(limit)
        }

        leaderboardListener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val scores = snapshot?.documents?.mapNotNull { doc ->
                doc.data?.let { ScoreEntry.fromMap(doc.id, it) }
            } ?: emptyList()

            onUpdate(scores)
        }
    }

    /**
     * Removes the real-time leaderboard listener.
     */
    fun removeListener() {
        leaderboardListener?.remove()
        leaderboardListener = null
    }
}
