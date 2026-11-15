package com.example.game

import com.example.data.model.GridOffset
import com.example.data.model.JewelColor
import com.example.data.model.PieceCatalog
import com.example.data.model.PieceShape
import com.example.data.model.TrayPiece
import java.time.LocalDate
import kotlin.random.Random

const val BOARD_SIZE = 8

data class ClearPreview(
    val fullRows: Set<Int> = emptySet(),
    val fullCols: Set<Int> = emptySet()
) {
    val totalLines: Int get() = fullRows.size + fullCols.size
}

data class ClearedCell(
    val row: Int,
    val col: Int,
    val color: JewelColor
)

data class PlacementOutcome(
    val updatedBoard: List<JewelColor?>,
    val clearedRows: Set<Int>,
    val clearedCols: Set<Int>,
    val clearedCells: List<ClearedCell>,
    val pointsEarned: Int,
    val newCombo: Int,
    val linesCleared: Int
)

data class DailyChallengeSeed(
    val dateKey: String,
    val initialBoard: List<JewelColor?>,
    val targetScore: Int,
    val initialTray: List<TrayPiece>,
    val seedValue: Long
)

object PuzzleEngine {

    fun emptyBoard(): List<JewelColor?> = List(BOARD_SIZE * BOARD_SIZE) { null }

    fun indexOf(row: Int, col: Int): Int = row * BOARD_SIZE + col

    fun canPlace(
        board: List<JewelColor?>,
        shape: PieceShape,
        anchorRow: Int,
        anchorCol: Int
    ): Boolean {
        if (board.size != BOARD_SIZE * BOARD_SIZE) return false
        for (offset in shape.cells) {
            val r = anchorRow + offset.row
            val c = anchorCol + offset.col
            if (r !in 0 until BOARD_SIZE || c !in 0 until BOARD_SIZE) return false
            if (board[indexOf(r, c)] != null) return false
        }
        return true
    }

    fun previewClears(
        board: List<JewelColor?>,
        shape: PieceShape,
        anchorRow: Int,
        anchorCol: Int
    ): ClearPreview {
        if (!canPlace(board, shape, anchorRow, anchorCol)) return ClearPreview()
        val projected = board.toMutableList()
        for (offset in shape.cells) {
            projected[indexOf(anchorRow + offset.row, anchorCol + offset.col)] = JewelColor.SAPPHIRE
        }
        val fullRows = (0 until BOARD_SIZE).filter { r ->
            (0 until BOARD_SIZE).all { c -> projected[indexOf(r, c)] != null }
        }.toSet()
        val fullCols = (0 until BOARD_SIZE).filter { c ->
            (0 until BOARD_SIZE).all { r -> projected[indexOf(r, c)] != null }
        }.toSet()
        return ClearPreview(fullRows = fullRows, fullCols = fullCols)
    }

