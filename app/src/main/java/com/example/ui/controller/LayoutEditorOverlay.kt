package com.example.ui.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomLayoutConfig
import com.example.model.ElementLayoutConfig
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VividIndigo
import kotlin.math.roundToInt

/**
 * Wrapper for a controller element that supports dragging to reposition,
 * scaling, and selection outlines when in layout customization edit mode.
 *
 * When isEditMode is TRUE:
 * An overlay sits on top of content() so touches are captured cleanly for dragging
 * and selecting, without passing clicks to underlying buttons or moving joysticks.
 */
@Composable
fun CustomizableElement(
    modifier: Modifier = Modifier,
    elementKey: String,
    title: String,
    config: ElementLayoutConfig,
    isEditMode: Boolean,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDragOffsetDelta: (dx: Float, dy: Float) -> Unit,
    content: @Composable () -> Unit
) {
    if (!config.visible && !isEditMode) return

    val editBorderModifier = if (isEditMode) {
        Modifier
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricCyan else VividIndigo.copy(alpha = 0.5f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(4.dp)
    } else Modifier

    Box(
        modifier = modifier
            .offset {
                IntOffset(
                    (config.offsetX * density).roundToInt(),
                    (config.offsetY * density).roundToInt()
                )
            }
            .scale(config.scale)
            .then(editBorderModifier)
    ) {
        // Underneath: Original Gamepad Element
        content()

        // In Edit Mode: Transparent touch interceptor overlay covering the entire element
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(elementKey) {
                        awaitEachGesture {
                            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                            down.consume()
                            onSelect()

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                                val change = event.changes.find { it.id == pointerId }
                                if (change == null || !change.pressed) {
                                    break
                                }
                                val delta = change.positionChange()
                                if (delta != Offset.Zero) {
                                    change.consume()
                                    onDragOffsetDelta(delta.x / density, delta.y / density)
                                }
                            }
                        }
                    }
                    .background(if (isSelected) ElectricCyan.copy(alpha = 0.12f) else Color.Transparent)
            )

            // Edit Mode label & size percentage badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-15).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) ElectricCyan else VividIndigo)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$title (${(config.scale * 100).toInt()}%)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
            }
        }
    }
}

/**
 * Top floating toolbar providing size slider, touch pad toggle, reset, and save actions.
 * Sits as an overlay above the controller so the controller screen is NOT squeezed.
 */
@Composable
fun LayoutEditorBar(
    isEditMode: Boolean,
    selectedElementKey: String?,
    customLayout: CustomLayoutConfig,
    isLandscape: Boolean = true,
    onScaleChange: (scale: Float) -> Unit,
    onToggleTouchpad: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveAndExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMinimized by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isEditMode,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val currentConfig = selectedElementKey?.let { customLayout.getElement(it) } ?: ElementLayoutConfig()
        val elementName = when (selectedElementKey) {
            CustomLayoutConfig.KEY_LEFT_STICK -> "Left Stick"
            CustomLayoutConfig.KEY_RIGHT_STICK -> "Right Stick"
            CustomLayoutConfig.KEY_DPAD -> "D-Pad"
            CustomLayoutConfig.KEY_ACTION_BUTTONS -> "Action Buttons (ABXY)"
            CustomLayoutConfig.KEY_SHOULDER_LEFT -> "Shoulder L1 / L2"
            CustomLayoutConfig.KEY_SHOULDER_RIGHT -> "Shoulder R1 / R2"
            CustomLayoutConfig.KEY_CENTER_PILLS -> "Menu (Select/Start)"
            CustomLayoutConfig.KEY_TOUCHPAD -> "Touchpad"
            else -> "Tap any control to select"
        }

        if (isMinimized) {
            // Minimized sleek pill to give complete 100% view of the screen
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2141C2B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                    Text(
                        text = "Editing: $elementName",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                    // Expand Button
                    Button(
                        onClick = { isMinimized = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Controls", fontSize = 10.sp, color = TextPrimary)
                    }
                    // Quick Save Button
                    Button(
                        onClick = onSaveAndExit,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        } else {
            // Expanded HUD Bar
            Card(
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.88f else 0.98f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF2101624)),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, ElectricCyan.copy(alpha = 0.85f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            )
                            Text(
                                text = "LAYOUT CUSTOMIZER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "• Drag controls to move",
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }

                        // Action buttons: Minimize, Reset, Cancel, Save
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Minimize button
                            IconButton(
                                onClick = { isMinimized = true },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Minimize bar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }

                            // Reset defaults button
                            Button(
                                onClick = onResetDefaults,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reset", fontSize = 10.sp, color = TextSecondary)
                            }

                            // Cancel button
                            Button(
                                onClick = onCancel,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = CoralRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Cancel", fontSize = 10.sp, color = CoralRed)
                            }

                            // Save & Exit button
                            Button(
                                onClick = onSaveAndExit,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save & Exit", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }

                    // Controls adjustment row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Selected element label
                        Text(
                            text = "Selected: $elementName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selectedElementKey != null) TextPrimary else TextTertiary
                        )

                        // Size controls: - Button, Slider, + Button
                        if (selectedElementKey != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Size: ${(currentConfig.scale * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )

                                IconButton(
                                    onClick = { onScaleChange((currentConfig.scale - 0.05f).coerceIn(0.5f, 1.75f)) },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Smaller", tint = TextPrimary, modifier = Modifier.size(12.dp))
                                }

                                Slider(
                                    value = currentConfig.scale,
                                    onValueChange = onScaleChange,
                                    valueRange = 0.5f..1.75f,
                                    steps = 24,
                                    modifier = Modifier.width(100.dp),
                                    colors = SliderDefaults.colors(
                                        thumbColor = ElectricCyan,
                                        activeTrackColor = ElectricCyan,
                                        inactiveTrackColor = DarkBorder
                                    )
                                )

                                IconButton(
                                    onClick = { onScaleChange((currentConfig.scale + 0.05f).coerceIn(0.5f, 1.75f)) },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(DarkSurfaceVariant)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Larger", tint = TextPrimary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        // Touchpad toggle switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Touchpad",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (customLayout.touchpad.visible) ElectricCyan else TextTertiary
                            )
                            Switch(
                                checked = customLayout.touchpad.visible,
                                onCheckedChange = onToggleTouchpad,
                                modifier = Modifier.scale(0.70f),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ElectricCyan,
                                    checkedTrackColor = VividIndigo,
                                    uncheckedThumbColor = TextTertiary,
                                    uncheckedTrackColor = DarkSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
