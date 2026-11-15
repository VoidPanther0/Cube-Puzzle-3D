package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameMode {
    CLASSIC,
    DAILY
}

enum class JewelColor(
    val id: Int,
    val label: String,
    val base: Color,
    val lightFacet: Color,
    val darkFacet: Color,
    val glow: Color
) {
    RUBY(
        id = 1,
        label = "Ruby",
        base = Color(0xFFE11D48),
        lightFacet = Color(0xFFFDA4AF),
        darkFacet = Color(0xFF881337),
        glow = Color(0xFFFF4D6D)
    ),
    SAPPHIRE(
        id = 2,
        label = "Sapphire",
        base = Color(0xFF2563EB),
        lightFacet = Color(0xFF93C5FD),
        darkFacet = Color(0xFF1E3A8A),
        glow = Color(0xFF60A5FA)
    ),
    EMERALD(
        id = 3,
        label = "Emerald",
        base = Color(0xFF10B981),
        lightFacet = Color(0xFF6EE7B7),
        darkFacet = Color(0xFF065F46),
        glow = Color(0xFF34D399)
    ),
    AMBER(
        id = 4,
        label = "Amber",
        base = Color(0xFFF59E0B),
        lightFacet = Color(0xFFFDE68A),
        darkFacet = Color(0xFF92400E),
        glow = Color(0xFFFBBF24)
    ),
    AMETHYST(
        id = 5,
        label = "Amethyst",
        base = Color(0xFF9333EA),
        lightFacet = Color(0xFFD8B4FE),
        darkFacet = Color(0xFF581C87),
        glow = Color(0xFFC084FC)
    ),
    AQUA(
        id = 6,
        label = "Aqua",
        base = Color(0xFF06B6D4),
        lightFacet = Color(0xFFA5F3FC),
        darkFacet = Color(0xFF155E75),
        glow = Color(0xFF22D3EE)
    ),
    CORAL(
        id = 7,
        label = "Coral",
        base = Color(0xFFF97316),
        lightFacet = Color(0xFFFDBA74),
        darkFacet = Color(0xFF9A3412),
        glow = Color(0xFFFB923C)
    );

    companion object {
        fun fromId(id: Int): JewelColor? = entries.firstOrNull { it.id == id }
    }
}

data class GridOffset(val row: Int, val col: Int)

data class PieceShape(
    val id: String,
    val name: String,
    val cells: List<GridOffset>
) {
    val rows: Int = (cells.maxOfOrNull { it.row } ?: 0) + 1
    val cols: Int = (cells.maxOfOrNull { it.col } ?: 0) + 1
    val cellCount: Int = cells.size
}

data class TrayPiece(
    val slotIndex: Int,
    val shape: PieceShape,
    val color: JewelColor,
    val isPlaced: Boolean = false
) {
    fun encode(): String = if (isPlaced) "" else "${shape.id}:${color.id}"
}

