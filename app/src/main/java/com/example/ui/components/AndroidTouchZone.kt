package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VividIndigo
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Full-Surface Dynamic Touchpad Zone.
 *
 * Characteristics:
 * 1. Screen-half touch surface: Touching ANYWHERE sets an intuitive dynamic anchor origin.
 * 2. Finger movement immediately drives continuous analog deflection (-1.0 to 1.0) without needing
 *    a rigid, fixed joystick circle on screen.
 * 3. Mobile FPS-style anchor tracking: If the finger travels past the max radius, the center
 *    gently tracks behind the finger so the player never loses control or runs off the boundary.
 * 4. Sleek visual feedback: Translucent glowing reticle, directional vector line, and illuminated thumb knob.
 * 5. Consumes only touches not claimed by overlay buttons.
 */
@Composable
fun AndroidTouchZone(
    modifier: Modifier = Modifier,
    label: String = "TOUCH TO MOVE",
    subLabel: String = "Left Half Touchpad",
    accentColor: Color = ElectricCyan,
    maxRadius: Dp = 64.dp,
    deadzone: Float = 0.08f,
    enabled: Boolean = true,
    isRelativeDeltaMode: Boolean = false,
    testTag: String = "android_touch_zone",
    onMove: (x: Float, y: Float) -> Unit,
    onTriggerHaptic: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val maxRadiusPx = with(density) { maxRadius.toPx() }

    var isTouching by remember { mutableStateOf(false) }
    var anchorPoint by remember { mutableStateOf(Offset.Zero) }
    var currentPoint by remember { mutableStateOf(Offset.Zero) }

    val visualAlpha by animateFloatAsState(
        targetValue = if (isTouching) 1.0f else 0.0f,
        animationSpec = tween(durationMillis = 120),
        label = "touch_zone_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(testTag)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isTouching) 0.08f else 0.02f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = if (isTouching) 0.35f else 0.12f),
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(isRelativeDeltaMode, maxRadiusPx) {
                        awaitEachGesture {
                            // Require unconsumed touch so overlay buttons on top take priority!
                            val down = awaitFirstDown(requireUnconsumed = true)
                            val pointerId = down.id
                            down.consume()

                            isTouching = true
                            anchorPoint = down.position
                            currentPoint = down.position
                            onTriggerHaptic?.invoke()

                            var lastPoint = down.position

                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    val curPos = change.position
                                    currentPoint = curPos

                                    if (isRelativeDeltaMode) {
                                        // Trackpad relative delta mode (for look / aim swiping)
                                        val dx = (curPos.x - lastPoint.x) * 0.08f
                                        val dy = (curPos.y - lastPoint.y) * 0.08f
                                        lastPoint = curPos
                                        onMove(dx.coerceIn(-1f, 1f), dy.coerceIn(-1f, 1f))
                                    } else {
                                        // Dynamic Floating Anchor Analog Stick mode (for character movement)
                                        var rawDx = curPos.x - anchorPoint.x
                                        var rawDy = curPos.y - anchorPoint.y
                                        val dist = hypot(rawDx, rawDy)

                                        // Dynamic floating tracking: If finger drags further than maxRadius,
                                        // pull the anchor along so the user doesn't lose range
                                        if (dist > maxRadiusPx) {
                                            val angle = kotlin.math.atan2(rawDy, rawDx)
                                            val excess = dist - maxRadiusPx
                                            anchorPoint = Offset(
                                                x = anchorPoint.x + cos(angle) * excess,
                                                y = anchorPoint.y + sin(angle) * excess
                                            )
                                            rawDx = curPos.x - anchorPoint.x
                                            rawDy = curPos.y - anchorPoint.y
                                        }

                                        val currentDist = hypot(rawDx, rawDy)
                                        if (currentDist / maxRadiusPx < deadzone) {
                                            onMove(0f, 0f)
                                        } else {
                                            val normalizedMagnitude = (currentDist / maxRadiusPx).coerceIn(0f, 1f)
                                            val angle = kotlin.math.atan2(rawDy, rawDx)
                                            val normX = cos(angle) * normalizedMagnitude
                                            val normY = sin(angle) * normalizedMagnitude
                                            onMove(normX, normY)
                                        }
                                    }
                                } while (true)
                            } finally {
                                isTouching = false
                                onMove(0f, 0f)
                            }
                        }
                    }
                } else Modifier
            )
    ) {
        // Subtle watermark helper text when resting
        if (!isTouching) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(8.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        color = accentColor.copy(alpha = 0.35f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = subLabel,
                        color = TextTertiary.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Dynamic Floating Reticle HUD
        if (visualAlpha > 0.01f && isTouching) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val alpha = visualAlpha

                // Outer Anchor Base Ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.15f * alpha),
                    radius = maxRadiusPx,
                    center = anchorPoint
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.6f * alpha),
                    radius = maxRadiusPx,
                    center = anchorPoint,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Deadzone Inner Ring
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f * alpha),
                    radius = maxRadiusPx * deadzone * 2.5f,
                    center = anchorPoint,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Vector Trail from Center to Thumb
                drawLine(
                    color = accentColor.copy(alpha = 0.7f * alpha),
                    start = anchorPoint,
                    end = currentPoint,
                    strokeWidth = 3.dp.toPx()
                )

                // Glowing Thumb Knob
                drawCircle(
                    color = accentColor.copy(alpha = 0.35f * alpha),
                    radius = 28.dp.toPx(),
                    center = currentPoint
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * alpha),
                    radius = 16.dp.toPx(),
                    center = currentPoint
                )
                drawCircle(
                    color = accentColor.copy(alpha = 0.95f * alpha),
                    radius = 16.dp.toPx(),
                    center = currentPoint,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }
        }
    }
}
