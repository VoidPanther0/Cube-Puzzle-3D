package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.data.model.JewelColor
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianMid
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Draws a faceted, glossy 3D jewel cube inside [topLeft]..[topLeft + size].
 */
fun DrawScope.drawJewelCube(
    topLeft: Offset,
    cubeSize: Float,
    jewelColor: JewelColor,
    alpha: Float = 1f,
    isGhost: Boolean = false,
    isHighlightedClear: Boolean = false
) {
    if (cubeSize <= 1f) return
    val corner = cubeSize * 0.18f
    val bevel = cubeSize * 0.17f

    if (isHighlightedClear) {
        drawRoundRect(
            color = jewelColor.glow.copy(alpha = 0.45f * alpha),
            topLeft = Offset(topLeft.x - cubeSize * 0.08f, topLeft.y - cubeSize * 0.08f),
            size = Size(cubeSize * 1.16f, cubeSize * 1.16f),
            cornerRadius = CornerRadius(corner * 1.3f, corner * 1.3f)
        )
    }

    if (isGhost) {
        drawRoundRect(
            color = jewelColor.base.copy(alpha = 0.34f * alpha),
            topLeft = topLeft,
            size = Size(cubeSize, cubeSize),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = jewelColor.lightFacet.copy(alpha = 0.72f * alpha),
            topLeft = topLeft,
            size = Size(cubeSize, cubeSize),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (cubeSize * 0.07f).coerceAtLeast(1.5f))
        )
        return
    }

    // Drop shadow for 3D depth
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.38f * alpha),
        topLeft = Offset(topLeft.x + cubeSize * 0.04f, topLeft.y + cubeSize * 0.08f),
        size = Size(cubeSize, cubeSize),
        cornerRadius = CornerRadius(corner, corner)
    )

    // Base rich gradient body
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                jewelColor.lightFacet.copy(alpha = 0.95f * alpha),
                jewelColor.base.copy(alpha = alpha),
                jewelColor.darkFacet.copy(alpha = alpha)
            ),
            start = topLeft,
            end = Offset(topLeft.x + cubeSize, topLeft.y + cubeSize)
        ),
        topLeft = topLeft,
        size = Size(cubeSize, cubeSize),
        cornerRadius = CornerRadius(corner, corner)
    )

    val x0 = topLeft.x
    val y0 = topLeft.y
    val x1 = x0 + cubeSize
    val y1 = y0 + cubeSize
    val ix0 = x0 + bevel
    val iy0 = y0 + bevel
    val ix1 = x1 - bevel
    val iy1 = y1 - bevel

    // Top bright crystal facet
    val topFacet = Path().apply {
        moveTo(x0 + corner * 0.5f, y0 + corner * 0.35f)
        lineTo(x1 - corner * 0.5f, y0 + corner * 0.35f)
        lineTo(ix1, iy0)
        lineTo(ix0, iy0)
        close()
    }
    drawPath(
        path = topFacet,
        color = Color.White.copy(alpha = 0.40f * alpha)
    )

    // Left sunlit facet
    val leftFacet = Path().apply {
        moveTo(x0 + corner * 0.35f, y0 + corner * 0.5f)
        lineTo(ix0, iy0)
        lineTo(ix0, iy1)
        lineTo(x0 + corner * 0.35f, y1 - corner * 0.5f)
        close()
    }
    drawPath(
        path = leftFacet,
        color = jewelColor.lightFacet.copy(alpha = 0.35f * alpha)
    )

    // Bottom deep shadow facet
    val bottomFacet = Path().apply {
        moveTo(ix0, iy1)
        lineTo(ix1, iy1)
        lineTo(x1 - corner * 0.5f, y1 - corner * 0.35f)
        lineTo(x0 + corner * 0.5f, y1 - corner * 0.35f)
        close()
    }
    drawPath(
        path = bottomFacet,
        color = Color.Black.copy(alpha = 0.32f * alpha)
    )

    // Right shadow facet
    val rightFacet = Path().apply {
        moveTo(ix1, iy0)
        lineTo(x1 - corner * 0.35f, y0 + corner * 0.5f)
        lineTo(x1 - corner * 0.35f, y1 - corner * 0.5f)
        lineTo(ix1, iy1)
        close()
    }
    drawPath(
        path = rightFacet,
        color = jewelColor.darkFacet.copy(alpha = 0.55f * alpha)
    )

    // Inner glowing core plate
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                jewelColor.lightFacet.copy(alpha = 0.42f * alpha),
                jewelColor.base.copy(alpha = 0.82f * alpha)
            ),
            center = Offset(x0 + cubeSize * 0.42f, y0 + cubeSize * 0.40f),
            radius = cubeSize * 0.55f
        ),
        topLeft = Offset(ix0, iy0),
        size = Size(cubeSize - bevel * 2f, cubeSize - bevel * 2f),
        cornerRadius = CornerRadius(corner * 0.45f, corner * 0.45f)
    )

    // Crisp specular edge highlight
    drawRoundRect(
        color = Color.White.copy(alpha = 0.38f * alpha),
        topLeft = Offset(x0 + 1f, y0 + 1f),
        size = Size(cubeSize - 2f, cubeSize - 2f),
        cornerRadius = CornerRadius(corner, corner),
        style = Stroke(width = 1.2f)
    )

    // Top-left glint sparkle
    drawCircle(
        color = Color.White.copy(alpha = 0.65f * alpha),
        radius = (cubeSize * 0.065f).coerceAtLeast(1.5f),
        center = Offset(x0 + bevel * 0.95f, y0 + bevel * 0.95f)
    )
}

