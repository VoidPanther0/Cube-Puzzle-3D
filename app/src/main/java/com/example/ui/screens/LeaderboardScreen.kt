package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.LeaderboardEntry
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedJewelBackground
import com.example.ui.theme.JewelCyan
import com.example.ui.theme.JewelEmerald
import com.example.ui.theme.JewelGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.UiState

@Composable
fun LeaderboardScreen(
    currentUserId: String,
    profile: UserProfile?,
    leaderboardState: UiState<List<LeaderboardEntry>>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedJewelBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 600.dp)
                .align(Alignment.Center)
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("leaderboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "GLOBAL LEADERBOARD",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Personal Stats Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, JewelCyan.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CareerStat("BEST", "${profile?.bestScore ?: 0L}", JewelGold)
                    CareerStat("COMBO", "x${profile?.highestCombo ?: 0L}", JewelCyan)
                    CareerStat("LINES", "${profile?.totalLinesCleared ?: 0L}", JewelEmerald)
                    CareerStat("DAILY", "${profile?.dailyWins ?: 0L}", Color(0xFFC084FC))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (leaderboardState) {
                UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = JewelCyan)
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = leaderboardState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is UiState.Success -> {
                    val entries = leaderboardState.data
                    if (entries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = JewelGold.copy(alpha = 0.7f),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Complete a game to claim the #1 rank!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(entries, key = { _, item -> item.userId }) { index, entry ->
                                val isCurrentUser = entry.userId == currentUserId
                                val rankColor = when (index) {
                                    0 -> JewelGold
                                    1 -> Color(0xFFE2E8F0)
                                    2 -> Color(0xFFFB923C)
                                    else -> TextSecondary
                                }
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrentUser) {
                                            JewelCyan.copy(alpha = 0.14f)
                                        } else {
                                            ObsidianCard.copy(alpha = 0.85f)
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isCurrentUser) JewelCyan else ObsidianBorder
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = rankColor.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "#${index + 1}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = rankColor
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isCurrentUser) "${entry.displayName} (You)" else entry.displayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = "Max Combo x${entry.highestCombo} • ${entry.dailyWins} Daily Wins",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextSecondary
                                            )
                                        }
                                        Text(
                                            text = "${entry.bestScore}",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = if (index == 0) JewelGold else JewelCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = color
        )
    }
}
