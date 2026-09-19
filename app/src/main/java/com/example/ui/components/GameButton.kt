package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceVariant

@Composable
fun GameButton(
    modifier: Modifier = Modifier,
    label: String,
    size: Dp = 64.dp,
    accentColor: Color,
    testTag: String = "game_btn_$label",
    onPressChange: (isPressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = tween(durationMillis = 50),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .testTag(testTag)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed) accentColor.copy(alpha = 0.35f) else DarkSurfaceVariant,
                        if (isPressed) accentColor.copy(alpha = 0.15f) else Color(0xFF141B2B)
                    )
                )
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    isPressed = true
                    onPressChange(true)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId }
                        if (change == null || !change.pressed) {
                            break
                        }
                    } while (true)

                    isPressed = false
                    onPressChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Glowing outline & inner rim
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = this.size.width / 2f
            // Outer glow / rim
            drawCircle(
                color = if (isPressed) accentColor else accentColor.copy(alpha = 0.6f),
                radius = radius - 2f,
                style = Stroke(width = if (isPressed) 3.5.dp.toPx() else 2.dp.toPx())
            )

            if (isPressed) {
                // Flash highlight
                drawCircle(
                    color = accentColor.copy(alpha = 0.25f),
                    radius = radius - 4f
                )
            }
        }

        Text(
            text = label,
            fontSize = (size.value * 0.40f).sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.White else accentColor
        )
    }
}
