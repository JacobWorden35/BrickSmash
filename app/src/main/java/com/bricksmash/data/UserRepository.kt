/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.data

import com.bricksmash.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for user authentication and profile management.
 * Wraps Firebase Auth and Firestore user document operations.
 */
class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Registers a new user with email and password, then sets
     * the display name on both the Auth profile and Firestore document.
     */
    suspend fun register(email: String, password: String, displayName: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Registration failed"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            user.updateProfile(profileUpdates).await()

            val profile = UserProfile(
                uid = user.uid,
                displayName = displayName,
                email = email
            )
            usersCollection.document(user.uid).set(profile.toMap()).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Login failed"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun getUserProfile(): Result<UserProfile> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val doc = usersCollection.document(uid).get().await()
            if (doc.exists()) {
                val profile = UserProfile.fromMap(doc.data ?: emptyMap())
                Result.success(profile)
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the user's display name on both the Firebase Auth
     * profile (for score submissions) and the Firestore user document.
     *
     * Note: existing leaderboard entries are NOT retroactively updated
     * because they capture the name at time of submission.
     */
    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = currentUser ?: return Result.failure(Exception("Not logged in"))
        return try {
            // Update Firebase Auth profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build()
            user.updateProfile(profileUpdates).await()

            // Update Firestore user document
            usersCollection.document(user.uid)
                .update("displayName", newName)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgress(additionalScore: Int, levelCompleted: Int): Result<Unit> {
        val uid = currentUser?.uid ?: return Result.failure(Exception("Not logged in"))
        return try {
            val docRef = usersCollection.document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val currentScore = snapshot.getLong("cumulativeScore") ?: 0
                val currentHighest = snapshot.getLong("highestLevelUnlocked")?.toInt() ?: 1
                val currentCompleted = snapshot.getLong("levelsCompleted")?.toInt() ?: 0

                transaction.update(docRef, mapOf(
                    "cumulativeScore" to currentScore + additionalScore,
                    "levelsCompleted" to currentCompleted + 1,
                    "highestLevelUnlocked" to maxOf(currentHighest, levelCompleted + 1)
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}