package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundAndHapticsManager
import com.example.data.ai.AiStrategyRepository
import com.example.data.ai.StrategyHint
import com.example.data.model.GameMode
import com.example.data.model.JewelColor
import com.example.data.model.PieceCatalog
import com.example.data.model.TrayPiece
import com.example.data.model.UserProfile
import com.example.data.repository.GameRepository
import com.example.game.ClearedCell
import com.example.game.PuzzleEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class ShatterBurst(
    val id: Long,
    val cells: List<ClearedCell>,
    val pointsEarned: Int,
    val combo: Int,
    val linesCleared: Int
)

data class PlacementPulse(
    val id: Long,
    val cellIndices: Set<Int>,
    val pointsEarned: Int
)

sealed interface AiHintUiState {
    data object Idle : AiHintUiState
    data object Loading : AiHintUiState
    data class Ready(val hint: StrategyHint) : AiHintUiState
    data class Error(val message: String) : AiHintUiState
}

data class GameUiState(
    val mode: GameMode = GameMode.CLASSIC,
    val board: List<JewelColor?> = PuzzleEngine.emptyBoard(),
    val tray: List<TrayPiece> = PuzzleEngine.generateTray(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val combo: Int = 0,
    val maxComboInRun: Int = 0,
    val linesClearedInRun: Int = 0,
    val dailyDateKey: String = PuzzleEngine.todayDateKey(),
    val dailyTargetScore: Int = 1500,
    val dailyCompletedToday: Boolean = false,
    val dailyVictoryJustTriggered: Boolean = false,
    val isGameOver: Boolean = false,
    val newRecordAchieved: Boolean = false,
    val shatterBurst: ShatterBurst? = null,
    val placementPulse: PlacementPulse? = null,
    val invalidShakeNonce: Long = 0L,
    val aiHintState: AiHintUiState = AiHintUiState.Idle,
    val showLinePreview: Boolean = true
)

class GameViewModel(
    private val repository: GameRepository,
    private val aiRepository: AiStrategyRepository,
    private val soundAndHaptics: SoundAndHapticsManager,
    private val playerDisplayName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var dailyRandom: Random = Random.Default
    private var effectCounter: Long = 0L
    private var hasInitializedMode: GameMode? = null

    fun initializeGame(mode: GameMode, profile: UserProfile?, forceRestart: Boolean = false) {
        if (!forceRestart && hasInitializedMode == mode && !_uiState.value.isGameOver) {
            // Sync latest bestScore and preview preference without resetting active board
            _uiState.update {
                it.copy(
                    bestScore = maxOf(it.bestScore, (profile?.bestScore ?: 0L).toInt()),
                    showLinePreview = profile?.showLinePreview ?: it.showLinePreview
                )
            }
            return
        }
        hasInitializedMode = mode

        val bestScore = (profile?.bestScore ?: 0L).toInt()
        val showPreview = profile?.showLinePreview ?: true
        val today = PuzzleEngine.todayDateKey()

        if (mode == GameMode.CLASSIC) {
            val savedBoardInts = profile?.savedClassicBoard.orEmpty()
            val savedTray = profile?.savedClassicTray?.let { PieceCatalog.decodeTray(it) }
            if (!forceRestart && savedBoardInts.size == 64 && savedTray != null) {
                val restoredBoard = PuzzleEngine.intsToBoard(savedBoardInts)
                if (PuzzleEngine.hasAnyValidMove(restoredBoard, savedTray)) {
                    _uiState.value = GameUiState(
                        mode = GameMode.CLASSIC,
                        board = restoredBoard,
                        tray = savedTray,
                        score = (profile?.savedClassicScore ?: 0L).toInt(),
                        bestScore = bestScore,
                        combo = (profile?.savedClassicCombo ?: 0L).toInt(),
                        maxComboInRun = (profile?.savedClassicCombo ?: 0L).toInt(),
                        linesClearedInRun = 0,
                        isGameOver = false,
                        showLinePreview = showPreview
                    )
                    return
                }
            }

            val freshBoard = PuzzleEngine.emptyBoard()
            val freshTray = PuzzleEngine.generateTray(Random.Default, freshBoard)
            _uiState.value = GameUiState(
                mode = GameMode.CLASSIC,
                board = freshBoard,
                tray = freshTray,
                score = 0,
                bestScore = bestScore,
                combo = 0,
                maxComboInRun = 0,
                linesClearedInRun = 0,
                isGameOver = false,
                showLinePreview = showPreview
            )
            if (forceRestart) {
                viewModelScope.launch { repository.clearClassicSession() }
            }
        } else {
            val seed = PuzzleEngine.createDailyChallenge(today)
            dailyRandom = Random(seed.seedValue + 101L)
            val alreadyCompleted = profile?.lastDailyDate == today
            _uiState.value = GameUiState(
                mode = GameMode.DAILY,
                board = seed.initialBoard,
                tray = seed.initialTray,
                score = 0,
                bestScore = bestScore,
                combo = 0,
                maxComboInRun = 0,
                linesClearedInRun = 0,
                dailyDateKey = seed.dateKey,
                dailyTargetScore = seed.targetScore,
                dailyCompletedToday = alreadyCompleted,
                dailyVictoryJustTriggered = false,
                isGameOver = false,
                showLinePreview = showPreview
            )
        }
    }

    fun onPiecePickup() {
        soundAndHaptics.onPiecePickedUp()
    }

    fun attemptPlacePiece(slotIndex: Int, anchorRow: Int, anchorCol: Int): Boolean {
        val current = _uiState.value
        if (current.isGameOver) return false
        val piece = current.tray.firstOrNull { it.slotIndex == slotIndex && !it.isPlaced } ?: return false

        val outcome = PuzzleEngine.placePiece(
            board = current.board,
            shape = piece.shape,
            color = piece.color,
            anchorRow = anchorRow,
            anchorCol = anchorCol,
            currentCombo = current.combo
        )

        if (outcome == null) {
            soundAndHaptics.onInvalidPlacement()
            _uiState.update { it.copy(invalidShakeNonce = ++effectCounter) }
            return false
        }

        soundAndHaptics.onPiecePlaced(outcome.linesCleared, outcome.newCombo)

        val placedIndices = piece.shape.cells.map { offset ->
            PuzzleEngine.indexOf(anchorRow + offset.row, anchorCol + offset.col)
        }.toSet()

        val updatedScore = current.score + outcome.pointsEarned
        val updatedMaxCombo = maxOf(current.maxComboInRun, outcome.newCombo)
        val updatedLines = current.linesClearedInRun + outcome.linesCleared
        val isNewRecord = updatedScore > current.bestScore
        val updatedBest = maxOf(current.bestScore, updatedScore)

        var nextTray = current.tray.map {
            if (it.slotIndex == slotIndex) it.copy(isPlaced = true) else it
        }
        if (nextTray.all { it.isPlaced }) {
            val rng = if (current.mode == GameMode.DAILY) dailyRandom else Random.Default
            nextTray = PuzzleEngine.generateTray(rng, outcome.updatedBoard)
        }

        val noMovesLeft = !PuzzleEngine.hasAnyValidMove(outcome.updatedBoard, nextTray)
        val dailyJustWon = current.mode == GameMode.DAILY &&
            !current.dailyCompletedToday &&
            updatedScore >= current.dailyTargetScore

        if (dailyJustWon) {
            soundAndHaptics.onDailyWin()
        } else if (noMovesLeft) {
            soundAndHaptics.onGameOver()
        }

        val burst = if (outcome.clearedCells.isNotEmpty()) {
            ShatterBurst(
                id = ++effectCounter,
                cells = outcome.clearedCells,
                pointsEarned = outcome.pointsEarned,
                combo = outcome.newCombo,
                linesCleared = outcome.linesCleared
            )
        } else {
            current.shatterBurst
        }

        val pulse = PlacementPulse(
            id = ++effectCounter,
            cellIndices = placedIndices,
            pointsEarned = outcome.pointsEarned
        )

        _uiState.value = current.copy(
            board = outcome.updatedBoard,
            tray = nextTray,
            score = updatedScore,
            bestScore = updatedBest,
            combo = outcome.newCombo,
            maxComboInRun = updatedMaxCombo,
            linesClearedInRun = updatedLines,
            dailyCompletedToday = current.dailyCompletedToday || dailyJustWon,
            dailyVictoryJustTriggered = dailyJustWon,
            isGameOver = noMovesLeft,
            newRecordAchieved = current.newRecordAchieved || isNewRecord,
            shatterBurst = burst,
            placementPulse = pulse,
            aiHintState = AiHintUiState.Idle
        )

        viewModelScope.launch {
            if (dailyJustWon) {
                repository.recordGameCompletion(
                    displayName = playerDisplayName,
                    score = updatedScore.toLong(),
                    maxComboInRun = updatedMaxCombo.toLong(),
                    linesClearedInRun = updatedLines.toLong(),
                    isDailyWin = true,
                    dailyDateKey = current.dailyDateKey
                )
            } else if (noMovesLeft) {
                repository.recordGameCompletion(
                    displayName = playerDisplayName,
                    score = updatedScore.toLong(),
                    maxComboInRun = updatedMaxCombo.toLong(),
                    linesClearedInRun = updatedLines.toLong(),
                    isDailyWin = false
                )
            } else if (current.mode == GameMode.CLASSIC) {
                repository.saveClassicSession(
                    board = outcome.updatedBoard,
                    score = updatedScore.toLong(),
                    combo = outcome.newCombo.toLong(),
                    tray = nextTray
                )
            }
        }
        return true
    }

    fun dismissDailyBanner() {
        _uiState.update { it.copy(dailyVictoryJustTriggered = false) }
    }

    fun requestAiHint() {
        val current = _uiState.value
        if (current.isGameOver) return
        if (current.aiHintState is AiHintUiState.Loading) return

        _uiState.update { it.copy(aiHintState = AiHintUiState.Loading) }
        viewModelScope.launch {
            val result = aiRepository.suggestBestMove(
                board = current.board,
                tray = current.tray,
                currentCombo = current.combo
            )
            _uiState.update { state ->
                result.fold(
                    onSuccess = { hint -> state.copy(aiHintState = AiHintUiState.Ready(hint)) },
                    onFailure = { err ->
                        state.copy(
                            aiHintState = AiHintUiState.Error(
                                err.localizedMessage ?: "Unable to analyze board right now."
                            )
                        )
                    }
                )
            }
        }
    }

    fun dismissAiHint() {
        _uiState.update { it.copy(aiHintState = AiHintUiState.Idle) }
    }
}
