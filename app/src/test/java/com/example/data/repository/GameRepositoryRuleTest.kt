package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.JewelColor
import com.example.data.model.PieceCatalog
import com.example.data.model.TrayPiece
import com.example.game.PuzzleEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andSaveClassicSession_persistsCorrectly() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore, auth)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Alice").getOrThrow()
        }
        assertEquals(uid, profile.userId)
        assertEquals("Alice", profile.displayName)

        val board = PuzzleEngine.emptyBoard().toMutableList().apply {
            this[0] = JewelColor.RUBY
            this[1] = JewelColor.EMERALD
        }
        val tray = listOf(
            TrayPiece(0, PieceCatalog.allShapes[0], JewelColor.RUBY, false),
            TrayPiece(1, PieceCatalog.allShapes[1], JewelColor.SAPPHIRE, false),
            TrayPiece(2, PieceCatalog.allShapes[2], JewelColor.AMBER, true)
        )
        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveClassicSession(board, 450L, 2L, tray)
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile(uid).first { it != null && it.savedClassicScore == 450L }
        }
        assertNotNull(observed)
        assertEquals(450L, observed?.savedClassicScore)
    }

    @Test
    fun recordGameCompletion_updatesStatsAndLeaderboard() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = GameRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.ensureUserProfile("Alice").getOrThrow()
        }

        val completion = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordGameCompletion(
                displayName = "Alice",
                score = 2150L,
                maxComboInRun = 5L,
                linesClearedInRun = 14L,
                isDailyWin = true,
                dailyDateKey = "2026-11-15"
            )
        }
        assertTrue("recordGameCompletion failed: ${completion.exceptionOrNull()}", completion.isSuccess)

        val leaderboard = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeLeaderboard(20L).first { entries ->
                entries.any { it.userId == uid && it.bestScore >= 2150L }
            }
        }
        assertTrue(leaderboard.any { it.userId == uid && it.dailyWins >= 1L })
    }

    @Test
    fun crossUserProfileAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = GameRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.ensureUserProfile("Alice").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var denied = false
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(aliceUid).get().await()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Expected cross-user profile read to fail with PERMISSION_DENIED", denied)
    }

    @Test
    fun observeLeaderboard_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = GameRepository(firestore, auth)

        var denied = false
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeLeaderboard(10L).first()
            }
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                denied = true
            }
        }
        assertTrue("Expected unauthenticated leaderboard query to fail with PERMISSION_DENIED", denied)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_cube@test.com"
        const val BOB_EMAIL = "bob_cube@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
