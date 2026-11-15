package com.example.data.ai

import android.util.Log
import com.example.data.model.JewelColor
import com.example.data.model.TrayPiece
import com.example.game.BOARD_SIZE
import com.example.game.PuzzleEngine
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class StrategyHint(
    val slotIndex: Int,
    val anchorRow: Int,
    val anchorCol: Int,
    val explanation: String
)

class AiStrategyRepository {

    private val hintSchema = Schema.obj(
        properties = mapOf(
            "slotIndex" to Schema.integer(description = "Zero-based tray slot index (0, 1, or 2) of the best piece to place next"),
            "targetRow" to Schema.integer(description = "Zero-based board row (0..7) for the piece's (0,0) top-left anchor"),
            "targetCol" to Schema.integer(description = "Zero-based board column (0..7) for the piece's (0,0) top-left anchor"),
            "strategyTip" to Schema.string(description = "Short 1-2 sentence tactical explanation of why this placement maximizes line clears or board survival")
        )
    )

    private val strategyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-carbon",
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
                responseMimeType = "application/json"
                responseSchema = hintSchema
            }
        )
    }

    suspend fun suggestBestMove(
        board: List<JewelColor?>,
        tray: List<TrayPiece>,
        currentCombo: Int
    ): Result<StrategyHint> {
        val validMoves = buildList {
            for (piece in tray) {
                if (piece.isPlaced) continue
                for (r in 0 until BOARD_SIZE) {
                    for (c in 0 until BOARD_SIZE) {
                        if (PuzzleEngine.canPlace(board, piece.shape, r, c)) {
                            val clear = PuzzleEngine.previewClears(board, piece.shape, r, c)
                            add("slot=${piece.slotIndex} row=$r col=$c clears=${clear.totalLines}")
                        }
                    }
                }
            }
        }

        if (validMoves.isEmpty()) {
            return Result.failure(IllegalStateException("No valid moves remain on the board."))
        }

        val prompt = buildString {
            appendLine("You are a grandmaster puzzle strategist for an 8x8 Block Blast-style jewel puzzle game.")
            appendLine("Current board (# = occupied, . = empty, rows 0..7 top-to-bottom, cols 0..7 left-to-right):")
            appendLine(PuzzleEngine.formatBoardAscii(board))
            appendLine("Active combo streak: $currentCombo")
            appendLine("Available tray pieces:")
            for (piece in tray) {
                if (!piece.isPlaced) {
                    appendLine("- Slot ${piece.slotIndex} (${piece.shape.name}): cells relative to anchor (0,0) = ${PuzzleEngine.formatShapeOffsets(piece.shape.cells)}")
                } else {
                    appendLine("- Slot ${piece.slotIndex}: ALREADY PLACED")
                }
            }
            appendLine("Legal placements (choose ONLY from this exact list so the move is 100% valid):")
            appendLine(validMoves.take(60).joinToString("; "))
            appendLine("Pick the strongest strategic move considering immediate row/column clears, keeping 3x3 space open, and enabling the remaining tray pieces to fit.")
        }

        return try {
            val response = withTimeout(60_000L) {
                strategyModel.generateContent(prompt)
            }
            val rawJson = response.text?.takeIf { it.isNotBlank() }
                ?: return Result.failure(IllegalStateException("AI Coach returned an empty response."))

            val json = JSONObject(rawJson)
            val slotIndex = json.optInt("slotIndex", -1)
            val targetRow = json.optInt("targetRow", -1)
            val targetCol = json.optInt("targetCol", -1)
            val tip = json.optString("strategyTip", "Places cleanly while keeping space open for upcoming pieces.")

            val chosenPiece = tray.firstOrNull { it.slotIndex == slotIndex && !it.isPlaced }
            if (chosenPiece != null && PuzzleEngine.canPlace(board, chosenPiece.shape, targetRow, targetCol)) {
                Result.success(
                    StrategyHint(
                        slotIndex = slotIndex,
                        anchorRow = targetRow,
                        anchorCol = targetCol,
                        explanation = tip
                    )
                )
            } else {
                // If model coordinates were slightly off despite prompt list, report cleanly or pick best legal move
                val fallbackPiece = tray.firstOrNull { !it.isPlaced }
                var bestMove: StrategyHint? = null
                var bestClears = -1
                for (p in tray.filter { !it.isPlaced }) {
                    for (r in 0 until BOARD_SIZE) {
                        for (c in 0 until BOARD_SIZE) {
                            if (PuzzleEngine.canPlace(board, p.shape, r, c)) {
                                val clears = PuzzleEngine.previewClears(board, p.shape, r, c).totalLines
                                if (clears > bestClears) {
                                    bestClears = clears
                                    bestMove = StrategyHint(p.slotIndex, r, c, tip)
                                }
                            }
                        }
                    }
                }
                if (bestMove != null) {
                    Result.success(bestMove)
                } else if (fallbackPiece != null) {
                    Result.failure(IllegalStateException("No valid placement found."))
                } else {
                    Result.failure(IllegalStateException("Tray is empty."))
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI hint timed out", e)
            Result.failure(IllegalStateException("AI Strategy Coach timed out. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Strategy Coach failed", e)
            Result.failure(e)
        }
    }

    private companion object {
        const val TAG = "AiStrategyRepo"
    }
}
