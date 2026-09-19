package com.example.ui.controller

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Control Bar
            ControllerTopBar(
                isConnected = isConnected,
                connectionMedium = connectionMedium,
                pingMs = pingMs,
                layout = layout,
                isGyroEnabled = isGyroEnabled,
                isCustomEditMode = isCustomEditMode,
                onToggleCustomize = onToggleCustomize,
                onOpenSettings = onOpenSettings,
                onSwitchToReceiver = onSwitchToReceiver
            )

            // Layout Editor Bar (active during Customize mode)
            LayoutEditorBar(
                isEditMode = isCustomEditMode,
                selectedElementKey = selectedElementKey,
                customLayout = customLayout,
                onScaleChange = onScaleElement,
                onToggleTouchpad = onToggleTouchpad,
                onResetDefaults = onResetLayout,
                onSaveAndExit = onSaveLayout,
                onCancel = onToggleCustomize
            )

            // Main Pad Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = if (isLandscape) 12.dp else 8.dp, vertical = 6.dp)
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
                    ControllerLayout.RETRO_ARCADE -> RetroArcadeLayout(
                        state = gamepadState,
                        isLandscape = isLandscape,
                        onUpdateState = onUpdateState,
                        onTriggerHaptic = onTriggerHaptic
                    )
                    ControllerLayout.RACING -> RacingControllerLayout(
                        state = gamepadState,
                        isLandscape = isLandscape,
                        onUpdateState = onUpdateState,
                        onTriggerHaptic = onTriggerHaptic
                    )
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
                            onBumperChange = {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnL1 = it) }
                            },
                            onTriggerChange = {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f) }
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
                            size = 140.dp,
                            onMove = { x, y ->
                                onUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
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
                            dpadSize = 130.dp,
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
                            width = 185.dp,
                            height = 100.dp,
                            onRelativeMove = { dx, dy ->
                                onUpdateState { s ->
                                    s.copy(
                                        touchpadX = dx,
                                        touchpadY = dy,
                                        rightStickX = dx.coerceIn(-1f, 1f),
                                        rightStickY = dy.coerceIn(-1f, 1f)
                                    )
                                }
                            },
                            onClickChange = { pressed ->
                                if (pressed) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnTouchpad = pressed) }
                            },
                            onRightClickChange = { pressed ->
                                if (pressed) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnStart = pressed) }
                            },
                            onTriggerHaptic = onTriggerHaptic
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
                            CenterPillButton(label = "SELECT", isPressed = state.btnSelect) {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnSelect = it) }
                            }
                            CenterPillButton(label = "START", isPressed = state.btnStart) {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnStart = it) }
                            }
                        }
                        CenterPillButton(label = "HOME", isPressed = state.btnHome, color = VividIndigo) {
                            if (it) onTriggerHaptic()
                            onUpdateState { s -> s.copy(btnHome = it) }
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
                            onBumperChange = {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnR1 = it) }
                            },
                            onTriggerChange = {
                                if (it) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f) }
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
                            size = 140.dp,
                            onMove = { x, y ->
                                onUpdateState { s -> s.copy(rightStickX = x, rightStickY = y) }
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
                            onButtonChange = { btn, pressed ->
                                if (pressed) onTriggerHaptic()
                                when (btn) {
                                    "A" -> onUpdateState { s -> s.copy(btnA = pressed) }
                                    "B" -> onUpdateState { s -> s.copy(btnB = pressed) }
                                    "X" -> onUpdateState { s -> s.copy(btnX = pressed) }
                                    "Y" -> onUpdateState { s -> s.copy(btnY = pressed) }
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
                    ShoulderGroup("L1", "L2", ElectricCyan, {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnL1 = it) }
                    }, {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f) }
                    })
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
                    ShoulderGroup("R1", "R2", ElectricCyan, {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnR1 = it) }
                    }, {
                        if (it) onTriggerHaptic()
                        onUpdateState { s -> s.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f) }
                    })
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
                        onMove = { x, y ->
                            onUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
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
                        onButtonChange = { btn, pressed ->
                            if (pressed) onTriggerHaptic()
                            when (btn) {
                                "A" -> onUpdateState { s -> s.copy(btnA = pressed) }
                                "B" -> onUpdateState { s -> s.copy(btnB = pressed) }
                                "X" -> onUpdateState { s -> s.copy(btnX = pressed) }
                                "Y" -> onUpdateState { s -> s.copy(btnY = pressed) }
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
                            onRelativeMove = { dx, dy ->
                                onUpdateState { s ->
                                    s.copy(
                                        touchpadX = dx,
                                        touchpadY = dy,
                                        rightStickX = dx.coerceIn(-1f, 1f),
                                        rightStickY = dy.coerceIn(-1f, 1f)
                                    )
                                }
                            },
                            onClickChange = { pressed ->
                                if (pressed) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnTouchpad = pressed) }
                            },
                            onRightClickChange = { pressed ->
                                if (pressed) onTriggerHaptic()
                                onUpdateState { s -> s.copy(btnStart = pressed) }
                            },
                            onTriggerHaptic = onTriggerHaptic
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
                        CenterPillButton("SELECT", state.btnSelect) {
                            if (it) onTriggerHaptic()
                            onUpdateState { s -> s.copy(btnSelect = it) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        CenterPillButton("START", state.btnStart) {
                            if (it) onTriggerHaptic()
                            onUpdateState { s -> s.copy(btnStart = it) }
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
                        onMove = { x, y ->
                            onUpdateState { s -> s.copy(rightStickX = x, rightStickY = y) }
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
    buttonSize: androidx.compose.ui.unit.Dp = 52.dp,
    onButtonChange: (String, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(150.dp)
            .testTag("action_buttons_diamond"),
        contentAlignment = Alignment.Center
    ) {
        // Y button (Top)
        GameButton(
            label = "Y",
            size = buttonSize,
            accentColor = ButtonYColor,
            modifier = Modifier.align(Alignment.TopCenter),
            onPressChange = { onButtonChange("Y", it) }
        )

        // A button (Bottom)
        GameButton(
            label = "A",
            size = buttonSize,
            accentColor = ButtonAColor,
            modifier = Modifier.align(Alignment.BottomCenter),
            onPressChange = { onButtonChange("A", it) }
        )

        // X button (Left)
        GameButton(
            label = "X",
            size = buttonSize,
            accentColor = ButtonXColor,
            modifier = Modifier.align(Alignment.CenterStart),
            onPressChange = { onButtonChange("X", it) }
        )

        // B button (Right)
        GameButton(
            label = "B",
            size = buttonSize,
            accentColor = ButtonBColor,
            modifier = Modifier.align(Alignment.CenterEnd),
            onPressChange = { onButtonChange("B", it) }
        )
    }
}

@Composable
fun CenterPillButton(
    label: String,
    isPressed: Boolean,
    color: Color = Color(0xFF64748B),
    onPressChange: (Boolean) -> Unit
) {
    ShoulderBumper(
        label = label,
        width = 62.dp,
        height = 28.dp,
        accentColor = color,
        onPressChange = onPressChange
    )
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
    onUpdateState: ((GamepadState) -> GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Brake Pedal & Handbrake
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("BRAKE / REVERSE", style = MaterialTheme.typography.labelMedium, color = CoralRed, fontWeight = FontWeight.Bold)

            PedalButton(
                label = "BRAKE",
                subLabel = "L2",
                color = CoralRed,
                height = 130.dp,
                onPressChange = {
                    if (it) onTriggerHaptic()
                    onUpdateState { s -> s.copy(btnL2 = it, btnB = it, leftTrigger = if (it) 1f else 0f) }
                }
            )

            ShoulderBumper(label = "HANDBRAKE (L1)", width = 140.dp, height = 40.dp, accentColor = CoralRed) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnL1 = it, dpadDown = it) }
            }
        }

        // Center: Wheel / Steering stick + Nitro
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("STEER", style = MaterialTheme.typography.labelMedium, color = ElectricCyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            AnalogStick(
                label = "WHEEL",
                size = 150.dp,
                onMove = { x, y ->
                    onUpdateState { s -> s.copy(leftStickX = x, leftStickY = y) }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            GameButton(
                label = "NOS",
                size = 56.dp,
                accentColor = VividIndigo
            ) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnX = it, btnTurbo = it) }
            }
        }

        // Right: Gas / Throttle Pedal & Boost
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceAround,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("THROTTLE / GAS", style = MaterialTheme.typography.labelMedium, color = EmeraldGreen, fontWeight = FontWeight.Bold)

            PedalButton(
                label = "GAS",
                subLabel = "R2",
                color = EmeraldGreen,
                height = 130.dp,
                onPressChange = {
                    if (it) onTriggerHaptic()
                    onUpdateState { s -> s.copy(btnR2 = it, btnA = it, rightTrigger = if (it) 1f else 0f) }
                }
            )

            ShoulderBumper(label = "BOOST (R1)", width = 140.dp, height = 40.dp, accentColor = EmeraldGreen) {
                if (it) onTriggerHaptic()
                onUpdateState { s -> s.copy(btnR1 = it, btnY = it) }
            }
        }
    }
}

@Composable
fun PedalButton(
    label: String,
    subLabel: String,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        if (isPressed) color.copy(alpha = 0.5f) else DarkSurfaceVariant,
                        if (isPressed) color.copy(alpha = 0.2f) else Color(0xFF131A29)
                    )
                )
            )
            .border(
                width = if (isPressed) 2.5.dp else 1.dp,
                color = if (isPressed) color else DarkBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .pointerInput(Unit) {
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
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isPressed) Color.White else color)
            Text(subLabel, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
