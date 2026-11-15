package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.GameMode
import com.example.data.model.JewelColor
import com.example.data.model.TrayPiece
import com.example.game.BOARD_SIZE
import com.example.game.ClearPreview
import com.example.game.PuzzleEngine
import com.example.ui.components.AnimatedJewelBackground
import com.example.ui.components.drawJewelCube
import com.example.ui.theme.JewelAmethyst
import com.example.ui.theme.JewelCrimson
import com.example.ui.theme.JewelCyan
import com.example.ui.theme.JewelEmerald
import com.example.ui.theme.JewelGold
import com.example.ui.theme.ObsidianBoard
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCellEmpty
import com.example.ui.theme.TextDarkOnAccent
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiHintUiState
import com.example.ui.viewmodel.GameUiState
import kotlin.math.roundToInt

private data class HoverPreview(
    val anchorRow: Int,
    val anchorCol: Int,
    val isValid: Boolean,
    val clearPreview: ClearPreview
)

@Composable
fun GameScreen(
    uiState: GameUiState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onPiecePickup: () -> Unit,
    onAttemptPlace: (slotIndex: Int, anchorRow: Int, anchorCol: Int) -> Boolean,
    onRequestAiHint: () -> Unit,
    onDismissAiHint: () -> Unit,
    onDismissDailyBanner: () -> Unit
) {
    BackHandler { onBack() }

    var boardBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val slotBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }

    var draggingSlot by remember { mutableStateOf<Int?>(null) }
    var dragPointerRoot by remember { mutableStateOf(Offset.Zero) }

    // Placement scale bounce animation
    val placementAnim = remember { Animatable(1f) }
    LaunchedEffect(uiState.placementPulse?.id) {
        if (uiState.placementPulse != null) {
            placementAnim.snapTo(0.68f)
            placementAnim.animateTo(1f, tween(durationMillis = 190, easing = FastOutSlowInEasing))
        }
    }

    // Line-clear shatter burst animation
    val shatterAnim = remember { Animatable(1f) }
    LaunchedEffect(uiState.shatterBurst?.id) {
        if (uiState.shatterBurst != null) {
            shatterAnim.snapTo(0f)
            shatterAnim.animateTo(1f, tween(durationMillis = 440, easing = FastOutSlowInEasing))
        }
    }

    // Invalid placement subtle board shake
    val shakeAnim = remember { Animatable(0f) }
    LaunchedEffect(uiState.invalidShakeNonce) {
        if (uiState.invalidShakeNonce > 0L) {
            shakeAnim.snapTo(1f)
            shakeAnim.animateTo(0f, tween(durationMillis = 220))
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "hint_pulse")
    val hintPulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hint_alpha"
    )

    val boardPadFraction = 0.025f
    val boardInnerSize = boardBoundsInRoot.width * (1f - boardPadFraction * 2f)
    val cellPitch = if (boardInnerSize > 0f) boardInnerSize / BOARD_SIZE else 0f
    val cellGap = cellPitch * 0.10f
    val cubeSize = (cellPitch - cellGap).coerceAtLeast(1f)
    val boardInnerTopLeft = Offset(
        x = boardBoundsInRoot.left + boardBoundsInRoot.width * boardPadFraction,
        y = boardBoundsInRoot.top + boardBoundsInRoot.height * boardPadFraction
    )

    val activeDragPiece: TrayPiece? = draggingSlot?.let { slot ->
        uiState.tray.firstOrNull { it.slotIndex == slot && !it.isPlaced }
    }

    val liftedPieceTopLeft: Offset? = if (activeDragPiece != null && cellPitch > 0f) {
        val liftedCenterY = dragPointerRoot.y - cellPitch * 2.35f
        val pieceW = activeDragPiece.shape.cols * cellPitch
        val pieceH = activeDragPiece.shape.rows * cellPitch
        Offset(
            x = dragPointerRoot.x - pieceW / 2f,
            y = liftedCenterY - pieceH / 2f
        )
    } else {
        null
    }

    val hoverPreview: HoverPreview? = if (activeDragPiece != null && liftedPieceTopLeft != null && cellPitch > 0f) {
        val relX = liftedPieceTopLeft.x - boardInnerTopLeft.x
        val relY = liftedPieceTopLeft.y - boardInnerTopLeft.y
        val col = (relX / cellPitch).roundToInt()
        val row = (relY / cellPitch).roundToInt()
        val withinHoverBand =
            col >= -1 && col <= BOARD_SIZE &&
                row >= -1 && row <= BOARD_SIZE &&
                liftedPieceTopLeft.y < boardBoundsInRoot.bottom + cellPitch * 0.7f
        if (withinHoverBand) {
            val valid = PuzzleEngine.canPlace(uiState.board, activeDragPiece.shape, row, col)
            val clears = if (valid && uiState.showLinePreview) {
                PuzzleEngine.previewClears(uiState.board, activeDragPiece.shape, row, col)
            } else {
                ClearPreview()
            }
            HoverPreview(row, col, valid, clears)
        } else {
            null
        }
    } else {
        null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedJewelBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column {
                        Text(
                            text = if (uiState.mode == GameMode.DAILY) "DAILY CHALLENGE" else "CLASSIC ENDLESS",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (uiState.mode == GameMode.DAILY) JewelGold else JewelCyan,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.mode == GameMode.DAILY) {
                            Text(
                                text = uiState.dailyDateKey,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = onRequestAiHint,
                        enabled = uiState.aiHintState !is AiHintUiState.Loading && !uiState.isGameOver,
                        color = JewelAmethyst.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, JewelAmethyst.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("ai_hint_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            if (uiState.aiHintState is AiHintUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = JewelAmethyst,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Strategy Hint",
                                    tint = JewelAmethyst,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.aiHintState is AiHintUiState.Loading) "THINKING" else "AI HINT",
                                style = MaterialTheme.typography.labelSmall,
                                color = JewelAmethyst
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier.testTag("restart_game_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart Game",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Score & Best Score HUD Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.90f)),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "SCORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = uiState.score.toString(),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("score_value")
                            )
                        }

                        if (uiState.combo >= 2) {
                            Surface(
                                color = JewelGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, JewelGold),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "COMBO x${uiState.combo}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = JewelGold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Best Score",
                                    tint = JewelGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = uiState.bestScore.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = JewelGold
                            )
                        }
                    }

                    if (uiState.mode == GameMode.DAILY) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val progress = (uiState.score.toFloat() / uiState.dailyTargetScore.toFloat()).coerceIn(0f, 1f)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (uiState.dailyCompletedToday) "Daily Goal Reached!" else "Daily Goal: ${uiState.score} / ${uiState.dailyTargetScore}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.dailyCompletedToday) JewelEmerald else JewelGold
                            )
                            Text(
                                text = "${(progress * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            color = if (uiState.dailyCompletedToday) JewelEmerald else JewelGold,
                            trackColor = ObsidianCellEmpty,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }

            // Optional AI Strategy Hint Banner
            AnimatedVisibility(
                visible = uiState.aiHintState is AiHintUiState.Ready || uiState.aiHintState is AiHintUiState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val hintReady = uiState.aiHintState as? AiHintUiState.Ready
                val hintError = uiState.aiHintState as? AiHintUiState.Error
                Surface(
                    color = if (hintReady != null) JewelAmethyst.copy(alpha = 0.16f) else JewelCrimson.copy(alpha = 0.16f),
                    border = BorderStroke(
                        1.dp,
                        if (hintReady != null) JewelAmethyst else JewelCrimson
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (hintReady != null) JewelAmethyst else JewelCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = hintReady?.hint?.explanation ?: hintError?.message.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onDismissAiHint,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss hint",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 8x8 Obsidian Jewel Board
            val shakeOffsetPx = kotlin.math.sin(shakeAnim.value * Math.PI * 4).toFloat() * 14f * shakeAnim.value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .offset { IntOffset(shakeOffsetPx.roundToInt(), 0) }
                    .onGloballyPositioned { coords ->
                        boardBoundsInRoot = coords.boundsInRoot()
                    }
                    .testTag("puzzle_board")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Outer board frame
                    drawRoundRect(
                        color = ObsidianBoard,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                    )
                    drawRoundRect(
                        color = ObsidianBorder,
                        topLeft = Offset.Zero,
                        size = size,
                        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val pad = size.width * boardPadFraction
                    val inner = size.width - pad * 2f
                    val pitch = inner / BOARD_SIZE
                    val gap = pitch * 0.10f
                    val cellSz = pitch - gap
                    val offsetHalfGap = gap / 2f

                    val clearRows = hoverPreview?.clearPreview?.fullRows.orEmpty()
                    val clearCols = hoverPreview?.clearPreview?.fullCols.orEmpty()
                    val pulseIndices = uiState.placementPulse?.cellIndices.orEmpty()
                    val pulseScale = placementAnim.value

                    // 1. Empty recessed cells + placed jewel blocks
                    for (r in 0 until BOARD_SIZE) {
                        for (c in 0 until BOARD_SIZE) {
                            val cellTopLeft = Offset(
                                x = pad + c * pitch + offsetHalfGap,
                                y = pad + r * pitch + offsetHalfGap
                            )
                            val isHighlightLine = (r in clearRows || c in clearCols)
                            drawRoundRect(
                                color = if (isHighlightLine) {
                                    JewelCyan.copy(alpha = 0.22f)
                                } else {
                                    ObsidianCellEmpty
                                },
                                topLeft = cellTopLeft,
                                size = Size(cellSz, cellSz),
                                cornerRadius = CornerRadius(cellSz * 0.18f, cellSz * 0.18f)
                            )

                            val idx = PuzzleEngine.indexOf(r, c)
                            val cellColor = uiState.board[idx]
                            if (cellColor != null) {
                                val scale = if (idx in pulseIndices) pulseScale else 1f
                                val scaledSz = cellSz * scale
                                val inset = (cellSz - scaledSz) / 2f
                                drawJewelCube(
                                    topLeft = Offset(cellTopLeft.x + inset, cellTopLeft.y + inset),
                                    cubeSize = scaledSz,
                                    jewelColor = cellColor,
                                    isHighlightedClear = isHighlightLine
                                )
                            }
                        }
                    }

                    // 2. Dragging ghost placement preview
                    if (activeDragPiece != null && hoverPreview != null && hoverPreview.isValid) {
                        for (offset in activeDragPiece.shape.cells) {
                            val r = hoverPreview.anchorRow + offset.row
                            val c = hoverPreview.anchorCol + offset.col
                            val topLeft = Offset(
                                x = pad + c * pitch + offsetHalfGap,
                                y = pad + r * pitch + offsetHalfGap
                            )
                            drawJewelCube(
                                topLeft = topLeft,
                                cubeSize = cellSz,
                                jewelColor = activeDragPiece.color,
                                isGhost = true,
                                isHighlightedClear = (r in clearRows || c in clearCols)
                            )
                        }
                    }

                    // 3. AI Hint pulsing ghost preview
                    val readyHint = (uiState.aiHintState as? AiHintUiState.Ready)?.hint
                    if (readyHint != null && draggingSlot == null) {
                        val hintedPiece = uiState.tray.firstOrNull {
                            it.slotIndex == readyHint.slotIndex && !it.isPlaced
                        }
                        if (hintedPiece != null) {
                            for (offset in hintedPiece.shape.cells) {
                                val r = readyHint.anchorRow + offset.row
                                val c = readyHint.anchorCol + offset.col
                                if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                                    val topLeft = Offset(
                                        x = pad + c * pitch + offsetHalfGap,
                                        y = pad + r * pitch + offsetHalfGap
                                    )
                                    drawJewelCube(
                                        topLeft = topLeft,
                                        cubeSize = cellSz,
                                        jewelColor = hintedPiece.color,
                                        alpha = hintPulseAlpha,
                                        isGhost = true
                                    )
                                }
                            }
                        }
                    }

                    // 4. Shatter particle burst on cleared lines
                    val burst = uiState.shatterBurst
                    val t = shatterAnim.value
                    if (burst != null && t < 1f) {
                        val fade = 1f - t
                        for (cleared in burst.cells) {
                            val cx = pad + cleared.col * pitch + pitch / 2f
                            val cy = pad + cleared.row * pitch + pitch / 2f
                            val spread = cellSz * 1.15f * t
                            val shardSz = cellSz * 0.28f * (1f - t * 0.6f)
                            val dirs = listOf(
                                Offset(-1f, -1f),
                                Offset(1f, -1f),
                                Offset(-1f, 1f),
                                Offset(1f, 1f)
                            )
                            for (d in dirs) {
                                drawRoundRect(
                                    color = cleared.color.glow.copy(alpha = 0.85f * fade),
                                    topLeft = Offset(cx + d.x * spread - shardSz / 2f, cy + d.y * spread - shardSz / 2f),
                                    size = Size(shardSz, shardSz),
                                    cornerRadius = CornerRadius(shardSz * 0.3f, shardSz * 0.3f)
                                )
                            }
                        }
                    }
                }

                // Floating score popup when lines clear
                val burst = uiState.shatterBurst
                if (burst != null && shatterAnim.value < 0.98f) {
                    val riseDp = (-28f * shatterAnim.value).dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = riseDp)
                    ) {
                        Surface(
                            color = ObsidianCard.copy(alpha = (1f - shatterAnim.value) * 0.95f),
                            border = BorderStroke(1.dp, JewelGold.copy(alpha = 1f - shatterAnim.value)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (burst.combo >= 2) "+${burst.pointsEarned}  COMBO x${burst.combo}!" else "+${burst.pointsEarned}",
                                style = MaterialTheme.typography.titleLarge,
                                color = JewelGold.copy(alpha = 1f - shatterAnim.value),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 3-Piece Tray
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hintedSlot = (uiState.aiHintState as? AiHintUiState.Ready)?.hint?.slotIndex
                uiState.tray.forEach { piece ->
                    val isBeingDragged = draggingSlot == piece.slotIndex
                    val isHinted = hintedSlot == piece.slotIndex && !piece.isPlaced
                    Surface(
                        color = ObsidianCard.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = if (isHinted) 2.dp else 1.dp,
                            color = if (isHinted) JewelAmethyst else ObsidianBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                slotBoundsInRoot[piece.slotIndex] = coords.boundsInRoot()
                            }
                            .pointerInput(piece.slotIndex, piece.shape.id, piece.isPlaced, uiState.isGameOver) {
                                if (piece.isPlaced || uiState.isGameOver) return@pointerInput
                                detectDragGestures(
                                    onDragStart = { localOffset ->
                                        val slotRect = slotBoundsInRoot[piece.slotIndex] ?: Rect.Zero
                                        draggingSlot = piece.slotIndex
                                        dragPointerRoot = Offset(
                                            x = slotRect.left + localOffset.x,
                                            y = slotRect.top + localOffset.y
                                        )
                                        onPiecePickup()
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val slotRect = slotBoundsInRoot[piece.slotIndex] ?: Rect.Zero
                                        dragPointerRoot = Offset(
                                            x = slotRect.left + change.position.x,
                                            y = slotRect.top + change.position.y
                                        )
                                    },
                                    onDragEnd = {
                                        val slot = draggingSlot
                                        val preview = hoverPreview
                                        if (slot != null && preview != null) {
                                            onAttemptPlace(slot, preview.anchorRow, preview.anchorCol)
                                        }
                                        draggingSlot = null
                                    },
                                    onDragCancel = {
                                        draggingSlot = null
                                    }
                                )
                            }
                            .testTag("tray_slot_${piece.slotIndex}")
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!piece.isPlaced && !isBeingDragged) {
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                ) {
                                    val maxDim = 5f
                                    val cellPitchTray = (size.minDimension / maxDim)
                                    val gapTray = cellPitchTray * 0.12f
                                    val cellSzTray = cellPitchTray - gapTray
                                    val totalW = piece.shape.cols * cellPitchTray - gapTray
                                    val totalH = piece.shape.rows * cellPitchTray - gapTray
                                    val originX = (size.width - totalW) / 2f
                                    val originY = (size.height - totalH) / 2f

                                    for (cell in piece.shape.cells) {
                                        drawJewelCube(
                                            topLeft = Offset(
                                                x = originX + cell.col * cellPitchTray,
                                                y = originY + cell.row * cellPitchTray
                                            ),
                                            cubeSize = cellSzTray,
                                            jewelColor = piece.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating dragged piece overlay (full board-scale, lifted above finger)
        if (activeDragPiece != null && liftedPieceTopLeft != null && cellPitch > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (offset in activeDragPiece.shape.cells) {
                    val x = liftedPieceTopLeft.x + offset.col * cellPitch + cellGap / 2f
                    val y = liftedPieceTopLeft.y + offset.row * cellPitch + cellGap / 2f
                    drawJewelCube(
                        topLeft = Offset(x, y),
                        cubeSize = cubeSize,
                        jewelColor = activeDragPiece.color,
                        alpha = 0.96f
                    )
                }
            }
        }

        // Daily Challenge Victory Celebration Modal
        if (uiState.dailyVictoryJustTriggered && !uiState.isGameOver) {
            ModalOverlay {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.5.dp, JewelEmerald),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 380.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Daily Victory",
                            tint = JewelGold,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DAILY CHALLENGE CLEARED!",
                            style = MaterialTheme.typography.titleLarge,
                            color = JewelEmerald,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You reached ${uiState.score} points! Your daily streak has been synced to the cloud.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onDismissDailyBanner,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JewelEmerald,
                                contentColor = TextDarkOnAccent
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("KEEP PLAYING", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Game Over Modal
        if (uiState.isGameOver) {
            ModalOverlay {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard),
                    border = BorderStroke(1.5.dp, if (uiState.newRecordAchieved) JewelGold else ObsidianBorder),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 390.dp)
                        .testTag("game_over_dialog")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (uiState.newRecordAchieved) "NEW HIGH SCORE!" else "NO MORE MOVES",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (uiState.newRecordAchieved) JewelGold else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "${uiState.score}",
                            style = MaterialTheme.typography.displayLarge,
                            color = JewelCyan
                        )
                        Text(
                            text = "Best: ${uiState.bestScore}  •  Lines: ${uiState.linesClearedInRun}  •  Max Combo: x${uiState.maxComboInRun}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = onRestart,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = JewelCyan,
                                contentColor = TextDarkOnAccent
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("play_again_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PLAY AGAIN", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onBack,
                            border = BorderStroke(1.dp, ObsidianBorder),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("game_over_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("HOME", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModalOverlay(content: @Composable () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
