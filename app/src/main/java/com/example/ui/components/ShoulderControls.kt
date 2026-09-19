package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan

@Composable
fun ShoulderBumper(
    modifier: Modifier = Modifier,
    label: String,
    width: Dp = 90.dp,
    height: Dp = 38.dp,
    accentColor: Color = ElectricCyan,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(50),
        label = "bumper_scale"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .scale(scale)
            .testTag("bumper_$label")
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed) accentColor.copy(alpha = 0.4f) else DarkSurfaceVariant,
                        if (isPressed) accentColor.copy(alpha = 0.15f) else Color(0xFF131A29)
                    )
                )
            )
            .border(
                width = if (isPressed) 2.dp else 1.dp,
                color = if (isPressed) accentColor else DarkBorder,
                shape = RoundedCornerShape(8.dp)
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
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPressed) Color.White else Color(0xFFE2E8F0)
        )
    }
}

@Composable
fun ShoulderGroup(
    bumperLabel: String,
    triggerLabel: String,
    accentColor: Color = ElectricCyan,
    onBumperChange: (Boolean) -> Unit,
    onTriggerChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShoulderBumper(
            label = bumperLabel,
            width = 76.dp,
            height = 36.dp,
            accentColor = accentColor,
            onPressChange = onBumperChange
        )
        ShoulderBumper(
            label = triggerLabel,
            width = 76.dp,
            height = 36.dp,
            accentColor = accentColor,
            onPressChange = onTriggerChange
        )
    }
}
