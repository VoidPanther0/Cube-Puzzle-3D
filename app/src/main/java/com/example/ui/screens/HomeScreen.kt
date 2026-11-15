package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.JewelColor
import com.example.data.model.PieceCatalog
import com.example.data.model.UserProfile
import com.example.game.PuzzleEngine
import com.example.ui.components.AnimatedJewelBackground
import com.example.ui.components.drawJewelCube
import com.example.ui.theme.JewelCyan
import com.example.ui.theme.JewelEmerald
import com.example.ui.theme.JewelGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextDarkOnAccent
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    profile: UserProfile?,
    displayName: String,
    onPlayClassic: () -> Unit,
    onPlayDailyChallenge: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuitApp: () -> Unit
) {
    val todayKey = PuzzleEngine.todayDateKey()
    val dailyCompleted = profile?.lastDailyDate == todayKey
    val hasResumableClassic = profile != null &&
        profile.savedClassicBoard.size == 64 &&
        PieceCatalog.decodeTray(profile.savedClassicTray) != null

    val pulseTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoFloat by pulseTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_float"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedJewelBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top player badge
            Surface(
                color = ObsidianCard.copy(alpha = 0.85f),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WELCOME BACK",
                            style = MaterialTheme.typography.labelSmall,
                            color = JewelCyan
                        )
                        Text(
                            text = profile?.displayName ?: displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadge(
                            icon = Icons.Default.EmojiEvents,
                            tint = JewelGold,
                            value = (profile?.bestScore ?: 0L).toString()
                        )
                        StatBadge(
                            icon = Icons.Default.LocalFireDepartment,
                            tint = Color(0xFFFB923C),
                            value = "${profile?.dailyStreak ?: 0L}d"
                        )
                    }
                }
            }

            // Center 3D Game Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(110.dp)
                        .padding(top = (4f + logoFloat).dp)
                ) {
                    val unit = size.minDimension * 0.28f
                    val gap = size.minDimension * 0.04f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    // L-cluster + top jewel crown
                    drawJewelCube(Offset(cx - unit - gap / 2f, cy - unit - gap / 2f), unit, JewelColor.RUBY)
                    drawJewelCube(Offset(cx - unit - gap / 2f, cy + gap / 2f), unit, JewelColor.AMETHYST)
                    drawJewelCube(Offset(cx + gap / 2f, cy + gap / 2f), unit, JewelColor.AQUA)
                    drawJewelCube(Offset(cx + unit + gap * 1.5f, cy + gap / 2f), unit, JewelColor.AMBER)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "CUBE PUZZLE 3D",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ALIGN • SHATTER • CONQUER",
                    style = MaterialTheme.typography.labelSmall,
                    color = JewelGold
                )
            }

            // Action stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayClassic,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JewelCyan,
                        contentColor = TextDarkOnAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasResumableClassic) "PLAY (RESUME)" else "PLAY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Daily Puzzle Challenge card button
                Card(
                    onClick = onPlayDailyChallenge,
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.92f)),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (dailyCompleted) JewelEmerald.copy(alpha = 0.7f) else JewelGold.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_challenge_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (dailyCompleted) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                                contentDescription = "Daily Challenge",
                                tint = if (dailyCompleted) JewelEmerald else JewelGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "DAILY CHALLENGE",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (dailyCompleted) "Completed today ($todayKey)" else "Target 1,500 pts • $todayKey",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        Surface(
                            color = (if (dailyCompleted) JewelEmerald else JewelGold).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (dailyCompleted) "DONE" else "PLAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (dailyCompleted) JewelEmerald else JewelGold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenLeaderboard,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ObsidianCard.copy(alpha = 0.78f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("leaderboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = JewelGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RANKS",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ObsidianBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ObsidianCard.copy(alpha = 0.78f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = JewelCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SETTINGS",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                OutlinedButton(
                    onClick = onQuitApp,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFFB7185).copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = ObsidianCard.copy(alpha = 0.55f),
                        contentColor = Color(0xFFFB7185)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("quit_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Quit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "QUIT",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
