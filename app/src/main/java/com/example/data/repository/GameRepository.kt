package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firestore.OperationType
import com.example.data.firestore.handleFirestoreError
import com.example.data.model.JewelColor
import com.example.data.model.LeaderboardEntry
import com.example.data.model.TrayPiece
import com.example.data.model.UserProfile
import com.example.game.PuzzleEngine
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = FirebaseAuth.getInstance()
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(displayName: String): Result<UserProfile> {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Player" }.take(60)
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid, displayName = safeName)
                Result.success(existing)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "bestScore" to 0L,
                    "highestCombo" to 0L,
                    "gamesPlayed" to 0L,
                    "totalLinesCleared" to 0L,
                    "dailyStreak" to 0L,
                    "dailyWins" to 0L,
                    "lastDailyDate" to "",
                    "soundEnabled" to true,
                    "hapticsEnabled" to true,
                    "showLinePreview" to true,
                    "savedClassicBoard" to emptyList<Int>(),
                    "savedClassicScore" to 0L,
                    "savedClassicCombo" to 0L,
                    "savedClassicTray" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                val created = docRef.get().await().toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfile(id = uid, userId = uid, displayName = safeName)
                Result.success(created)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val docRef = db.collection("users").document(userId)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path, auth)
                }
                throw error
            }
    }

    fun observeLeaderboard(limitCount: Long = 50L): Flow<List<LeaderboardEntry>> {
        val path = "leaderboard"
        val query = db.collection(path)
            .whereEqualTo("isPublic", true)
            .orderBy("bestScore", Query.Direction.DESCENDING)
            .limit(limitCount)
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LeaderboardEntry::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser != null) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path, auth)
                }
                throw error
            }
    }

    suspend fun saveClassicSession(
        board: List<JewelColor?>,
        score: Long,
        combo: Long,
        tray: List<TrayPiece>
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "savedClassicBoard" to PuzzleEngine.boardToInts(board),
            "savedClassicScore" to score.coerceAtLeast(0L),
            "savedClassicCombo" to combo.coerceAtLeast(0L),
            "savedClassicTray" to tray.map { it.encode() },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun clearClassicSession(): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "savedClassicBoard" to emptyList<Int>(),
            "savedClassicScore" to 0L,
            "savedClassicCombo" to 0L,
            "savedClassicTray" to emptyList<String>(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun recordGameCompletion(
        displayName: String,
        score: Long,
        maxComboInRun: Long,
        linesClearedInRun: Long,
        isDailyWin: Boolean,
        dailyDateKey: String? = null
    ): Result<Unit> {
        val uid = requireUserId()
        val safeName = displayName.trim().ifBlank { "Player" }.take(60)
        val userRef = db.collection("users").document(uid)
        val lbRef = db.collection("leaderboard").document(uid)

        return try {
            val snap = userRef.get().await()
            val current = snap.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            val newBest = maxOf(current?.bestScore ?: 0L, score)
            val newCombo = maxOf(current?.highestCombo ?: 0L, maxComboInRun)
            val newGamesPlayed = (current?.gamesPlayed ?: 0L) + 1L
            val newTotalLines = (current?.totalLinesCleared ?: 0L) + linesClearedInRun.coerceAtLeast(0L)

            var newDailyStreak = current?.dailyStreak ?: 0L
            var newDailyWins = current?.dailyWins ?: 0L
            var newLastDailyDate = current?.lastDailyDate ?: ""

            if (isDailyWin && !dailyDateKey.isNullOrBlank() && newLastDailyDate != dailyDateKey) {
                newDailyWins += 1L
                val yesterdayKey = try {
                    LocalDate.parse(dailyDateKey).minusDays(1).toString()
                } catch (_: Exception) {
                    ""
                }
                newDailyStreak = if (newLastDailyDate == yesterdayKey) newDailyStreak + 1L else 1L
                newLastDailyDate = dailyDateKey
            }

            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to safeName,
                "bestScore" to newBest,
                "highestCombo" to newCombo,
                "gamesPlayed" to newGamesPlayed,
                "totalLinesCleared" to newTotalLines,
                "dailyStreak" to newDailyStreak,
                "dailyWins" to newDailyWins,
                "lastDailyDate" to newLastDailyDate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!isDailyWin) {
                userUpdates["savedClassicBoard"] = emptyList<Int>()
                userUpdates["savedClassicScore"] = 0L
                userUpdates["savedClassicCombo"] = 0L
                userUpdates["savedClassicTray"] = emptyList<String>()
            }
            userRef.update(userUpdates).await()

            val lbPayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "bestScore" to newBest,
                "highestCombo" to newCombo,
                "dailyWins" to newDailyWins,
                "isPublic" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            lbRef.set(lbPayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        showLinePreview: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mapOf(
            "soundEnabled" to soundEnabled,
            "hapticsEnabled" to hapticsEnabled,
            "showLinePreview" to showLinePreview,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }
}
