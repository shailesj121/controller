package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DpadColor
import com.example.ui.theme.ElectricCyan
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun DPad(
    modifier: Modifier = Modifier,
    dpadSize: Dp = 150.dp,
    onDirectionChange: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit
) {
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    var leftPressed by remember { mutableStateOf(false) }
    var rightPressed by remember { mutableStateOf(false) }

    fun updateDirection(up: Boolean, down: Boolean, left: Boolean, right: Boolean) {
        if (up != upPressed || down != downPressed || left != leftPressed || right != rightPressed) {
            upPressed = up
            downPressed = down
            leftPressed = left
            rightPressed = right
            onDirectionChange(up, down, left, right)
        }
    }

    fun handleOffset(offset: Offset, width: Float, height: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val dx = offset.x - centerX
        val dy = offset.y - centerY
        val dist = Offset(dx, dy).getDistance()
        val deadzone = width * 0.12f

        if (dist < deadzone) {
            updateDirection(false, false, false, false)
            return
        }

        val halfArmW = width * 0.36f / 2f
        val halfArmH = height * 0.36f / 2f

        // 1. Direct hit on vertical cross arm -> Pure Up or Down (no horizontal bleed)
        if (abs(dx) <= halfArmW) {
            if (dy < -deadzone) {
                updateDirection(up = true, down = false, left = false, right = false)
                return
            } else if (dy > deadzone) {
                updateDirection(up = false, down = true, left = false, right = false)
                return
            }
        }

        // 2. Direct hit on horizontal cross arm -> Pure Left or Right (no vertical bleed)
        if (abs(dy) <= halfArmH) {
            if (dx < -deadzone) {
                updateDirection(up = false, down = false, left = true, right = false)
                return
            } else if (dx > deadzone) {
                updateDirection(up = false, down = false, left = false, right = true)
                return
            }
        }

        // 3. Diagonal quadrants: calculate angle (-180 to 180) with clear non-overlapping sectors
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        when {
            // Pure Right (-30° to 30°)
            angle in -30f..30f -> updateDirection(up = false, down = false, left = false, right = true)
            // Down-Right (30° to 60°)
            angle in 30f..60f -> updateDirection(up = false, down = true, left = false, right = true)
            // Pure Down (60° to 120°)
            angle in 60f..120f -> updateDirection(up = false, down = true, left = false, right = false)
            // Down-Left (120° to 150°)
            angle in 120f..150f -> updateDirection(up = false, down = true, left = true, right = false)
            // Pure Left (> 150° or < -150°)
            angle > 150f || angle < -150f -> updateDirection(up = false, down = false, left = true, right = false)
            // Up-Left (-150° to -120°)
            angle in -150f..-120f -> updateDirection(up = true, down = false, left = true, right = false)
            // Pure Up (-120° to -60°)
            angle in -120f..-60f -> updateDirection(up = true, down = false, left = false, right = false)
            // Up-Right (-60° to -30°)
            angle in -60f..-30f -> updateDirection(up = true, down = false, left = false, right = true)
            else -> updateDirection(false, false, false, false)
        }
    }

    Box(
        modifier = modifier
            .size(dpadSize)
            .testTag("dpad_controller")
            .pointerInput(Unit) {
                val inputWidth = size.width.toFloat()
                val inputHeight = size.height.toFloat()

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pointerId = down.id
                    handleOffset(down.position, inputWidth, inputHeight)

                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.find { it.id == pointerId }
                        if (change != null && change.pressed) {
                            change.consume()
                            handleOffset(change.position, inputWidth, inputHeight)
                        } else {
                            break
                        }
                    } while (change != null && change.pressed)

                    updateDirection(false, false, false, false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Cross drawing
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val armW = w * 0.36f
            val armH = h * 0.36f
            val cornerR = 8.dp.toPx()

            val horizRect = Rect(0f, (h - armH) / 2f, w, (h + armH) / 2f)
            val vertRect = Rect((w - armW) / 2f, 0f, (w + armW) / 2f, h)

            // Draw Base Cross
            drawRoundRect(
                color = DpadColor,
                topLeft = horizRect.topLeft,
                size = horizRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
            )
            drawRoundRect(
                color = DpadColor,
                topLeft = vertRect.topLeft,
                size = vertRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
            )

            // Outer border
            drawRoundRect(
                color = DarkBorder,
                topLeft = horizRect.topLeft,
                size = horizRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
                style = Stroke(width = 2.dp.toPx())
            )
            drawRoundRect(
                color = DarkBorder,
                topLeft = vertRect.topLeft,
                size = vertRect.size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
                style = Stroke(width = 2.dp.toPx())
            )

            // Highlight active arms
            if (upPressed) {
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.35f),
                    topLeft = Offset(vertRect.left, vertRect.top),
                    size = Size(armW, (h - armH) / 2f + 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
            }
            if (downPressed) {
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.35f),
                    topLeft = Offset(vertRect.left, (h + armH) / 2f - 4.dp.toPx()),
                    size = Size(armW, (h - armH) / 2f + 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
            }
            if (leftPressed) {
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.35f),
                    topLeft = Offset(horizRect.left, horizRect.top),
                    size = Size((w - armW) / 2f + 4.dp.toPx(), armH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
            }
            if (rightPressed) {
                drawRoundRect(
                    color = ElectricCyan.copy(alpha = 0.35f),
                    topLeft = Offset((w + armW) / 2f - 4.dp.toPx(), horizRect.top),
                    size = Size((w - armW) / 2f + 4.dp.toPx(), armH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR)
                )
            }

            // Center indented circle
            drawCircle(
                color = Color(0xFF131A29),
                radius = armW * 0.40f
            )
        }

        // Direction Arrows
        Box(modifier = Modifier.size(dpadSize)) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "DPad Up",
                tint = if (upPressed) ElectricCyan else Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(28.dp)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "DPad Down",
                tint = if (downPressed) ElectricCyan else Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(28.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "DPad Left",
                tint = if (leftPressed) ElectricCyan else Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(28.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "DPad Right",
                tint = if (rightPressed) ElectricCyan else Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(28.dp)
            )
        }
    }
}
