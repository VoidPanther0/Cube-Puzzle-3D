package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticsManager
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.data.repository.GameRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class MainViewModel(
    private val repository: GameRepository,
    private val soundAndHaptics: SoundAndHapticsManager,
    private val currentUserId: String,
    private val initialDisplayName: String
) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(initialDisplayName)
        }
    }

    val userProfileState: StateFlow<UiState<UserProfile>> =
        repository.observeUserProfile(currentUserId)
            .onEach { profile ->
                if (profile != null) {
                    soundAndHaptics.soundEnabled = profile.soundEnabled
                    soundAndHaptics.hapticsEnabled = profile.hapticsEnabled
                }
            }
            .map<UserProfile?, UiState<UserProfile>> { profile ->
                if (profile != null) {
                    UiState.Success(profile)
                } else {
                    UiState.Loading
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing user profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load player profile."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val leaderboardState: StateFlow<UiState<List<LeaderboardEntry>>> =
        repository.observeLeaderboard(50L)
            .map<List<LeaderboardEntry>, UiState<List<LeaderboardEntry>>> { entries ->
                UiState.Success(entries)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing leaderboard", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load leaderboard."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    fun updateSettings(
        soundEnabled: Boolean,
        hapticsEnabled: Boolean,
        showLinePreview: Boolean
    ) {
        soundAndHaptics.soundEnabled = soundEnabled
        soundAndHaptics.hapticsEnabled = hapticsEnabled
        viewModelScope.launch {
            repository.updateSettings(
                soundEnabled = soundEnabled,
                hapticsEnabled = hapticsEnabled,
                showLinePreview = showLinePreview
            )
        }
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
