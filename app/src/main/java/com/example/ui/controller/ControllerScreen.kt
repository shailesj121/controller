package com.example.ui.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.atan2
import com.example.model.ConnectionMedium
import com.example.model.ControllerLayout
import com.example.model.CustomLayoutConfig
import com.example.model.ElementLayoutConfig
import com.example.model.GamepadState
import com.example.ui.components.AnalogStick
import com.example.ui.components.DPad
import com.example.ui.components.GameButton
import com.example.ui.components.ShoulderBumper
import com.example.ui.components.ShoulderGroup
import com.example.ui.components.Touchpad
import com.example.ui.components.AndroidTouchZone
import com.example.ui.components.IndividualActionButton
import com.example.ui.components.IndividualShoulderButton
import com.example.ui.controller.CustomizableElement
import com.example.ui.controller.LayoutEditorBar
import com.example.ui.theme.ButtonAColor
import com.example.ui.theme.ButtonBColor
import com.example.ui.theme.ButtonXColor
import com.example.ui.theme.ButtonYColor
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VividIndigo

@Composable
fun ControllerScreen(
    gamepadState: GamepadState,
    layout: ControllerLayout,
    customLayout: CustomLayoutConfig = CustomLayoutConfig(),
    isCustomEditMode: Boolean = false,
    selectedElementKey: String? = null,
    isConnected: Boolean,
    connectionMedium: ConnectionMedium = ConnectionMedium.WIFI,
    pingMs: Int,
    isHapticEnabled: Boolean,
    isGyroEnabled: Boolean,
    onStateUpdated: (GamepadState) -> Unit,
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit = { transform -> onStateUpdated(transform(gamepadState)) },
    onOpenSettings: () -> Unit,
    onSwitchToReceiver: () -> Unit,
    onTriggerHaptic: () -> Unit,
    onTriggerHapticTick: () -> Unit = onTriggerHaptic,
    onTriggerHapticHeavy: () -> Unit = onTriggerHaptic,
    onTriggerAccelerationPulse: (Float) -> Unit = {},
    onStopHaptic: () -> Unit = {},
    isRacingJoystickMode: Boolean = false,
    onToggleRacingJoystickMode: (Boolean) -> Unit = {},
    onToggleCustomize: () -> Unit = {},
    onSelectElement: (String) -> Unit = {},
    onDragElementDelta: (elementKey: String, dx: Float, dy: Float) -> Unit = { _, _, _ -> },
    onScaleElement: (scale: Float) -> Unit = {},
    onToggleTouchpad: (Boolean) -> Unit = {},
    onResetLayout: () -> Unit = {},
    onSaveLayout: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("controller_screen")
    ) {
        val isLandscape = maxWidth > maxHeight
        var showQuickSettings by remember { mutableStateOf(false) }

        // Controller Body: ALWAYS 100% Fullscreen - zero squeeze from toolbars!
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isLandscape) 14.dp else 8.dp,
                    vertical = if (isLandscape) 6.dp else 10.dp
                )
        ) {
            when (layout) {
                ControllerLayout.MODERN, ControllerLayout.CUSTOM -> ModernGamepadLayout(
                    state = gamepadState,
                    isLandscape = isLandscape,
                    customLayout = customLayout,
                    isEditMode = isCustomEditMode,
                    selectedElementKey = selectedElementKey,
                    onSelectElement = onSelectElement,
                    onDragOffsetDelta = onDragElementDelta,
                    onUpdateState = onUpdateState,
                    onTriggerHaptic = onTriggerHaptic
                )
                ControllerLayout.ANDROID_TOUCH -> AndroidTouchControllerLayout(
                    state = gamepadState,
                    isLandscape = isLandscape,
                    customLayout = customLayout,
                    isEditMode = isCustomEditMode,
                    selectedElementKey = selectedElementKey,
                    onSelectElement = onSelectElement,
                    onDragOffsetDelta = onDragElementDelta,
                    onUpdateState = onUpdateState,
                    onTriggerHaptic = onTriggerHaptic
                )
                ControllerLayout.RETRO_ARCADE -> RetroArcadeLayout(
                    state = gamepadState,
                    isLandscape = isLandscape,
                    onUpdateState = onUpdateState,
                    onTriggerHaptic = onTriggerHaptic
                )
                ControllerLayout.RACING -> RacingControllerLayout(
                    state = gamepadState,
                    isLandscape = isLandscape,
                    customLayout = customLayout,
                    isEditMode = isCustomEditMode,
                    selectedElementKey = selectedElementKey,
                    isJoystickMode = isRacingJoystickMode,
                    onToggleJoystickMode = onToggleRacingJoystickMode,
                    onSelectElement = onSelectElement,
                    onDragOffsetDelta = onDragElementDelta,
                    onUpdateState = onUpdateState,
                    onTriggerHaptic = onTriggerHaptic,
                    onTriggerHapticTick = onTriggerHapticTick,
                    onTriggerHapticHeavy = onTriggerHapticHeavy,
                    onTriggerAccelerationPulse = onTriggerAccelerationPulse,
                    onStopHaptic = onStopHaptic
                )
            }
        }

        // Minimalist Floating Settings Button (visible during normal gameplay)
        if (!isCustomEditMode) {
            FloatingSettingsButton(
                isConnected = isConnected,
                connectionMedium = connectionMedium,
                pingMs = pingMs,
                isGyroEnabled = isGyroEnabled,
                onClick = { showQuickSettings = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
        }

        // Floating Layout Editor Bar Overlay (floats on top, DOES NOT squeeze controller body!)
        if (isCustomEditMode) {
            LayoutEditorBar(
                isEditMode = isCustomEditMode,
                selectedElementKey = selectedElementKey,
                customLayout = customLayout,
                isLandscape = isLandscape,
                onScaleChange = onScaleElement,
                onToggleTouchpad = onToggleTouchpad,
                onResetDefaults = onResetLayout,
                onSaveAndExit = onSaveLayout,
                onCancel = onToggleCustomize,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
            )
        }

        // Quick Settings Overlay Dialog
        if (showQuickSettings && !isCustomEditMode) {
            QuickSettingsOverlay(
                isConnected = isConnected,
                connectionMedium = connectionMedium,
                pingMs = pingMs,
                layout = layout,
                isGyroEnabled = isGyroEnabled,
                isHapticEnabled = isHapticEnabled,
                onDismiss = { showQuickSettings = false },
                onToggleCustomize = {
                    showQuickSettings = false
                    onToggleCustomize()
                },
                onSwitchToReceiver = {
                    showQuickSettings = false
                    onSwitchToReceiver()
                },
                onOpenFullSettings = {
                    showQuickSettings = false
                    onOpenSettings()
                }
            )
        }
    }
}