object PieceCatalog {
    val allShapes: List<PieceShape> = listOf(
        // Single block
        PieceShape("DOT_1", "Single", listOf(GridOffset(0, 0))),
        // Horizontal lines
        PieceShape("H_2", "Line 2H", listOf(GridOffset(0, 0), GridOffset(0, 1))),
        PieceShape("H_3", "Line 3H", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2))),
        PieceShape("H_4", "Line 4H", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(0, 3))),
        PieceShape("H_5", "Line 5H", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(0, 3), GridOffset(0, 4))),
        // Vertical lines
        PieceShape("V_2", "Line 2V", listOf(GridOffset(0, 0), GridOffset(1, 0))),
        PieceShape("V_3", "Line 3V", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(2, 0))),
        PieceShape("V_4", "Line 4V", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(2, 0), GridOffset(3, 0))),
        PieceShape("V_5", "Line 5V", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(2, 0), GridOffset(3, 0), GridOffset(4, 0))),
        // Squares & Rectangles
        PieceShape("SQ_2", "Square 2x2", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(1, 0), GridOffset(1, 1))),
        PieceShape(
            "SQ_3", "Square 3x3",
            (0..2).flatMap { r -> (0..2).map { c -> GridOffset(r, c) } }
        ),
        PieceShape(
            "RECT_2X3", "Block 2x3",
            (0..1).flatMap { r -> (0..2).map { c -> GridOffset(r, c) } }
        ),
        PieceShape(
            "RECT_3X2", "Block 3x2",
            (0..2).flatMap { r -> (0..1).map { c -> GridOffset(r, c) } }
        ),
        // Mini 2x2 L-corners
        PieceShape("ML_1", "Corner TL", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(1, 0))),
        PieceShape("ML_2", "Corner TR", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(1, 1))),
        PieceShape("ML_3", "Corner BL", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(1, 1))),
        PieceShape("ML_4", "Corner BR", listOf(GridOffset(0, 1), GridOffset(1, 0), GridOffset(1, 1))),
        // 3x3 Big L-corners
        PieceShape("BL_1", "Big L 1", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(2, 0), GridOffset(2, 1), GridOffset(2, 2))),
        PieceShape("BL_2", "Big L 2", listOf(GridOffset(0, 2), GridOffset(1, 2), GridOffset(2, 0), GridOffset(2, 1), GridOffset(2, 2))),
        PieceShape("BL_3", "Big L 3", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(1, 0), GridOffset(2, 0))),
        PieceShape("BL_4", "Big L 4", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(1, 2), GridOffset(2, 2))),
        // Classic 4-cell L & J shapes
        PieceShape("L_1", "L Shape", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(2, 0), GridOffset(2, 1))),
        PieceShape("L_2", "L Right", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(1, 0))),
        PieceShape("J_1", "J Shape", listOf(GridOffset(0, 1), GridOffset(1, 1), GridOffset(2, 0), GridOffset(2, 1))),
        PieceShape("J_2", "J Left", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(1, 1), GridOffset(1, 2))),
        // T-shapes
        PieceShape("T_UP", "T Up", listOf(GridOffset(0, 1), GridOffset(1, 0), GridOffset(1, 1), GridOffset(1, 2))),
        PieceShape("T_DOWN", "T Down", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(0, 2), GridOffset(1, 1))),
        PieceShape("T_LEFT", "T Left", listOf(GridOffset(0, 1), GridOffset(1, 0), GridOffset(1, 1), GridOffset(2, 1))),
        PieceShape("T_RIGHT", "T Right", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(1, 1), GridOffset(2, 0))),
        // S & Z shapes
        PieceShape("S_H", "S Horiz", listOf(GridOffset(0, 1), GridOffset(0, 2), GridOffset(1, 0), GridOffset(1, 1))),
        PieceShape("Z_H", "Z Horiz", listOf(GridOffset(0, 0), GridOffset(0, 1), GridOffset(1, 1), GridOffset(1, 2))),
        PieceShape("S_V", "S Vert", listOf(GridOffset(0, 0), GridOffset(1, 0), GridOffset(1, 1), GridOffset(2, 1))),
        PieceShape("Z_V", "Z Vert", listOf(GridOffset(0, 1), GridOffset(1, 0), GridOffset(1, 1), GridOffset(2, 0)))
    )

    private val byId = allShapes.associateBy { it.id }

    fun findById(id: String): PieceShape? = byId[id]

    fun decodeTray(encoded: List<String>): List<TrayPiece>? {
        if (encoded.size != 3) return null
        val decoded = encoded.mapIndexed { index, raw ->
            if (raw.isBlank()) {
                TrayPiece(
                    slotIndex = index,
                    shape = allShapes.first(),
                    color = JewelColor.SAPPHIRE,
                    isPlaced = true
                )
            } else {
                val parts = raw.split(":")
                if (parts.size != 2) return null
                val shape = findById(parts[0]) ?: return null
                val color = JewelColor.fromId(parts[1].toIntOrNull() ?: return null) ?: return null
                TrayPiece(slotIndex = index, shape = shape, color = color, isPlaced = false)
            }
        }
        return if (decoded.all { it.isPlaced }) null else decoded
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val bestScore: Long = 0L,
    val highestCombo: Long = 0L,
    val gamesPlayed: Long = 0L,
    val totalLinesCleared: Long = 0L,
    val dailyStreak: Long = 0L,
    val dailyWins: Long = 0L,
    val lastDailyDate: String = "",
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val showLinePreview: Boolean = true,
    val savedClassicBoard: List<Int> = emptyList(),
    val savedClassicScore: Long = 0L,
    val savedClassicCombo: Long = 0L,
    val savedClassicTray: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val bestScore: Long = 0L,
    val highestCombo: Long = 0L,
    val dailyWins: Long = 0L,
    val isPublic: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
