package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.example.ui.theme.TextPrimary

/**
 * Individual Standalone Action Button (A, B, X, Y).
 * Consumes pointer touches so the underlying touch pad zone is not falsely triggered.
 */
@Composable
fun IndividualActionButton(
    modifier: Modifier = Modifier,
    label: String,
    accentColor: Color,
    size: Dp = 56.dp,
    enabled: Boolean = true,
    testTag: String = "individual_btn_$label",
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1.0f,
        animationSpec = tween(40),
        label = "indiv_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .testTag(testTag)
            .shadow(
                elevation = if (isPressed) 2.dp else 6.dp,
                shape = CircleShape,
                spotColor = accentColor
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed && enabled) accentColor.copy(alpha = 0.5f) else Color(0xFF1E283C),
                        if (isPressed && enabled) accentColor.copy(alpha = 0.25f) else Color(0xFF101622)
                    )
                )
            )
            .border(
                width = if (isPressed && enabled) 2.5.dp else 1.5.dp,
                color = if (isPressed && enabled) accentColor else accentColor.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(label) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            down.consume()

                            val radius = this.size.width.toFloat() / 2f
                            val maxRadius = radius * 1.4f
                            val center = Offset(radius, radius)

                            isPressed = true
                            onPressChange(true)

                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    val dist = (change.position - center).getDistance()
                                    if (dist > maxRadius) {
                                        break
                                    }
                                } while (true)
                            } finally {
                                isPressed = false
                                onPressChange(false)
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Black,
            color = if (isPressed && enabled) Color.White else accentColor
        )
    }
}

/**
 * Individual Standalone Shoulder Bumper / Trigger Button (L1, L2, R1, R2).
 * Consumes pointer touches so the underlying touch pad zone is not falsely triggered.
 */
@Composable
fun IndividualShoulderButton(
    modifier: Modifier = Modifier,
    label: String,
    accentColor: Color = ElectricCyan,
    width: Dp = 80.dp,
    height: Dp = 42.dp,
    enabled: Boolean = true,
    testTag: String = "individual_shoulder_$label",
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1.0f,
        animationSpec = tween(40),
        label = "indiv_shoulder_scale"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .scale(scale)
            .testTag(testTag)
            .shadow(
                elevation = if (isPressed) 2.dp else 5.dp,
                shape = RoundedCornerShape(10.dp),
                spotColor = accentColor
            )
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed && enabled) accentColor.copy(alpha = 0.5f) else Color(0xFF1E283C),
                        if (isPressed && enabled) accentColor.copy(alpha = 0.2f) else Color(0xFF101622)
                    )
                )
            )
            .border(
                width = if (isPressed && enabled) 2.dp else 1.2.dp,
                color = if (isPressed && enabled) accentColor else accentColor.copy(alpha = 0.55f),
                shape = RoundedCornerShape(10.dp)
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(label) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            down.consume()

                            val w = this.size.width.toFloat()
                            val h = this.size.height.toFloat()
                            val slack = 28f

                            isPressed = true
                            onPressChange(true)

                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }
                                    change.consume()
                                    if (change.position.x < -slack || change.position.x > w + slack ||
                                        change.position.y < -slack || change.position.y > h + slack
                                    ) {
                                        break
                                    }
                                } while (true)
                            } finally {
                                isPressed = false
                                onPressChange(false)
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = (height.value * 0.40f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isPressed && enabled) Color.White else TextPrimary
        )
    }
}
