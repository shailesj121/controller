package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.StickBaseColor
import com.example.ui.theme.StickCapColor
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun AnalogStick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    label: String = "L",
    deadzone: Float = 0.08f,
    enabled: Boolean = true,
    onMove: (x: Float, y: Float) -> Unit,
    onPressL3R3: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val animatedOffsetX = remember { Animatable(0f) }
    val animatedOffsetY = remember { Animatable(0f) }

    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("analog_stick_$label")
            .clip(CircleShape)
            .background(StickBaseColor)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        val radius = (size.toPx() / 2f)
                        val maxDistance = radius * 0.75f

                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            isDragging = true

                            fun updateStickPosition(pos: Offset) {
                                val center = Offset(radius, radius)
                                val delta = pos - center
                                val dist = delta.getDistance()
                                val angle = atan2(delta.y, delta.x)
                                val clampedDist = min(dist, maxDistance)

                                val curX = clampedDist * cos(angle)
                                val curY = clampedDist * sin(angle)

                                coroutineScope.launch {
                                    animatedOffsetX.snapTo(curX)
                                    animatedOffsetY.snapTo(curY)
                                }

                                // Normalize (-1.0 to 1.0)
                                val normX = (curX / maxDistance).coerceIn(-1f, 1f)
                                val normY = (curY / maxDistance).coerceIn(-1f, 1f)
                                val rawDist = sqrt(normX * normX + normY * normY)
                                if (rawDist < deadzone) {
                                    onMove(0f, 0f)
                                } else {
                                    onMove(normX, normY)
                                }
                            }

                            try {
                                updateStickPosition(down.position)

                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change != null && change.pressed) {
                                        change.consume()
                                        updateStickPosition(change.position)
                                    } else {
                                        break
                                    }
                                } while (change != null && change.pressed)
                            } finally {
                                isDragging = false
                                coroutineScope.launch {
                                    animatedOffsetX.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 600f))
                                }
                                coroutineScope.launch {
                                    animatedOffsetY.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 600f))
                                }
                                onMove(0f, 0f)
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        // Base plate graphic with concentric tactile notches
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.width / 2f

            // Outer ring
            drawCircle(
                color = if (isDragging) ElectricCyan.copy(alpha = 0.4f) else Color(0xFF223048),
                radius = baseRadius - 2f,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Inner guide ring
            drawCircle(
                color = Color(0xFF1E293B),
                radius = baseRadius * 0.5f,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Cross indicators (Up/Down/Left/Right markers)
            val markerLen = 8.dp.toPx()
            val markerOffset = baseRadius * 0.72f
            val markerColor = Color(0xFF475569)

            // Top
            drawLine(markerColor, Offset(center.x, center.y - markerOffset), Offset(center.x, center.y - markerOffset + markerLen), strokeWidth = 2f)
            // Bottom
            drawLine(markerColor, Offset(center.x, center.y + markerOffset - markerLen), Offset(center.x, center.y + markerOffset), strokeWidth = 2f)
            // Left
            drawLine(markerColor, Offset(center.x - markerOffset, center.y), Offset(center.x - markerOffset + markerLen, center.y), strokeWidth = 2f)
            // Right
            drawLine(markerColor, Offset(center.x + markerOffset - markerLen, center.y), Offset(center.x + markerOffset, center.y), strokeWidth = 2f)
        }

        // Thumbstick Cap
        val capSize = size * 0.46f
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        animatedOffsetX.value.roundToInt(),
                        animatedOffsetY.value.roundToInt()
                    )
                }
                .size(capSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDragging) ElectricCyan.copy(alpha = 0.25f) else Color(0xFF334155),
                            StickCapColor,
                            Color(0xFF0F172A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val capRadius = this.size.width / 2f
                // Edge rim
                drawCircle(
                    color = if (isDragging) ElectricCyan else Color(0xFF64748B),
                    radius = capRadius - 2f,
                    style = Stroke(width = if (isDragging) 2.5.dp.toPx() else 1.5.dp.toPx())
                )
                // Grip texture rings
                drawCircle(
                    color = Color(0xFF475569),
                    radius = capRadius * 0.65f,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    color = if (isDragging) ElectricCyan else Color(0xFF94A3B8)
                )
            )
        }
    }
}
