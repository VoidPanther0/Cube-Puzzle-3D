package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ObsidianJewelColorScheme = darkColorScheme(
    primary = JewelCyan,
    onPrimary = TextDarkOnAccent,
    primaryContainer = ObsidianCard,
    onPrimaryContainer = TextPrimary,
    secondary = JewelGold,
    onSecondary = TextDarkOnAccent,
    secondaryContainer = ObsidianBoard,
    onSecondaryContainer = JewelGold,
    tertiary = JewelEmerald,
    onTertiary = TextDarkOnAccent,
    background = ObsidianDeep,
    onBackground = TextPrimary,
    surface = ObsidianMid,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder,
    error = JewelCrimson,
    onError = TextPrimary
)

private val CubeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun CubePuzzleTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ObsidianJewelColorScheme,
        typography = Typography,
        shapes = CubeShapes,
        content = content
    )
}