@Composable
fun FloatingSettingsButton(
    isConnected: Boolean,
    connectionMedium: ConnectionMedium,
    pingMs: Int,
    isGyroEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xCC0E1422))
            .border(1.dp, Color(0xFF222D42), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isConnected) EmeraldGreen else CoralRed)
        )
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Controller Settings",
            tint = ElectricCyan,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = if (isConnected) {
                if (connectionMedium == ConnectionMedium.BLUETOOTH) "BT" else "${pingMs}ms"
            } else "Settings",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isConnected) TextPrimary else TextSecondary
        )
        if (isGyroEnabled) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(VividIndigo.copy(alpha = 0.5f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            ) {
                Text("G", fontSize = 8.sp, fontWeight = FontWeight.Black, color = ElectricCyan)
            }
        }
    }
}

@Composable
fun QuickSettingsOverlay(
    isConnected: Boolean,
    connectionMedium: ConnectionMedium,
    pingMs: Int,
    layout: ControllerLayout,
    isGyroEnabled: Boolean,
    isHapticEnabled: Boolean,
    onDismiss: () -> Unit,
    onToggleCustomize: () -> Unit,
    onSwitchToReceiver: () -> Unit,
    onOpenFullSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .clickable(enabled = false) {}
                .padding(16.dp)
                .width(420.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF2121826)),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "CONTROLLER SETTINGS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }

                // Connection status card (Tap to pair/connect)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { onOpenFullSettings() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) EmeraldGreen else CoralRed)
                        )
                        Column {
                            Text(
                                text = if (isConnected) {
                                    if (connectionMedium == ConnectionMedium.BLUETOOTH) "Bluetooth Linked" else "Wi-Fi Connected"
                                } else "Not Linked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) EmeraldGreen else CoralRed
                            )
                            Text(
                                text = if (isConnected) {
                                    if (connectionMedium == ConnectionMedium.BLUETOOTH) "Latency: Direct BT HID" else "Ping: ${pingMs}ms"
                                } else "Tap to connect or pair with PC / TV",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Icon(
                        imageVector = if (connectionMedium == ConnectionMedium.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isConnected) EmeraldGreen else TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Actions Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customize Controller Layout
                    Button(
                        onClick = onToggleCustomize,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VividIndigo),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Receiver Screen
                    Button(
                        onClick = onSwitchToReceiver,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Icon(Icons.Default.Tv, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Receiver", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                // Full Settings Button
                Button(
                    onClick = onOpenFullSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, DarkBorder)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pairing & Connection Settings", fontSize = 12.sp, color = TextSecondary)
                }

                // Status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Layout: ${layout.title}",
                        fontSize = 10.sp,
                        color = TextTertiary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (isGyroEnabled) {
                            Text("• Gyro Active", fontSize = 10.sp, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                        }
                        if (isHapticEnabled) {
                            Text("• Haptics On", fontSize = 10.sp, color = EmeraldGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControllerTopBar(
    isConnected: Boolean,
    connectionMedium: ConnectionMedium = ConnectionMedium.WIFI,
    pingMs: Int,
    layout: ControllerLayout,
    isGyroEnabled: Boolean,
    isCustomEditMode: Boolean = false,
    onToggleCustomize: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onSwitchToReceiver: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(width = 0.5.dp, color = DarkBorder)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Connection status pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurfaceVariant)
                .clickable { onOpenSettings() }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) EmeraldGreen else CoralRed)
            )
            Icon(
                imageVector = if (connectionMedium == ConnectionMedium.BLUETOOTH) Icons.Default.Bluetooth else Icons.Default.Wifi,
                contentDescription = null,
                tint = if (isConnected) EmeraldGreen else TextSecondary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = if (isConnected) {
                    if (connectionMedium == ConnectionMedium.BLUETOOTH) "BT Linked" else "Wi-Fi (${pingMs}ms)"
                } else "Not Linked (Tap to Connect)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isConnected) EmeraldGreen else CoralRed
            )
        }

        // Layout badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = layout.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
            if (isGyroEnabled) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(VividIndigo.copy(alpha = 0.3f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text("GYRO ON", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = ElectricCyan)
                }
            }
        }

        // Action icons: Customize, Receiver Mode, Settings
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Customize Controls / Edit Layout button
            Button(
                onClick = onToggleCustomize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCustomEditMode) VividIndigo else DarkSurfaceElevated
                ),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isCustomEditMode) ElectricCyan else DarkBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Customize Controls",
                    tint = if (isCustomEditMode) Color.White else ElectricCyan,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCustomEditMode) "Done" else "Customize",
                    fontSize = 11.sp,
                    color = if (isCustomEditMode) Color.White else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Switch to Tablet Receiver mode
            Button(
                onClick = onSwitchToReceiver,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(
                    Icons.Default.Tv,
                    contentDescription = "Receiver Mode",
                    tint = ElectricCyan,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Receiver", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            // Settings dialog
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings & Pairing",
                    tint = ElectricCyan
                )
            }
        }
    }
}

