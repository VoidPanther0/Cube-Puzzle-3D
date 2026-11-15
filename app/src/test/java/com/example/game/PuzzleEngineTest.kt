package com.example.game

import com.example.data.model.JewelColor
import com.example.data.model.PieceCatalog
import com.example.data.model.TrayPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleEngineTest {

    @Test
    fun emptyBoard_has64EmptyCells() {
        val board = PuzzleEngine.emptyBoard()
        assertEquals(64, board.size)
        assertTrue(board.all { it == null })
    }

    @Test
    fun placePiece_rejectsOverlapAndOutOfBounds() {
        val line3H = PieceCatalog.findById("H_3")!!
        val board = PuzzleEngine.emptyBoard().toMutableList()
        board[PuzzleEngine.indexOf(0, 2)] = JewelColor.RUBY

        // Overlaps (0,2)
        assertFalse(PuzzleEngine.canPlace(board, line3H, 0, 0))
        // Out of bounds on right edge (cols 6,7,8)
        assertFalse(PuzzleEngine.canPlace(board, line3H, 0, 6))
        // Valid on row 1
        assertTrue(PuzzleEngine.canPlace(board, line3H, 1, 0))
    }

    @Test
    fun placePiece_clearsFullRowAndAwardsCombo() {
        val board = PuzzleEngine.emptyBoard().toMutableList()
        for (c in 0..4) {
            board[PuzzleEngine.indexOf(3, c)] = JewelColor.SAPPHIRE
        }
        val line3H = PieceCatalog.findById("H_3")!!
        val outcome = PuzzleEngine.placePiece(board, line3H, JewelColor.EMERALD, 3, 5, currentCombo = 1)
        assertNotNull(outcome)
        assertEquals(1, outcome!!.linesCleared)
        assertEquals(2, outcome.newCombo)
        for (c in 0 until BOARD_SIZE) {
            assertNull(outcome.updatedBoard[PuzzleEngine.indexOf(3, c)])
        }
        assertTrue(outcome.pointsEarned > 30)
    }

    @Test
    fun hasAnyValidMove_detectsGameOverWhenBoardFull() {
        val fullBoard = List(BOARD_SIZE * BOARD_SIZE) { JewelColor.AMBER }
        val tray = listOf(
            TrayPiece(0, PieceCatalog.findById("DOT_1")!!, JewelColor.RUBY, false),
            TrayPiece(1, PieceCatalog.findById("H_2")!!, JewelColor.AQUA, false),
            TrayPiece(2, PieceCatalog.findById("SQ_2")!!, JewelColor.EMERALD, false)
        )
        assertFalse(PuzzleEngine.hasAnyValidMove(fullBoard, tray))
    }

    @Test
    fun createDailyChallenge_isDeterministicForSameDate() {
        val seedA = PuzzleEngine.createDailyChallenge("2026-11-15")
        val seedB = PuzzleEngine.createDailyChallenge("2026-11-15")
        assertEquals(seedA.initialBoard, seedB.initialBoard)
        assertEquals(seedA.initialTray.map { it.encode() }, seedB.initialTray.map { it.encode() })
    }
}
