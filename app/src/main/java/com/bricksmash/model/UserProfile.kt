package com.bricksmash.model

/**
 * Represents a user's profile stored in Firestore.
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "Player",
    val email: String = "",
    val avatarUrl: String = "",
    val cumulativeScore: Long = 0,
    val levelsCompleted: Int = 0,
    val highestLevelUnlocked: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid" to uid,
        "displayName" to displayName,
        "email" to email,
        "avatarUrl" to avatarUrl,
        "cumulativeScore" to cumulativeScore,
        "levelsCompleted" to levelsCompleted,
        "highestLevelUnlocked" to highestLevelUnlocked,
        "createdAt" to createdAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): UserProfile {
            return UserProfile(
                uid = map["uid"] as? String ?: "",
                displayName = map["displayName"] as? String ?: "Player",
                email = map["email"] as? String ?: "",
                avatarUrl = map["avatarUrl"] as? String ?: "",
                cumulativeScore = map["cumulativeScore"] as? Long ?: 0,
                levelsCompleted = (map["levelsCompleted"] as? Long)?.toInt() ?: 0,
                highestLevelUnlocked = (map["highestLevelUnlocked"] as? Long)?.toInt() ?: 1,
                createdAt = map["createdAt"] as? Long ?: 0L
            )
        }
    }
}