@Composable
fun ModernGamepadLayout(
    state: GamepadState,
    isLandscape: Boolean,
    customLayout: CustomLayoutConfig = CustomLayoutConfig(),
    isEditMode: Boolean = false,
    selectedElementKey: String? = null,
    onSelectElement: (String) -> Unit = {},
    onDragOffsetDelta: (String, Float, Float) -> Unit = { _, _, _ -> },
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit
) {
    val safeTriggerHaptic: () -> Unit = {
        if (!isEditMode) onTriggerHaptic()
    }
    val safeUpdateState: ((GamepadState) -> GamepadState) -> Unit = { transform ->
        if (!isEditMode) onUpdateState(transform)
    }

    if (isLandscape) {
        // Landscape Mode: Left stick & D-pad on left, Touchpad & menu in center, Buttons & Right stick on right
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: L1/L2, Stick, D-Pad
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_SHOULDER_LEFT,
                        title = "L1 / L2",
                        config = customLayout.shoulderLeft,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_SHOULDER_LEFT,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_SHOULDER_LEFT) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_SHOULDER_LEFT, dx, dy) }
                    ) {
                        ShoulderGroup(
                            bumperLabel = "L1",
                            triggerLabel = "L2",
                            accentColor = ElectricCyan,
                            enabled = !isEditMode,
                            onBumperChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnL1 = it) }
                            },
                            onTriggerChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f) }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_LEFT_STICK,
                        title = "LEFT STICK",
                        config = customLayout.leftStick,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_LEFT_STICK,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_LEFT_STICK) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_LEFT_STICK, dx, dy) }
                    ) {
                        AnalogStick(
                            label = "LS",
                            size = 130.dp,
                            enabled = !isEditMode,
                            onMove = { x, y ->
                                safeUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
                            }
                        )
                    }

                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_DPAD,
                        title = "D-PAD",
                        config = customLayout.dpad,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_DPAD,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_DPAD) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_DPAD, dx, dy) }
                    ) {
                        DPad(
                            dpadSize = 125.dp,
                            enabled = !isEditMode,
                            onDirectionChange = { up, down, left, right ->
                                if (up || down || left || right) safeTriggerHaptic()
                                safeUpdateState { s ->
                                    s.copy(
                                        dpadUp = up,
                                        dpadDown = down,
                                        dpadLeft = left,
                                        dpadRight = right
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Center: Touchpad + Select, Start, Home
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Touchpad
                if (customLayout.touchpad.visible || isEditMode) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_TOUCHPAD,
                        title = "TOUCHPAD",
                        config = customLayout.touchpad,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_TOUCHPAD,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_TOUCHPAD) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_TOUCHPAD, dx, dy) }
                    ) {
                        Touchpad(
                            width = 165.dp,
                            height = 96.dp,
                            enabled = !isEditMode,
                            onRelativeMove = { dx, dy ->
                                safeUpdateState { s ->
                                    s.copy(
                                        touchpadX = dx,
                                        touchpadY = dy,
                                        leftStickX = if (dx == 0f && dy == 0f) 0f else dx.coerceIn(-1f, 1f),
                                        leftStickY = if (dx == 0f && dy == 0f) 0f else dy.coerceIn(-1f, 1f),
                                        rightStickX = if (dx == 0f && dy == 0f) 0f else dx.coerceIn(-1f, 1f),
                                        rightStickY = if (dx == 0f && dy == 0f) 0f else dy.coerceIn(-1f, 1f)
                                    )
                                }
                            },
                            onClickChange = { pressed ->
                                if (pressed) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnTouchpad = pressed) }
                            },
                            onRightClickChange = { pressed ->
                                if (pressed) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnStart = pressed) }
                            },
                            onTriggerHaptic = if (!isEditMode) safeTriggerHaptic else null
                        )
                    }
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_CENTER_PILLS,
                    title = "MENU",
                    config = customLayout.centerPills,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_CENTER_PILLS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_CENTER_PILLS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_CENTER_PILLS, dx, dy) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CenterPillButton(label = "SELECT", isPressed = state.btnSelect, enabled = !isEditMode) {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnSelect = it) }
                            }
                            CenterPillButton(label = "START", isPressed = state.btnStart, enabled = !isEditMode) {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnStart = it) }
                            }
                        }
                        CenterPillButton(label = "HOME", isPressed = state.btnHome, color = VividIndigo, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnHome = it) }
                        }
                    }
                }
            }

            // Right Side: R1/R2, Action Buttons, Right Stick
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_SHOULDER_RIGHT,
                        title = "R1 / R2",
                        config = customLayout.shoulderRight,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_SHOULDER_RIGHT,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_SHOULDER_RIGHT) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_SHOULDER_RIGHT, dx, dy) }
                    ) {
                        ShoulderGroup(
                            bumperLabel = "R1",
                            triggerLabel = "R2",
                            accentColor = ElectricCyan,
                            enabled = !isEditMode,
                            onBumperChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnR1 = it) }
                            },
                            onTriggerChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f) }
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_RIGHT_STICK,
                        title = "RIGHT STICK",
                        config = customLayout.rightStick,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_RIGHT_STICK,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_RIGHT_STICK) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_RIGHT_STICK, dx, dy) }
                    ) {
                        AnalogStick(
                            label = "RS",
                            size = 130.dp,
                            enabled = !isEditMode,
                            onMove = { x, y ->
                                safeUpdateState { s -> s.copy(rightStickX = x, rightStickY = y) }
                            }
                        )
                    }

                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_ACTION_BUTTONS,
                        title = "ABXY",
                        config = customLayout.actionButtons,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_ACTION_BUTTONS,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_ACTION_BUTTONS) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_ACTION_BUTTONS, dx, dy) }
                    ) {
                        DiamondActionButtons(
                            state = state,
                            size = 136.dp,
                            buttonSize = 46.dp,
                            enabled = !isEditMode,
                            onButtonChange = { btn, pressed ->
                                if (pressed) safeTriggerHaptic()
                                when (btn) {
                                    "A" -> safeUpdateState { s -> s.copy(btnA = pressed) }
                                    "B" -> safeUpdateState { s -> s.copy(btnB = pressed) }
                                    "X" -> safeUpdateState { s -> s.copy(btnX = pressed) }
                                    "Y" -> safeUpdateState { s -> s.copy(btnY = pressed) }
                                }
                            }
                        )
                    }
                }
            }
        }
    } else {
        // Portrait Mode: Stacked layout
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Shoulder Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_SHOULDER_LEFT,
                    title = "L1 / L2",
                    config = customLayout.shoulderLeft,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_SHOULDER_LEFT,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_SHOULDER_LEFT) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_SHOULDER_LEFT, dx, dy) }
                ) {
                    ShoulderGroup(
                        bumperLabel = "L1",
                        triggerLabel = "L2",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onBumperChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnL1 = it) }
                        },
                        onTriggerChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f) }
                        }
                    )
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_SHOULDER_RIGHT,
                    title = "R1 / R2",
                    config = customLayout.shoulderRight,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_SHOULDER_RIGHT,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_SHOULDER_RIGHT) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_SHOULDER_RIGHT, dx, dy) }
                ) {
                    ShoulderGroup(
                        bumperLabel = "R1",
                        triggerLabel = "R2",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onBumperChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnR1 = it) }
                        },
                        onTriggerChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f) }
                        }
                    )
                }
            }

            // Middle: Sticks and DPad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_LEFT_STICK,
                    title = "LEFT STICK",
                    config = customLayout.leftStick,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_LEFT_STICK,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_LEFT_STICK) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_LEFT_STICK, dx, dy) }
                ) {
                    AnalogStick(
                        label = "LS",
                        size = 140.dp,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
                        }
                    )
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_ACTION_BUTTONS,
                    title = "ABXY",
                    config = customLayout.actionButtons,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_ACTION_BUTTONS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_ACTION_BUTTONS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_ACTION_BUTTONS, dx, dy) }
                ) {
                    DiamondActionButtons(
                        state = state,
                        enabled = !isEditMode,
                        onButtonChange = { btn, pressed ->
                            if (pressed) safeTriggerHaptic()
                            when (btn) {
                                "A" -> safeUpdateState { s -> s.copy(btnA = pressed) }
                                "B" -> safeUpdateState { s -> s.copy(btnB = pressed) }
                                "X" -> safeUpdateState { s -> s.copy(btnX = pressed) }
                                "Y" -> safeUpdateState { s -> s.copy(btnY = pressed) }
                            }
                        }
                    )
                }
            }

            // Center pills & Touchpad
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (customLayout.touchpad.visible || isEditMode) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_TOUCHPAD,
                        title = "TOUCHPAD",
                        config = customLayout.touchpad,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_TOUCHPAD,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_TOUCHPAD) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_TOUCHPAD, dx, dy) }
                    ) {
                        Touchpad(
                            width = 195.dp,
                            height = 88.dp,
                            enabled = !isEditMode,
                            onRelativeMove = { dx, dy ->
                                safeUpdateState { s ->
                                    s.copy(
                                        touchpadX = dx,
                                        touchpadY = dy,
                                        leftStickX = if (dx == 0f && dy == 0f) 0f else dx.coerceIn(-1f, 1f),
                                        leftStickY = if (dx == 0f && dy == 0f) 0f else dy.coerceIn(-1f, 1f),
                                        rightStickX = if (dx == 0f && dy == 0f) 0f else dx.coerceIn(-1f, 1f),
                                        rightStickY = if (dx == 0f && dy == 0f) 0f else dy.coerceIn(-1f, 1f)
                                    )
                                }
                            },
                            onClickChange = { pressed ->
                                if (pressed) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnTouchpad = pressed) }
                            },
                            onRightClickChange = { pressed ->
                                if (pressed) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnStart = pressed) }
                            },
                            onTriggerHaptic = if (!isEditMode) safeTriggerHaptic else null
                        )
                    }
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_CENTER_PILLS,
                    title = "MENU",
                    config = customLayout.centerPills,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_CENTER_PILLS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_CENTER_PILLS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_CENTER_PILLS, dx, dy) }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CenterPillButton("SELECT", state.btnSelect, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnSelect = it) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        CenterPillButton("START", state.btnStart, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnStart = it) }
                        }
                    }
                }
            }

            // Bottom row: D-Pad & Right Stick
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_DPAD,
                    title = "D-PAD",
                    config = customLayout.dpad,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_DPAD,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_DPAD) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_DPAD, dx, dy) }
                ) {
                    DPad(
                        dpadSize = 140.dp,
                        enabled = !isEditMode,
                        onDirectionChange = { up, down, left, right ->
                            if (up || down || left || right) safeTriggerHaptic()
                            safeUpdateState { s ->
                                s.copy(
                                    dpadUp = up,
                                    dpadDown = down,
                                    dpadLeft = left,
                                    dpadRight = right
                                )
                            }
                        }
                    )
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_RIGHT_STICK,
                    title = "RIGHT STICK",
                    config = customLayout.rightStick,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_RIGHT_STICK,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_RIGHT_STICK) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_RIGHT_STICK, dx, dy) }
                ) {
                    AnalogStick(
                        label = "RS",
                        size = 140.dp,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s -> s.copy(rightStickX = x, rightStickY = y) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiamondActionButtons(
    state: GamepadState,
    size: androidx.compose.ui.unit.Dp = 136.dp,
    buttonSize: androidx.compose.ui.unit.Dp = 46.dp,
    enabled: Boolean = true,
    onButtonChange: (String, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .requiredSize(size)
            .aspectRatio(1f)
            .testTag("action_buttons_diamond"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background disc for tactile depth
        Box(
            modifier = Modifier
                .size(size * 0.96f)
                .clip(CircleShape)
                .background(Color(0x14182338))
        )

        // Y button (Top)
        GameButton(
            label = "Y",
            size = buttonSize,
            accentColor = ButtonYColor,
            modifier = Modifier.align(Alignment.TopCenter),
            enabled = enabled,
            onPressChange = { onButtonChange("Y", it) }
        )

        // A button (Bottom)
        GameButton(
            label = "A",
            size = buttonSize,
            accentColor = ButtonAColor,
            modifier = Modifier.align(Alignment.BottomCenter),
            enabled = enabled,
            onPressChange = { onButtonChange("A", it) }
        )

        // X button (Left)
        GameButton(
            label = "X",
            size = buttonSize,
            accentColor = ButtonXColor,
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = enabled,
            onPressChange = { onButtonChange("X", it) }
        )

        // B button (Right)
        GameButton(
            label = "B",
            size = buttonSize,
            accentColor = ButtonBColor,
            modifier = Modifier.align(Alignment.CenterEnd),
            enabled = enabled,
            onPressChange = { onButtonChange("B", it) }
        )
    }
}

@Composable
fun CenterPillButton(
    label: String,
    isPressed: Boolean,
    color: Color = Color(0xFF64748B),
    enabled: Boolean = true,
    onPressChange: (Boolean) -> Unit
) {
    ShoulderBumper(
        label = label,
        width = 62.dp,
        height = 28.dp,
        accentColor = color,
        enabled = enabled,
        onPressChange = onPressChange
    )
}

@Composable
fun AndroidTouchControllerLayout(
    state: GamepadState,
    isLandscape: Boolean,
    customLayout: CustomLayoutConfig = CustomLayoutConfig(),
    isEditMode: Boolean = false,
    selectedElementKey: String? = null,
    onSelectElement: (String) -> Unit = {},
    onDragOffsetDelta: (String, Float, Float) -> Unit = { _, _, _ -> },
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit
) {
    val safeTriggerHaptic: () -> Unit = {
        if (!isEditMode) onTriggerHaptic()
    }
    val safeUpdateState: ((GamepadState) -> GamepadState) -> Unit = { transform ->
        if (!isEditMode) onUpdateState(transform)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Base Layer: Left and Right Full-Surface Touchpads (Half screen each)
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left Half: Full-Screen Touchpad for Movement (no rigid joystick knob)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    AndroidTouchZone(
                        label = "TOUCH TO MOVE",
                        subLabel = "Left Half Touchpad (Full Surface)",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
                        },
                        onTriggerHaptic = safeTriggerHaptic
                    )
                }

                // Right Half: Full-Screen Touchpad for Look / Aim
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) {
                    AndroidTouchZone(
                        label = "TOUCH TO AIM / LOOK",
                        subLabel = "Right Half Touchpad",
                        accentColor = VividIndigo,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s ->
                                s.copy(
                                    rightStickX = x,
                                    rightStickY = y,
                                    touchpadX = x,
                                    touchpadY = y
                                )
                            }
                        },
                        onTriggerHaptic = safeTriggerHaptic
                    )
                }
            }
        } else {
            // Portrait Mode: Split Upper and Lower
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidTouchZone(
                        label = "TOUCH TO MOVE",
                        subLabel = "Upper Touchpad",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
                        },
                        onTriggerHaptic = safeTriggerHaptic
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    AndroidTouchZone(
                        label = "TOUCH TO AIM / LOOK",
                        subLabel = "Lower Touchpad",
                        accentColor = VividIndigo,
                        enabled = !isEditMode,
                        onMove = { x, y ->
                            safeUpdateState { s ->
                                s.copy(
                                    rightStickX = x,
                                    rightStickY = y,
                                    touchpadX = x,
                                    touchpadY = y
                                )
                            }
                        },
                        onTriggerHaptic = safeTriggerHaptic
                    )
                }
            }
        }

        // Overlay Layer: Floating Individual Buttons
        // Every button is individually selectable, resizable, and draggable!
        Box(modifier = Modifier.fillMaxSize()) {
            // --- TOP LEFT: Individual L1 and L2 Buttons ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = if (isLandscape) 6.dp else 10.dp, start = if (isLandscape) 12.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_BTN_L1,
                    title = "L1",
                    config = customLayout.btnL1,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_L1,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_L1) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_L1, dx, dy) }
                ) {
                    IndividualShoulderButton(
                        label = "L1",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnL1 = it) }
                        }
                    )
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_BTN_L2,
                    title = "L2",
                    config = customLayout.btnL2,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_L2,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_L2) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_L2, dx, dy) }
                ) {
                    IndividualShoulderButton(
                        label = "L2",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f) }
                        }
                    )
                }
            }

            // --- TOP RIGHT: Individual R1 and R2 Buttons ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = if (isLandscape) 6.dp else 10.dp, end = if (isLandscape) 12.dp else 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_BTN_R1,
                    title = "R1",
                    config = customLayout.btnR1,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_R1,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_R1) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_R1, dx, dy) }
                ) {
                    IndividualShoulderButton(
                        label = "R1",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnR1 = it) }
                        }
                    )
                }

                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_BTN_R2,
                    title = "R2",
                    config = customLayout.btnR2,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_R2,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_R2) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_R2, dx, dy) }
                ) {
                    IndividualShoulderButton(
                        label = "R2",
                        accentColor = ElectricCyan,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f) }
                        }
                    )
                }
            }

            // --- CENTER MENU: Select, Start, Home ---
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 44.dp)
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_CENTER_PILLS,
                    title = "MENU",
                    config = customLayout.centerPills,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_CENTER_PILLS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_CENTER_PILLS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_CENTER_PILLS, dx, dy) }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CenterPillButton(label = "SEL", isPressed = state.btnSelect, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnSelect = it) }
                        }
                        CenterPillButton(label = "HOME", isPressed = state.btnHome, color = VividIndigo, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnHome = it) }
                        }
                        CenterPillButton(label = "START", isPressed = state.btnStart, enabled = !isEditMode) {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnStart = it) }
                        }
                    }
                }
            }

            // --- RIGHT AREA: Individual Action Buttons (A, B, X, Y) ---
            val actionClusterAlign = if (isLandscape) Alignment.BottomEnd else Alignment.BottomCenter
            Box(
                modifier = Modifier
                    .align(actionClusterAlign)
                    .padding(
                        end = if (isLandscape) 36.dp else 12.dp,
                        bottom = if (isLandscape) 24.dp else 36.dp
                    )
                    .size(175.dp),
                contentAlignment = Alignment.Center
            ) {
                // Button Y (Top)
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_BTN_Y,
                        title = "Y",
                        config = customLayout.btnY,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_Y,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_Y) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_Y, dx, dy) }
                    ) {
                        IndividualActionButton(
                            label = "Y",
                            accentColor = ButtonYColor,
                            enabled = !isEditMode,
                            onPressChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnY = it) }
                            }
                        )
                    }
                }

                // Button X (Left)
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_BTN_X,
                        title = "X",
                        config = customLayout.btnX,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_X,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_X) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_X, dx, dy) }
                    ) {
                        IndividualActionButton(
                            label = "X",
                            accentColor = ButtonXColor,
                            enabled = !isEditMode,
                            onPressChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnX = it) }
                            }
                        )
                    }
                }

                // Button B (Right)
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_BTN_B,
                        title = "B",
                        config = customLayout.btnB,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_B,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_B) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_B, dx, dy) }
                    ) {
                        IndividualActionButton(
                            label = "B",
                            accentColor = ButtonBColor,
                            enabled = !isEditMode,
                            onPressChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnB = it) }
                            }
                        )
                    }
                }

                // Button A (Bottom)
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    CustomizableElement(
                        elementKey = CustomLayoutConfig.KEY_BTN_A,
                        title = "A",
                        config = customLayout.btnA,
                        isEditMode = isEditMode,
                        isSelected = selectedElementKey == CustomLayoutConfig.KEY_BTN_A,
                        onSelect = { onSelectElement(CustomLayoutConfig.KEY_BTN_A) },
                        onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_BTN_A, dx, dy) }
                    ) {
                        IndividualActionButton(
                            label = "A",
                            accentColor = ButtonAColor,
                            enabled = !isEditMode,
                            onPressChange = {
                                if (it) safeTriggerHaptic()
                                safeUpdateState { s -> s.copy(btnA = it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RetroArcadeLayout(
    state: GamepadState,
    isLandscape: Boolean,
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Big DPad
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "DIRECTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            DPad(
                dpadSize = 170.dp,
                onDirectionChange = { up, down, left, right ->
                    if (up || down || left || right) onTriggerHaptic()
                    onUpdateState { s ->
                        s.copy(
                            dpadUp = up,
                            dpadDown = down,
                            dpadLeft = left,
                            dpadRight = right
                        )
                    }
                }
            )
        }

        // Center: Select, Turbo, Start
        Column(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CenterPillButton("TURBO", state.btnTurbo, color = CoralRed) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnTurbo = it) }
            }
            CenterPillButton("SELECT", state.btnSelect) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnSelect = it) }
            }
            CenterPillButton("START", state.btnStart) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnStart = it) }
            }
        }

        // Right: Arcade Buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "ARCADE ACTION",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GameButton(label = "X", size = 68.dp, accentColor = ButtonXColor) {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnX = it) }
                    }
                    GameButton(label = "A", size = 68.dp, accentColor = ButtonAColor) {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnA = it) }
                    }
                }
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GameButton(label = "Y", size = 68.dp, accentColor = ButtonYColor) {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnY = it) }
                    }
                    GameButton(label = "B", size = 68.dp, accentColor = ButtonBColor) {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnB = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun RacingControllerLayout(
    state: GamepadState,
    isLandscape: Boolean,
    customLayout: CustomLayoutConfig = CustomLayoutConfig(),
    isEditMode: Boolean = false,
    selectedElementKey: String? = null,
    isJoystickMode: Boolean = false,
    onToggleJoystickMode: (Boolean) -> Unit = {},
    onSelectElement: (String) -> Unit = {},
    onDragOffsetDelta: (String, Float, Float) -> Unit = { _, _, _ -> },
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit,
    onTriggerHapticTick: () -> Unit = onTriggerHaptic,
    onTriggerHapticHeavy: () -> Unit = onTriggerHaptic,
    onTriggerAccelerationPulse: (Float) -> Unit = {},
    onStopHaptic: () -> Unit = {}
) {

    val safeTriggerHaptic: () -> Unit = {
        if (!isEditMode) onTriggerHaptic()
    }
    val safeTriggerHapticTick: () -> Unit = {
        if (!isEditMode) onTriggerHapticTick()
    }
    val safeTriggerHapticHeavy: () -> Unit = {
        if (!isEditMode) onTriggerHapticHeavy()
    }
    val safeTriggerAccelerationPulse: (Float) -> Unit = { progress ->
        if (!isEditMode) onTriggerAccelerationPulse(progress)
    }
    val safeStopHaptic: () -> Unit = {
        if (!isEditMode) onStopHaptic()
    }
    val safeUpdateState: ((GamepadState) -> GamepadState) -> Unit = { transform ->
        if (!isEditMode) onUpdateState(transform)
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Steering Wheel or Navigation Joystick
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_STEERING_WHEEL,
                    title = if (isJoystickMode) "JOYSTICK" else "STEERING",
                    config = customLayout.steeringWheel,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_STEERING_WHEEL,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_STEERING_WHEEL) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_STEERING_WHEEL, dx, dy) }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Quick Mode Toggle Button (Steering Wheel <-> Nav Joystick)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x55111724))
                                .border(
                                    width = 1.dp,
                                    color = if (isJoystickMode) VividIndigo else ElectricCyan.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !isEditMode) {
                                    onToggleJoystickMode(!isJoystickMode)
                                    safeTriggerHaptic()
                                    safeUpdateState { s ->
                                        s.copy(
                                            leftStickX = 0f,
                                            leftStickY = 0f,
                                            dpadUp = false,
                                            dpadDown = false,
                                            dpadLeft = false,
                                            dpadRight = false
                                        )
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isJoystickMode) Icons.Default.SportsEsports else Icons.Default.Tune,
                                contentDescription = "Toggle Control",
                                tint = if (isJoystickMode) VividIndigo else ElectricCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isJoystickMode) "NAV JOYSTICK" else "STEERING WHEEL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isJoystickMode) TextPrimary else ElectricCyan,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isJoystickMode) {
                            AnalogStick(
                                label = "NAV",
                                size = 190.dp,
                                enabled = !isEditMode,
                                onMove = { x, y ->
                                    safeUpdateState { s ->
                                        s.copy(
                                            leftStickX = x,
                                            leftStickY = y,
                                            dpadUp = y < -0.35f,
                                            dpadDown = y > 0.35f,
                                            dpadLeft = x < -0.35f,
                                            dpadRight = x > 0.35f
                                        )
                                    }
                                }
                            )
                        } else {
                            SteeringWheel(
                                size = 195.dp,
                                onSteer = { steer ->
                                    safeUpdateState { s ->
                                        s.copy(
                                            leftStickX = steer,
                                            dpadLeft = steer < -0.3f,
                                            dpadRight = steer > 0.3f
                                        )
                                    }
                                },
                                onTriggerHaptic = safeTriggerHaptic,
                                onTriggerHapticTick = safeTriggerHapticTick,
                                onTriggerHapticHeavy = safeTriggerHapticHeavy
                            )
                        }
                    }
                }
            }

            // CENTER: Extra Functions (NOS, Drift, Reset, Pause)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_RACING_SYSTEMS,
                    title = "SYSTEMS",
                    config = customLayout.racingSystems,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_RACING_SYSTEMS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_RACING_SYSTEMS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_RACING_SYSTEMS, dx, dy) }
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SYSTEMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RacingExtraButton(
                                label = "NOS",
                                subLabel = "NITRO",
                                accentColor = VividIndigo,
                                size = 50.dp,
                                enabled = !isEditMode,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s -> s.copy(btnX = it, btnTurbo = it) }
                                }
                            )
                            RacingExtraButton(
                                label = "DRIFT",
                                subLabel = "E-BRAKE",
                                accentColor = CoralRed,
                                size = 50.dp,
                                enabled = !isEditMode,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s -> s.copy(btnL1 = it) }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RacingExtraButton(
                                label = "RESET",
                                subLabel = "CAR",
                                accentColor = ButtonYColor,
                                size = 50.dp,
                                enabled = !isEditMode,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s -> s.copy(btnY = it) }
                                }
                            )
                            RacingExtraButton(
                                label = "PAUSE",
                                subLabel = "MENU",
                                accentColor = ElectricCyan,
                                size = 50.dp,
                                enabled = !isEditMode,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s -> s.copy(btnStart = it) }
                                }
                            )
                        }
                    }
                }
            }

            // RIGHT: Pedals (Brake/Reverse and Accelerate/Gas)
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CustomizableElement(
                    elementKey = CustomLayoutConfig.KEY_PEDALS,
                    title = "PEDALS",
                    config = customLayout.pedals,
                    isEditMode = isEditMode,
                    isSelected = selectedElementKey == CustomLayoutConfig.KEY_PEDALS,
                    onSelect = { onSelectElement(CustomLayoutConfig.KEY_PEDALS) },
                    onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_PEDALS, dx, dy) }
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PEDALS",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Brake / Reverse Pedal
                            PedalButton(
                                label = "BRAKE",
                                subLabel = "REVERSE",
                                color = CoralRed,
                                width = 96.dp,
                                height = 175.dp,
                                enabled = !isEditMode,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s ->
                                        s.copy(
                                            btnB = it,
                                            btnL2 = it,
                                            leftTrigger = if (it) 1f else 0f,
                                            dpadDown = it
                                        )
                                    }
                                }
                            )

                            // Accelerate / Gas Pedal
                            PedalButton(
                                label = "ACCEL",
                                subLabel = "GAS",
                                color = EmeraldGreen,
                                width = 96.dp,
                                height = 175.dp,
                                enabled = !isEditMode,
                                isAccelerator = true,
                                onPressChange = {
                                    if (it) safeTriggerHaptic()
                                    safeUpdateState { s ->
                                        s.copy(
                                            btnA = it,
                                            btnR2 = it,
                                            rightTrigger = if (it) 1f else 0f,
                                            dpadUp = it
                                        )
                                    }
                                },
                                onAccelerationRumble = safeTriggerAccelerationPulse,
                                onStopHaptic = safeStopHaptic
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top: Steering Wheel or Navigation Joystick
            CustomizableElement(
                elementKey = CustomLayoutConfig.KEY_STEERING_WHEEL,
                title = if (isJoystickMode) "JOYSTICK" else "STEERING",
                config = customLayout.steeringWheel,
                isEditMode = isEditMode,
                isSelected = selectedElementKey == CustomLayoutConfig.KEY_STEERING_WHEEL,
                onSelect = { onSelectElement(CustomLayoutConfig.KEY_STEERING_WHEEL) },
                onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_STEERING_WHEEL, dx, dy) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Mode Toggle Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x55111724))
                            .border(
                                width = 1.dp,
                                color = if (isJoystickMode) VividIndigo else ElectricCyan.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !isEditMode) {
                                onToggleJoystickMode(!isJoystickMode)
                                safeTriggerHaptic()
                                safeUpdateState { s ->
                                    s.copy(
                                        leftStickX = 0f,
                                        leftStickY = 0f,
                                        dpadUp = false,
                                        dpadDown = false,
                                        dpadLeft = false,
                                        dpadRight = false
                                    )
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isJoystickMode) Icons.Default.SportsEsports else Icons.Default.Tune,
                            contentDescription = "Toggle Control",
                            tint = if (isJoystickMode) VividIndigo else ElectricCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isJoystickMode) "NAV JOYSTICK" else "STEERING WHEEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isJoystickMode) TextPrimary else ElectricCyan,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isJoystickMode) {
                        AnalogStick(
                            label = "NAV",
                            size = 180.dp,
                            enabled = !isEditMode,
                            onMove = { x, y ->
                                safeUpdateState { s ->
                                    s.copy(
                                        leftStickX = x,
                                        leftStickY = y,
                                        dpadUp = y < -0.35f,
                                        dpadDown = y > 0.35f,
                                        dpadLeft = x < -0.35f,
                                        dpadRight = x > 0.35f
                                    )
                                }
                            }
                        )
                    } else {
                        SteeringWheel(
                            size = 185.dp,
                            onSteer = { steer ->
                                safeUpdateState { s ->
                                    s.copy(
                                        leftStickX = steer,
                                        dpadLeft = steer < -0.3f,
                                        dpadRight = steer > 0.3f
                                    )
                                }
                            },
                            onTriggerHaptic = safeTriggerHaptic,
                            onTriggerHapticTick = safeTriggerHapticTick,
                            onTriggerHapticHeavy = safeTriggerHapticHeavy
                        )
                    }
                }
            }

            // Middle: Extra Systems
            CustomizableElement(
                elementKey = CustomLayoutConfig.KEY_RACING_SYSTEMS,
                title = "SYSTEMS",
                config = customLayout.racingSystems,
                isEditMode = isEditMode,
                isSelected = selectedElementKey == CustomLayoutConfig.KEY_RACING_SYSTEMS,
                onSelect = { onSelectElement(CustomLayoutConfig.KEY_RACING_SYSTEMS) },
                onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_RACING_SYSTEMS, dx, dy) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RacingExtraButton(
                        label = "NOS",
                        accentColor = VividIndigo,
                        size = 48.dp,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnX = it, btnTurbo = it) }
                        }
                    )
                    RacingExtraButton(
                        label = "DRIFT",
                        accentColor = CoralRed,
                        size = 48.dp,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnL1 = it) }
                        }
                    )
                    RacingExtraButton(
                        label = "RESET",
                        accentColor = ButtonYColor,
                        size = 48.dp,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnY = it) }
                        }
                    )
                    RacingExtraButton(
                        label = "PAUSE",
                        accentColor = ElectricCyan,
                        size = 48.dp,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s -> s.copy(btnStart = it) }
                        }
                    )
                }
            }

            // Bottom: Pedals (Brake & Accel)
            CustomizableElement(
                elementKey = CustomLayoutConfig.KEY_PEDALS,
                title = "PEDALS",
                config = customLayout.pedals,
                isEditMode = isEditMode,
                isSelected = selectedElementKey == CustomLayoutConfig.KEY_PEDALS,
                onSelect = { onSelectElement(CustomLayoutConfig.KEY_PEDALS) },
                onDragOffsetDelta = { dx, dy -> onDragOffsetDelta(CustomLayoutConfig.KEY_PEDALS, dx, dy) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PedalButton(
                        label = "BRAKE",
                        subLabel = "REVERSE",
                        color = CoralRed,
                        width = 130.dp,
                        height = 140.dp,
                        enabled = !isEditMode,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s ->
                                s.copy(
                                    btnB = it,
                                    btnL2 = it,
                                    leftTrigger = if (it) 1f else 0f,
                                    dpadDown = it
                                )
                            }
                        }
                    )

                    PedalButton(
                        label = "ACCEL",
                        subLabel = "GAS",
                        color = EmeraldGreen,
                        width = 130.dp,
                        height = 140.dp,
                        enabled = !isEditMode,
                        isAccelerator = true,
                        onPressChange = {
                            if (it) safeTriggerHaptic()
                            safeUpdateState { s ->
                                s.copy(
                                    btnA = it,
                                    btnR2 = it,
                                    rightTrigger = if (it) 1f else 0f,
                                    dpadUp = it
                                )
                            }
                        },
                        onAccelerationRumble = safeTriggerAccelerationPulse,
                        onStopHaptic = safeStopHaptic
                    )
                }
            }
        }
    }
}

