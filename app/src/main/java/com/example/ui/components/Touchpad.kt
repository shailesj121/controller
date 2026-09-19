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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VividIndigo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Authentic Gaming Trackpad component.
 *
 * True Trackpad Characteristics:
 * 1. Physical Laptop/PlayStation Trackpad styling with recessed bezel, frosted glass/matte surface,
 *    and physical mechanical Left & Right click buttons.
 * 2. Relative Delta Movement: Does NOT behave like a joystick. Touching the corner does not tilt
 *    a stick. Swiping your finger produces relative camera/cursor displacement based on finger velocity.
 * 3. Stopping finger movement instantly stops rotation/movement. Lifting and swiping repeatedly
 *    allows 360-degree rotation (clutching/swiping).
 * 4. Distinct Tap-to-Click and physical Left/Right Click button bar at the bottom.
 */
@Composable
fun Touchpad(
    modifier: Modifier = Modifier,
    width: Dp = 210.dp,
    height: Dp = 120.dp,
    label: String = "TRACKPAD",
    sensitivity: Float = 0.08f,
    enabled: Boolean = true,
    onRelativeMove: (deltaX: Float, deltaY: Float) -> Unit,
    onClickChange: (isPressed: Boolean) -> Unit,
    onRightClickChange: ((isPressed: Boolean) -> Unit)? = null,
    onTriggerHaptic: (() -> Unit)? = null
) {
    var isTouchingSurface by remember { mutableStateOf(false) }
    var isLeftClicked by remember { mutableStateOf(false) }
    var isRightClicked by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var autoZeroJob by remember { mutableStateOf<Job?>(null) }

    // Virtual cursor position for genuine trackpad pointer visualization
    var cursorX by remember { mutableFloatStateOf(0.5f) }
    var cursorY by remember { mutableFloatStateOf(0.5f) }

    // Touch contact position on surface for subtle diffused touch illumination
    var touchPos by remember { mutableStateOf<Offset?>(null) }

    val padDepressOffset by animateFloatAsState(
        targetValue = if (isLeftClicked || isRightClicked) 1.5f else 0f,
        animationSpec = tween(50),
        label = "pad_depress"
    )

    // Outer Recessed Enclosure
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .testTag("controller_touchpad")
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0C101A))
            .border(width = 1.dp, color = DarkBorder, shape = RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- Upper Touch Sensor Surface ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = padDepressOffset.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isTouchingSurface) Color(0xFF1C2538) else Color(0xFF141C2B),
                                if (isTouchingSurface) Color(0xFF182030) else Color(0xFF0F1622)
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        color = if (isTouchingSurface) ElectricCyan.copy(alpha = 0.6f) else Color(0xFF222D42),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                    )
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                            val pointerId = down.id
                            var lastPos = down.position
                            val downTime = System.currentTimeMillis()
                            var totalTravel = 0f

                            isTouchingSurface = true
                            touchPos = down.position

                            try {
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.find { it.id == pointerId }
                                    if (change == null || !change.pressed) {
                                        break
                                    }

                                    val curPos = change.position
                                    val dx = curPos.x - lastPos.x
                                    val dy = curPos.y - lastPos.y
                                    lastPos = curPos
                                    touchPos = curPos

                                    totalTravel += (abs(dx) + abs(dy))

                                    // Move virtual pointer on trackpad
                                    val w = size.width.toFloat()
                                    val h = size.height.toFloat()
                                    if (w > 0 && h > 0) {
                                        cursorX = (cursorX + (dx / w) * 1.5f).coerceIn(0.05f, 0.95f)
                                        cursorY = (cursorY + (dy / h) * 1.5f).coerceIn(0.05f, 0.95f)
                                    }

                                    // True trackpad relative impulse:
                                    // 1. Calculate displacement distance
                                    val moveDistance = kotlin.math.hypot(dx, dy)
                                    // 2. Jitter deadzone filters micro-tremors of resting finger (prevents sticky joystick drift)
                                    val jitterThreshold = 1.2f

                                    if (moveDistance >= jitterThreshold) {
                                        autoZeroJob?.cancel()
                                        val scaledDx = (dx * sensitivity).coerceIn(-1f, 1f)
                                        val scaledDy = (dy * sensitivity).coerceIn(-1f, 1f)
                                        onRelativeMove(scaledDx, scaledDy)

                                        // Fallback auto-zero timer in case Android stops sending events while finger is held completely motionless
                                        autoZeroJob = coroutineScope.launch {
                                            delay(25)
                                            onRelativeMove(0f, 0f)
                                        }
                                    } else {
                                        // Finger stopped moving or resting motionless -> IMMEDIATELY kill movement on screen!
                                        autoZeroJob?.cancel()
                                        onRelativeMove(0f, 0f)
                                    }
                                } while (true)
                            } finally {
                                isTouchingSurface = false
                                touchPos = null
                                autoZeroJob?.cancel()
                                // Return relative displacement to 0 immediately when finger stops/lifts
                                onRelativeMove(0f, 0f)

                                // Tap-to-Click detection (fast tap with minimal movement)
                                val elapsed = System.currentTimeMillis() - downTime
                                if (elapsed < 200 && totalTravel < 16f) {
                                    onTriggerHaptic?.invoke()
                                    onClickChange(true)
                                    // Auto release tap click
                                    isLeftClicked = false
                                    onClickChange(false)
                                }
                            }
                        }
                    }
                } else Modifier
            )
        ) {
            // Frosted Glass / Trackpad Canvas Texture
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Satin horizontal micro-texture lines (authentic metallic laptop trackpad finish)
                    val lineSpacing = 16.dp.toPx()
                    var y = lineSpacing
                    while (y < h) {
                        drawLine(
                            color = Color(0x0AFFFFFF),
                            start = Offset(8.dp.toPx(), y),
                            end = Offset(w - 8.dp.toPx(), y),
                            strokeWidth = 0.5.dp.toPx()
                        )
                        y += lineSpacing
                    }

                    // Corner precision registration ticks (characteristic of DualSense & precision pads)
                    val tickLen = 6.dp.toPx()
                    val tickColor = Color(0x3300E5FF)
                    // Top-Left
                    drawLine(tickColor, Offset(6.dp.toPx(), 6.dp.toPx()), Offset(6.dp.toPx() + tickLen, 6.dp.toPx()), 1.dp.toPx())
                    drawLine(tickColor, Offset(6.dp.toPx(), 6.dp.toPx()), Offset(6.dp.toPx(), 6.dp.toPx() + tickLen), 1.dp.toPx())
                    // Top-Right
                    drawLine(tickColor, Offset(w - 6.dp.toPx(), 6.dp.toPx()), Offset(w - 6.dp.toPx() - tickLen, 6.dp.toPx()), 1.dp.toPx())
                    drawLine(tickColor, Offset(w - 6.dp.toPx(), 6.dp.toPx()), Offset(w - 6.dp.toPx(), 6.dp.toPx() + tickLen), 1.dp.toPx())
                    // Bottom-Left
                    drawLine(tickColor, Offset(6.dp.toPx(), h - 6.dp.toPx()), Offset(6.dp.toPx() + tickLen, h - 6.dp.toPx()), 1.dp.toPx())
                    drawLine(tickColor, Offset(6.dp.toPx(), h - 6.dp.toPx()), Offset(6.dp.toPx(), h - 6.dp.toPx() - tickLen), 1.dp.toPx())
                    // Bottom-Right
                    drawLine(tickColor, Offset(w - 6.dp.toPx(), h - 6.dp.toPx()), Offset(w - 6.dp.toPx() - tickLen, h - 6.dp.toPx()), 1.dp.toPx())
                    drawLine(tickColor, Offset(w - 6.dp.toPx(), h - 6.dp.toPx()), Offset(w - 6.dp.toPx(), h - 6.dp.toPx() - tickLen), 1.dp.toPx())

                    // Center tactile registration cross
                    val cx = w / 2f
                    val cy = h / 2f
                    val crossSize = 5.dp.toPx()
                    drawLine(Color(0x2AFFFFFF), Offset(cx - crossSize, cy), Offset(cx + crossSize, cy), 1.dp.toPx())
                    drawLine(Color(0x2AFFFFFF), Offset(cx, cy - crossSize), Offset(cx, cy + crossSize), 1.dp.toPx())

                    // Soft diffuse contact illumination (NOT a joystick circle knob!)
                    touchPos?.let { pos ->
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ElectricCyan.copy(alpha = 0.25f),
                                    ElectricCyan.copy(alpha = 0.05f),
                                    Color.Transparent
                                ),
                                center = pos,
                                radius = 22.dp.toPx()
                            ),
                            radius = 22.dp.toPx(),
                            center = pos
                        )
                    }

                    // Genuine Trackpad Cursor Arrow
                    val curPx = Offset(cursorX * w, cursorY * h)
                    val cursorPath = Path().apply {
                        moveTo(curPx.x, curPx.y)
                        lineTo(curPx.x + 10.dp.toPx(), curPx.y + 7.dp.toPx())
                        lineTo(curPx.x + 5.dp.toPx(), curPx.y + 7.dp.toPx())
                        lineTo(curPx.x + 8.dp.toPx(), curPx.y + 13.dp.toPx())
                        lineTo(curPx.x + 5.5.dp.toPx(), curPx.y + 14.5.dp.toPx())
                        lineTo(curPx.x + 2.5.dp.toPx(), curPx.y + 8.5.dp.toPx())
                        lineTo(curPx.x, curPx.y + 11.dp.toPx())
                        close()
                    }
                    drawPath(cursorPath, color = ElectricCyan)
                    drawPath(cursorPath, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 0.8.dp.toPx()))
                }

                // Minimalist Trackpad Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Mouse,
                            contentDescription = null,
                            tint = if (isTouchingSurface) ElectricCyan else TextTertiary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTouchingSurface) ElectricCyan else TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "SWIPE TO LOOK • TAP TO CLICK",
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextTertiary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // --- Bottom Physical Click Buttons Bar (L-Click & R-Click) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Physical Left Click Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 2.dp, topStart = 2.dp, topEnd = 2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (isLeftClicked) ElectricCyan.copy(alpha = 0.35f) else Color(0xFF192233),
                                    if (isLeftClicked) ElectricCyan.copy(alpha = 0.15f) else Color(0xFF101724)
                                )
                            )
                        )
                        .border(
                            width = if (isLeftClicked) 1.5.dp else 0.8.dp,
                            color = if (isLeftClicked) ElectricCyan else DarkBorder,
                            shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 2.dp, topStart = 2.dp, topEnd = 2.dp)
                        )
                        .then(
                            if (enabled) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isLeftClicked = true
                                        onTriggerHaptic?.invoke()
                                        onClickChange(true)
                                        try {
                                            waitForUpOrCancellation()
                                        } finally {
                                            isLeftClicked = false
                                            onClickChange(false)
                                        }
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LEFT CLICK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLeftClicked) Color.White else TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Physical Right Click Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 2.dp, topEnd = 2.dp, topStart = 2.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    if (isRightClicked) VividIndigo.copy(alpha = 0.40f) else Color(0xFF192233),
                                    if (isRightClicked) VividIndigo.copy(alpha = 0.20f) else Color(0xFF101724)
                                )
                            )
                        )
                        .border(
                            width = if (isRightClicked) 1.5.dp else 0.8.dp,
                            color = if (isRightClicked) VividIndigo else DarkBorder,
                            shape = RoundedCornerShape(bottomEnd = 8.dp, bottomStart = 2.dp, topEnd = 2.dp, topStart = 2.dp)
                        )
                        .then(
                            if (enabled) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        isRightClicked = true
                                        onTriggerHaptic?.invoke()
                                        onRightClickChange?.invoke(true)
                                        try {
                                            waitForUpOrCancellation()
                                        } finally {
                                            isRightClicked = false
                                            onRightClickChange?.invoke(false)
                                        }
                                    }
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RIGHT CLICK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isRightClicked) Color.White else TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}