    fun placePiece(
        board: List<JewelColor?>,
        shape: PieceShape,
        color: JewelColor,
        anchorRow: Int,
        anchorCol: Int,
        currentCombo: Int
    ): PlacementOutcome? {
        if (!canPlace(board, shape, anchorRow, anchorCol)) return null

        val working = board.toMutableList()
        for (offset in shape.cells) {
            working[indexOf(anchorRow + offset.row, anchorCol + offset.col)] = color
        }

        val fullRows = (0 until BOARD_SIZE).filter { r ->
            (0 until BOARD_SIZE).all { c -> working[indexOf(r, c)] != null }
        }.toSet()
        val fullCols = (0 until BOARD_SIZE).filter { c ->
            (0 until BOARD_SIZE).all { r -> working[indexOf(r, c)] != null }
        }.toSet()

        val linesCleared = fullRows.size + fullCols.size
        val clearedCells = mutableListOf<ClearedCell>()

        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (r in fullRows || c in fullCols) {
                    val idx = indexOf(r, c)
                    val cellColor = working[idx]
                    if (cellColor != null) {
                        clearedCells.add(ClearedCell(r, c, cellColor))
                    }
                    working[idx] = null
                }
            }
        }

        val newCombo = if (linesCleared > 0) currentCombo + 1 else 0
        val cellPoints = shape.cellCount * 10
        val lineBonus = when {
            linesCleared == 0 -> 0
            linesCleared == 1 -> 100
            else -> linesCleared * 120 + (linesCleared - 1) * 80
        }
        val comboBonus = if (linesCleared > 0 && newCombo > 1) {
            (newCombo - 1) * 50 * linesCleared
        } else {
            0
        }

        return PlacementOutcome(
            updatedBoard = working.toList(),
            clearedRows = fullRows,
            clearedCols = fullCols,
            clearedCells = clearedCells,
            pointsEarned = cellPoints + lineBonus + comboBonus,
            newCombo = newCombo,
            linesCleared = linesCleared
        )
    }

    fun hasAnyValidMove(board: List<JewelColor?>, tray: List<TrayPiece>): Boolean {
        val unplaced = tray.filter { !it.isPlaced }
        if (unplaced.isEmpty()) return true
        for (piece in unplaced) {
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (canPlace(board, piece.shape, r, c)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun fitsAnywhere(board: List<JewelColor?>, shape: PieceShape): Boolean {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                if (canPlace(board, shape, r, c)) return true
            }
        }
        return false
    }

    fun generateTray(
        random: Random = Random.Default,
        board: List<JewelColor?>? = null
    ): List<TrayPiece> {
        val shapes = PieceCatalog.allShapes
        val colors = JewelColor.entries
        val picks = MutableList(3) { slot ->
            TrayPiece(
                slotIndex = slot,
                shape = shapes[random.nextInt(shapes.size)],
                color = colors[random.nextInt(colors.size)],
                isPlaced = false
            )
        }
        // Keep Classic gameplay fair by ensuring at least one piece can fit if any small shape fits
        if (board != null && !hasAnyValidMove(board, picks)) {
            val fittingShapes = shapes.filter { fitsAnywhere(board, it) }
            if (fittingShapes.isNotEmpty()) {
                val chosen = fittingShapes[random.nextInt(fittingShapes.size)]
                picks[0] = picks[0].copy(shape = chosen)
            }
        }
        return picks
    }

    fun todayDateKey(): String = LocalDate.now().toString()

    fun createDailyChallenge(dateKey: String = todayDateKey()): DailyChallengeSeed {
        val seed = dateKey.hashCode().toLong() * 31337L + 7919L
        val rng = Random(seed)
        val board = MutableList<JewelColor?>(BOARD_SIZE * BOARD_SIZE) { null }
        val colors = JewelColor.entries

        // Build an artistic symmetrical/tactical starting crystal pattern (14-18 pre-placed jewels)
        val prefillCount = 14 + rng.nextInt(5)
        var placed = 0
        var attempts = 0
        while (placed < prefillCount && attempts < 200) {
            attempts++
            val r = rng.nextInt(BOARD_SIZE)
            val c = rng.nextInt(BOARD_SIZE)
            val idx = indexOf(r, c)
            if (board[idx] == null) {
                val color = colors[rng.nextInt(colors.size)]
                board[idx] = color
                placed++
                // Avoid accidentally completing a full row/col at start
                val rowFull = (0 until BOARD_SIZE).all { col -> board[indexOf(r, col)] != null }
                val colFull = (0 until BOARD_SIZE).all { row -> board[indexOf(row, c)] != null }
                if (rowFull || colFull) {
                    board[idx] = null
                    placed--
                }
            }
        }

        val initialTray = generateTray(rng, board)
        return DailyChallengeSeed(
            dateKey = dateKey,
            initialBoard = board.toList(),
            targetScore = 1500,
            initialTray = initialTray,
            seedValue = seed
        )
    }

    fun boardToInts(board: List<JewelColor?>): List<Int> = board.map { it?.id ?: 0 }

    fun intsToBoard(ints: List<Int>): List<JewelColor?> {
        if (ints.size != BOARD_SIZE * BOARD_SIZE) return emptyBoard()
        return ints.map { id -> if (id == 0) null else JewelColor.fromId(id) }
    }

    fun formatBoardAscii(board: List<JewelColor?>): String = buildString {
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                append(if (board[indexOf(r, c)] != null) '#' else '.')
            }
            if (r < BOARD_SIZE - 1) append('\n')
        }
    }

    fun formatShapeOffsets(offsets: List<GridOffset>): String =
        offsets.joinToString(",") { "(${it.row},${it.col})" }
}