@Composable
fun SteeringWheel(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 205.dp,
    onSteer: (Float) -> Unit,
    onTriggerHaptic: () -> Unit = {},
    onTriggerHapticTick: () -> Unit = onTriggerHaptic,
    onTriggerHapticHeavy: () -> Unit = onTriggerHaptic
) {
    val coroutineScope = rememberCoroutineScope()
    val animatedAngle = remember { Animatable(0f) }
    var isSteering by remember { mutableStateOf(false) }

    val maxAngle = 105f // Rotation lock in degrees

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isSteering = true
                    onTriggerHapticTick()

                    val centerPx = Offset(this.size.width / 2f, this.size.height / 2f)
                    val dx = down.position.x - centerPx.x
                    val dy = down.position.y - centerPx.y
                    val distSq = dx * dx + dy * dy
                    var lastTouchAngle = if (distSq > 100f) {
                        Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    } else {
                        null
                    }
                    var currentWheelAngle = animatedAngle.value
                    var lastHapticAngle = currentWheelAngle
                    var hasHitMaxLock = currentWheelAngle >= maxAngle
                    var hasHitMinLock = currentWheelAngle <= -maxAngle

                    // Stop any ongoing spring animation immediately
                    coroutineScope.launch {
                        animatedAngle.snapTo(currentWheelAngle)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val currentDx = change.position.x - centerPx.x
                        val currentDy = change.position.y - centerPx.y
                        val moveDistSq = currentDx * currentDx + currentDy * currentDy

                        // Avoid erratic angles when touching near the center deadzone
                        if (moveDistSq > 100f) {
                            val currentTouchAngle = Math.toDegrees(atan2(currentDy.toDouble(), currentDx.toDouble())).toFloat()
                            val prevAngle = lastTouchAngle
                            if (prevAngle != null) {
                                var deltaAngle = currentTouchAngle - prevAngle
                                if (deltaAngle > 180f) deltaAngle -= 360f
                                else if (deltaAngle < -180f) deltaAngle += 360f

                                currentWheelAngle = (currentWheelAngle + deltaAngle).coerceIn(-maxAngle, maxAngle)

                                // Mechanical detent tick haptic every 8 degrees of rotation
                                val angleDiff = kotlin.math.abs(currentWheelAngle - lastHapticAngle)
                                if (angleDiff >= 8f) {
                                    onTriggerHapticTick()
                                    lastHapticAngle = currentWheelAngle
                                }

                                // Physical lock sensation when hitting maximum steering bounds
                                if (currentWheelAngle >= maxAngle) {
                                    if (!hasHitMaxLock) {
                                        onTriggerHapticHeavy()
                                        hasHitMaxLock = true
                                    }
                                } else if (currentWheelAngle < maxAngle - 2f) {
                                    hasHitMaxLock = false
                                }

                                if (currentWheelAngle <= -maxAngle) {
                                    if (!hasHitMinLock) {
                                        onTriggerHapticHeavy()
                                        hasHitMinLock = true
                                    }
                                } else if (currentWheelAngle > -maxAngle + 2f) {
                                    hasHitMinLock = false
                                }

                                coroutineScope.launch {
                                    animatedAngle.snapTo(currentWheelAngle)
                                }
                                val steerRatio = (currentWheelAngle / maxAngle).coerceIn(-1f, 1f)
                                onSteer(steerRatio)
                            }
                            lastTouchAngle = currentTouchAngle
                        }
                        change.consume()
                    }

                    // Release: spring back smoothly to center
                    isSteering = false
                    coroutineScope.launch {
                        animatedAngle.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f)
                        )
                    }
                    onSteer(0f)
                    onTriggerHapticTick()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f - 14.dp.toPx()
            val angle = animatedAngle.value

            rotate(degrees = angle, pivot = center) {
                // Outer Rim subtle glow
                drawCircle(
                    color = if (isSteering) ElectricCyan.copy(alpha = 0.25f) else Color(0x33000000),
                    radius = radius + 6.dp.toPx(),
                    center = center
                )

                // Thick Outer Steering Rim
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF222C3D), Color(0xFF101724), Color(0xFF090D15)),
                        center = center,
                        radius = radius + 10.dp.toPx()
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 24.dp.toPx())
                )

                // Outer Rim Accent Borders
                drawCircle(
                    color = if (isSteering) ElectricCyan.copy(alpha = 0.8f) else Color(0xFF2E3D56),
                    radius = radius + 11.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF161E2D),
                    radius = radius - 11.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 12 O'Clock Rally Center Marker (Cyan stripe)
                drawArc(
                    color = ElectricCyan,
                    startAngle = 264f,
                    sweepAngle = 12f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 24.dp.toPx())
                )

                // 3 Spokes: Left (180°), Right (0°), Bottom (90°)
                val hubRadius = 30.dp.toPx()
                val spokeColor = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C394F), Color(0xFF141C2B)),
                    center = center,
                    radius = radius
                )

                // Left spoke
                drawLine(
                    brush = spokeColor,
                    start = Offset(center.x - hubRadius, center.y),
                    end = Offset(center.x - radius + 10.dp.toPx(), center.y),
                    strokeWidth = 18.dp.toPx()
                )
                // Right spoke
                drawLine(
                    brush = spokeColor,
                    start = Offset(center.x + hubRadius, center.y),
                    end = Offset(center.x + radius - 10.dp.toPx(), center.y),
                    strokeWidth = 18.dp.toPx()
                )
                // Bottom spoke
                drawLine(
                    brush = spokeColor,
                    start = Offset(center.x, center.y + hubRadius),
                    end = Offset(center.x, center.y + radius - 10.dp.toPx()),
                    strokeWidth = 20.dp.toPx()
                )

                // Center Horn Hub
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF253347), Color(0xFF101724)),
                        center = center,
                        radius = hubRadius
                    ),
                    radius = hubRadius,
                    center = center
                )
                drawCircle(
                    color = if (isSteering) ElectricCyan else Color(0xFF384B66),
                    radius = hubRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Center Horn Hub Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "APEX",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSteering) ElectricCyan else TextSecondary,
                letterSpacing = 1.sp
            )
            val currentDegrees = animatedAngle.value.toInt()
            Text(
                text = when {
                    currentDegrees > 2 -> "${currentDegrees}° R"
                    currentDegrees < -2 -> "${-currentDegrees}° L"
                    else -> "0°"
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSteering) ElectricCyan else TextTertiary
            )
        }
    }
}