private data class FloatingCubeSpec(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val sizeFraction: Float,
    val rotationSpeed: Float,
    val initialAngle: Float,
    val color: JewelColor,
    val alpha: Float
)

@Composable
fun AnimatedJewelBackground(modifier: Modifier = Modifier) {
    val cubes = remember {
        val rng = Random(2026)
        List(16) { idx ->
            FloatingCubeSpec(
                xFraction = (idx + 0.5f) / 16f + (rng.nextFloat() - 0.5f) * 0.05f,
                yOffset = rng.nextFloat(),
                speed = 0.35f + rng.nextFloat() * 0.55f,
                sizeFraction = 0.045f + rng.nextFloat() * 0.055f,
                rotationSpeed = (if (idx % 2 == 0) 1f else -1f) * (15f + rng.nextFloat() * 25f),
                initialAngle = rng.nextFloat() * 360f,
                color = JewelColor.entries[idx % JewelColor.entries.size],
                alpha = 0.14f + rng.nextFloat() * 0.18f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_cubes")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bg_phase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep obsidian radial sky
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    ObsidianMid,
                    ObsidianDeep
                ),
                center = Offset(size.width * 0.5f, size.height * 0.28f),
                radius = size.maxDimension * 0.85f
            )
        )

        // Subtle ambient jewel glow pools
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(JewelColor.SAPPHIRE.glow.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.18f),
                radius = size.width * 0.55f
            ),
            radius = size.width * 0.55f,
            center = Offset(size.width * 0.15f, size.height * 0.18f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(JewelColor.AMETHYST.glow.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.78f),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.85f, size.height * 0.78f)
        )

        // Floating drifting 3D jewel blocks
        for (cube in cubes) {
            val cubeSize = size.minDimension * cube.sizeFraction
            val travel = (cube.yOffset + phase * cube.speed) % 1.2f
            val y = size.height - travel * (size.height + cubeSize * 2f) + cubeSize
            val sway = sin((phase * 2f * PI + cube.yOffset * 6f).toDouble()).toFloat() * (size.width * 0.025f)
            val x = cube.xFraction * size.width + sway
            val angle = cube.initialAngle + phase * cube.rotationSpeed * 6f

            withTransform({
                rotate(degrees = angle, pivot = Offset(x + cubeSize / 2f, y + cubeSize / 2f))
            }) {
                drawJewelCube(
                    topLeft = Offset(x, y),
                    cubeSize = cubeSize,
                    jewelColor = cube.color,
                    alpha = cube.alpha
                )
            }
        }
    }
}
