package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
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
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VividIndigo

/**
 * High-performance, tactile gaming Touchpad styled after DualSense / DualShock 4.
 * Supports smooth finger swipe (for aiming / camera look) and physical click / tap.
 */
@Composable
fun Touchpad(
    modifier: Modifier = Modifier,
    width: Dp = 200.dp,
    height: Dp = 110.dp,
    label: String = "TOUCHPAD",
    onSwipe: (deltaX: Float, deltaY: Float, normX: Float, normY: Float) -> Unit,
    onClickChange: (isPressed: Boolean) -> Unit,
    onTriggerHaptic: (() -> Unit)? = null
) {
    var isTouching by remember { mutableStateOf(false) }
    var touchPos by remember { mutableStateOf<Offset?>(null) }
    var isClicked by remember { mutableStateOf(false) }

    val pressScale by animateFloatAsState(
        targetValue = if (isClicked) 0.96f else 1.0f,
        animationSpec = tween(60),
        label = "touchpad_scale"
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .scale(pressScale)
            .testTag("controller_touchpad")
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isTouching) DarkSurfaceVariant else Color(0xFF141A28),
                        if (isTouching) Color(0xFF161F33) else DarkSurface
                    )
                )
            )
            .border(
                width = if (isTouching || isClicked) 1.5.dp else 1.dp,
                color = if (isClicked) ElectricCyan else if (isTouching) VividIndigo else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    var lastPos = down.position
                    val padWidth = size.width.toFloat()
                    val padHeight = size.height.toFloat()

                    isTouching = true
                    touchPos = down.position
                    onTriggerHaptic?.invoke()

                    // Click detection: if user taps or holds bottom click zone
                    val isBottomZone = down.position.y > (padHeight * 0.70f)
                    if (isBottomZone) {
                        isClicked = true
                        onClickChange(true)
                    }

                    try {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.find { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                break
                            }

                            val curPos = change.position
                            val deltaX = curPos.x - lastPos.x
                            val deltaY = curPos.y - lastPos.y
                            lastPos = curPos

                            touchPos = curPos

                            // Normalized coordinates: -1.0 to +1.0
                            val normX = ((curPos.x / padWidth) * 2f - 1f).coerceIn(-1f, 1f)
                            val normY = ((curPos.y / padHeight) * 2f - 1f).coerceIn(-1f, 1f)

                            // Swipe sensitivity scale for smooth camera aiming
                            val sensitivity = 0.05f
                            onSwipe(deltaX * sensitivity, deltaY * sensitivity, normX, normY)
                        } while (true)
                    } finally {
                        isTouching = false
                        touchPos = null
                        if (isClicked) {
                            isClicked = false
                            onClickChange(false)
                        }
                        // Reset look delta
                        onSwipe(0f, 0f, 0f, 0f)
                    }
                }
            }
    ) {
        // Futuristic Touchpad Grid Canvas & Active Touch Pointer Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Subtle grid dots / pattern
            val step = 20.dp.toPx()
            var x = step
            while (x < w) {
                var y = step
                while (y < h - 16.dp.toPx()) {
                    drawCircle(
                        color = Color(0x1FFFFFFF),
                        radius = 1.dp.toPx(),
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }

            // DualShock bottom click divider bar
            val clickBarY = h * 0.78f
            drawLine(
                color = if (isClicked) ElectricCyan.copy(alpha = 0.5f) else DarkBorder.copy(alpha = 0.6f),
                start = Offset(16.dp.toPx(), clickBarY),
                end = Offset(w - 16.dp.toPx(), clickBarY),
                strokeWidth = 1.dp.toPx()
            )
            // Center notch
            drawLine(
                color = DarkBorder.copy(alpha = 0.6f),
                start = Offset(w / 2f, clickBarY),
                end = Offset(w / 2f, h - 4.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )

            // Finger touch glowing ripple & cursor
            touchPos?.let { pos ->
                // Outer glow
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.20f),
                    radius = 28.dp.toPx(),
                    center = pos
                )
                // Inner ring
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.75f),
                    radius = 14.dp.toPx(),
                    center = pos,
                    style = Stroke(width = 2.dp.toPx())
                )
                // Core dot
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = pos
                )
            }
        }

        // Center labels & icon
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = if (isTouching) ElectricCyan else TextTertiary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isTouching) ElectricCyan else TextSecondary,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "SWIPE TO AIM • CLICK TO PRESS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = TextTertiary,
                letterSpacing = 0.5.sp
            )
        }

        // Left / Right click text at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "L-CLICK",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isClicked) ElectricCyan else TextTertiary
            )
            Text(
                "R-CLICK",
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isClicked) ElectricCyan else TextTertiary
            )
        }
    }
}
