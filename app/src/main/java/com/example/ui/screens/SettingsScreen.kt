package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOut
import com.example.data.model.UserProfile
import com.example.ui.components.AnimatedJewelBackground
import com.example.ui.theme.JewelCrimson
import com.example.ui.theme.JewelCyan
import com.example.ui.theme.JewelEmerald
import com.example.ui.theme.JewelGold
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    profile: UserProfile?,
    displayName: String,
    onUpdateSettings: (soundEnabled: Boolean, hapticsEnabled: Boolean, showLinePreview: Boolean) -> Unit,
    onBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val soundEnabled = profile?.soundEnabled ?: true
    val hapticsEnabled = profile?.hapticsEnabled ?: true
    val showLinePreview = profile?.showLinePreview ?: true

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedJewelBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.90f)),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "GAMEPLAY & FEEDBACK",
                        style = MaterialTheme.typography.labelSmall,
                        color = JewelCyan
                    )

                    SettingToggleRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        tint = JewelCyan,
                        title = "Sound Effects",
                        subtitle = "Synthesized jewel clicks and combo chimes",
                        checked = soundEnabled,
                        testTag = "sound_toggle",
                        onCheckedChange = { newVal ->
                            onUpdateSettings(newVal, hapticsEnabled, showLinePreview)
                        }
                    )

                    SettingToggleRow(
                        icon = Icons.Default.Vibration,
                        tint = JewelGold,
                        title = "Haptic Vibration",
                        subtitle = "Tactile feedback on snap and line clears",
                        checked = hapticsEnabled,
                        testTag = "haptics_toggle",
                        onCheckedChange = { newVal ->
                            onUpdateSettings(soundEnabled, newVal, showLinePreview)
                        }
                    )

                    SettingToggleRow(
                        icon = Icons.Default.AutoAwesome,
                        tint = JewelEmerald,
                        title = "Clear Line Preview",
                        subtitle = "Glow rows & columns poised to clear while dragging",
                        checked = showLinePreview,
                        testTag = "line_preview_toggle",
                        onCheckedChange = { newVal ->
                            onUpdateSettings(soundEnabled, hapticsEnabled, newVal)
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianCard.copy(alpha = 0.90f)),
                border = BorderStroke(1.dp, ObsidianBorder),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "CLOUD ACCOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = JewelGold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Player account",
                            tint = JewelCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = profile?.displayName ?: displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${profile?.gamesPlayed ?: 0L} games played • Cloud synced",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            signOut(
                                credentialManager = credentialManager,
                                scope = scope,
                                onSignOutComplete = onSignedOut
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, JewelCrimson.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = JewelCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SIGN OUT",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = tint
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
