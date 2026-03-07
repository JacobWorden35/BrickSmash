package com.bricksmash.model

/**
 * Represents a score entry on the leaderboard.
 * Used for both per-level scores and cumulative rankings.
 */
data class ScoreEntry(
    val odcId: String = "",
    val userId: String = "",
    val displayName: String = "Anonymous",
    val score: Int = 0,
    val levelId: String = "",
    val levelName: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Converts this ScoreEntry to a Map for Firestore writes.
     */
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName,
        "score" to score,
        "levelId" to levelId,
        "levelName" to levelName,
        "timestamp" to timestamp
    )

    companion object {
        /**
         * Creates a ScoreEntry from a Firestore document snapshot map.
         */
        fun fromMap(docId: String, map: Map<String, Any?>): ScoreEntry {
            return ScoreEntry(
                odcId = docId,
                userId = map["userId"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "Anonymous",
                score = (map["score"] as? Long)?.toInt() ?: 0,
                levelId = map["levelId"] as? String ?: "",
                levelName = map["levelName"] as? String ?: "",
                timestamp = map["timestamp"] as? Long ?: 0L
            )
        }
    }
}