@Composable
fun RacingExtraButton(
    label: String,
    subLabel: String? = null,
    accentColor: Color,
    size: androidx.compose.ui.unit.Dp = 50.dp,
    enabled: Boolean = true,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed && enabled) accentColor.copy(alpha = 0.45f) else DarkSurfaceElevated,
                        if (isPressed && enabled) accentColor.copy(alpha = 0.2f) else DarkSurfaceVariant
                    )
                )
            )
            .border(
                width = if (isPressed && enabled) 2.dp else 1.dp,
                color = if (isPressed && enabled) accentColor else DarkBorder,
                shape = CircleShape
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            onPressChange(true)
                            try {
                                waitForUpOrCancellation()
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isPressed && enabled) Color.White else accentColor
            )
            if (subLabel != null) {
                Text(
                    text = subLabel,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun PedalButton(
    label: String,
    subLabel: String,
    color: Color,
    width: androidx.compose.ui.unit.Dp = 96.dp,
    height: androidx.compose.ui.unit.Dp = 175.dp,
    enabled: Boolean = true,
    isAccelerator: Boolean = false,
    onPressChange: (Boolean) -> Unit,
    onAccelerationRumble: ((Float) -> Unit)? = null,
    onStopHaptic: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    if (isAccelerator && isPressed && enabled) {
        LaunchedEffect(Unit) {
            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                // Progress smoothly increments from 0f to 1f over 2.5 seconds
                val progress = (elapsed / 2500f).coerceIn(0f, 1f)
                onAccelerationRumble?.invoke(progress)

                // Interval between pulses starts at 120ms (gentle idle rumble) and speeds up to 36ms (high rev speed)
                val delayMs = (120 - progress * 84).toLong().coerceIn(36L, 120L)
                delay(delayMs)
            }
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed && enabled) color.copy(alpha = 0.5f) else Color(0xFF1E2838),
                        if (isPressed && enabled) color.copy(alpha = 0.25f) else Color(0xFF0F1522)
                    )
                )
            )
            .border(
                width = if (isPressed && enabled) 2.5.dp else 1.5.dp,
                color = if (isPressed && enabled) color else Color(0xFF28364C),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            isPressed = true
                            onPressChange(true)

                            try {
                                waitForUpOrCancellation()
                            } finally {
                                isPressed = false
                                onPressChange(false)
                                onStopHaptic?.invoke()
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Pedal top indicator bar
            Box(
                modifier = Modifier
                    .width(width * 0.5f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isPressed) color else color.copy(alpha = 0.35f))
            )

            // Tread grip ribs
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(width * 0.65f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(if (isPressed) color.copy(alpha = 0.8f) else Color(0xFF253347))
                    )
                }
            }

            // Text Label
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (isPressed) Color.White else color
                )
                Text(
                    text = subLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }
        }
    }
}
