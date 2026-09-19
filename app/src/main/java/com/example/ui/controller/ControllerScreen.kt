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
import com.example.model.GamepadState
import com.example.ui.components.AnalogStick
import com.example.ui.components.DPad
import com.example.ui.components.GameButton
import com.example.ui.components.ShoulderBumper
import com.example.ui.components.ShoulderGroup
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
    isConnected: Boolean,
    connectionMedium: ConnectionMedium = ConnectionMedium.WIFI,
    pingMs: Int,
    isHapticEnabled: Boolean,
    isGyroEnabled: Boolean,
    onStateUpdated: (GamepadState) -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchToReceiver: () -> Unit,
    onTriggerHaptic: () -> Unit
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
                onOpenSettings = onOpenSettings,
                onSwitchToReceiver = onSwitchToReceiver
            )

            // Main Pad Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = if (isLandscape) 12.dp else 8.dp, vertical = 6.dp)
            ) {
                when (layout) {
                    ControllerLayout.MODERN -> ModernGamepadLayout(
                        state = gamepadState,
                        isLandscape = isLandscape,
                        onStateUpdated = onStateUpdated,
                        onTriggerHaptic = onTriggerHaptic
                    )
                    ControllerLayout.RETRO_ARCADE -> RetroArcadeLayout(
                        state = gamepadState,
                        isLandscape = isLandscape,
                        onStateUpdated = onStateUpdated,
                        onTriggerHaptic = onTriggerHaptic
                    )
                    ControllerLayout.RACING -> RacingControllerLayout(
                        state = gamepadState,
                        isLandscape = isLandscape,
                        onStateUpdated = onStateUpdated,
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

        // Action icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                Text("Receiver Mode", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
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
    onStateUpdated: (GamepadState) -> Unit,
    onTriggerHaptic: () -> Unit
) {
    if (isLandscape) {
        // Landscape Mode: Left stick & D-pad on left, Buttons & Right stick on right
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
                    ShoulderGroup(
                        bumperLabel = "L1",
                        triggerLabel = "L2",
                        accentColor = ElectricCyan,
                        onBumperChange = {
                            if (it) onTriggerHaptic()
                            onStateUpdated(state.copy(btnL1 = it))
                        },
                        onTriggerChange = {
                            if (it) onTriggerHaptic()
                            onStateUpdated(state.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f))
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalogStick(
                        label = "LS",
                        size = 140.dp,
                        onMove = { x, y ->
                            onStateUpdated(state.copy(leftStickX = x, leftStickY = y))
                        }
                    )

                    DPad(
                        dpadSize = 130.dp,
                        onDirectionChange = { up, down, left, right ->
                            if (up || down || left || right) onTriggerHaptic()
                            onStateUpdated(
                                state.copy(
                                    dpadUp = up,
                                    dpadDown = down,
                                    dpadLeft = left,
                                    dpadRight = right
                                )
                            )
                        }
                    )
                }
            }

            // Center: Select & Start buttons
            Column(
                modifier = Modifier.padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CenterPillButton(label = "SELECT", isPressed = state.btnSelect) {
                        if (it) onTriggerHaptic()
                        onStateUpdated(state.copy(btnSelect = it))
                    }
                    CenterPillButton(label = "START", isPressed = state.btnStart) {
                        if (it) onTriggerHaptic()
                        onStateUpdated(state.copy(btnStart = it))
                    }
                }
                CenterPillButton(label = "HOME", isPressed = state.btnHome, color = VividIndigo) {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnHome = it))
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
                    ShoulderGroup(
                        bumperLabel = "R1",
                        triggerLabel = "R2",
                        accentColor = ElectricCyan,
                        onBumperChange = {
                            if (it) onTriggerHaptic()
                            onStateUpdated(state.copy(btnR1 = it))
                        },
                        onTriggerChange = {
                            if (it) onTriggerHaptic()
                            onStateUpdated(state.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f))
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnalogStick(
                        label = "RS",
                        size = 140.dp,
                        onMove = { x, y ->
                            onStateUpdated(state.copy(rightStickX = x, rightStickY = y))
                        }
                    )

                    DiamondActionButtons(
                        state = state,
                        onButtonChange = { btn, pressed ->
                            if (pressed) onTriggerHaptic()
                            when (btn) {
                                "A" -> onStateUpdated(state.copy(btnA = pressed))
                                "B" -> onStateUpdated(state.copy(btnB = pressed))
                                "X" -> onStateUpdated(state.copy(btnX = pressed))
                                "Y" -> onStateUpdated(state.copy(btnY = pressed))
                            }
                        }
                    )
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
                ShoulderGroup("L1", "L2", ElectricCyan, {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnL1 = it))
                }, {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnL2 = it, leftTrigger = if (it) 1f else 0f))
                })
                ShoulderGroup("R1", "R2", ElectricCyan, {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnR1 = it))
                }, {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnR2 = it, rightTrigger = if (it) 1f else 0f))
                })
            }

            // Middle: Sticks and DPad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalogStick(
                    label = "LS",
                    size = 140.dp,
                    onMove = { x, y ->
                        onStateUpdated(state.copy(leftStickX = x, leftStickY = y))
                    }
                )
                DiamondActionButtons(
                    state = state,
                    onButtonChange = { btn, pressed ->
                        if (pressed) onTriggerHaptic()
                        when (btn) {
                            "A" -> onStateUpdated(state.copy(btnA = pressed))
                            "B" -> onStateUpdated(state.copy(btnB = pressed))
                            "X" -> onStateUpdated(state.copy(btnX = pressed))
                            "Y" -> onStateUpdated(state.copy(btnY = pressed))
                        }
                    }
                )
            }

            // Center pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CenterPillButton("SELECT", state.btnSelect) {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnSelect = it))
                }
                Spacer(modifier = Modifier.width(16.dp))
                CenterPillButton("START", state.btnStart) {
                    if (it) onTriggerHaptic()
                    onStateUpdated(state.copy(btnStart = it))
                }
            }

            // Bottom row: D-Pad & Right Stick
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DPad(
                    dpadSize = 140.dp,
                    onDirectionChange = { up, down, left, right ->
                        if (up || down || left || right) onTriggerHaptic()
                        onStateUpdated(
                            state.copy(
                                dpadUp = up,
                                dpadDown = down,
                                dpadLeft = left,
                                dpadRight = right
                            )
                        )
                    }
                )
                AnalogStick(
                    label = "RS",
                    size = 140.dp,
                    onMove = { x, y ->
                        onStateUpdated(state.copy(rightStickX = x, rightStickY = y))
                    }
                )
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
    onStateUpdated: (GamepadState) -> Unit,
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
                    onStateUpdated(
                        state.copy(
                            dpadUp = up,
                            dpadDown = down,
                            dpadLeft = left,
                            dpadRight = right
                        )
                    )
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
                onStateUpdated(state.copy(btnTurbo = it))
            }
            CenterPillButton("SELECT", state.btnSelect) {
                if (it) onTriggerHaptic()
                onStateUpdated(state.copy(btnSelect = it))
            }
            CenterPillButton("START", state.btnStart) {
                if (it) onTriggerHaptic()
                onStateUpdated(state.copy(btnStart = it))
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
                        onStateUpdated(state.copy(btnX = it))
                    }
                    GameButton(label = "A", size = 68.dp, accentColor = ButtonAColor) {
                        if (it) onTriggerHaptic()
                        onStateUpdated(state.copy(btnA = it))
                    }
                }
                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GameButton(label = "Y", size = 68.dp, accentColor = ButtonYColor) {
                        if (it) onTriggerHaptic()
                        onStateUpdated(state.copy(btnY = it))
                    }
                    GameButton(label = "B", size = 68.dp, accentColor = ButtonBColor) {
                        if (it) onTriggerHaptic()
                        onStateUpdated(state.copy(btnB = it))
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
    onStateUpdated: (GamepadState) -> Unit,
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
                    onStateUpdated(state.copy(btnL2 = it, btnB = it, leftTrigger = if (it) 1f else 0f))
                }
            )

            ShoulderBumper(label = "HANDBRAKE (L1)", width = 140.dp, height = 40.dp, accentColor = CoralRed) {
                if (it) onTriggerHaptic()
                onStateUpdated(state.copy(btnL1 = it, dpadDown = it))
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
                    onStateUpdated(state.copy(leftStickX = x, leftStickY = y))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            GameButton(
                label = "NOS",
                size = 56.dp,
                accentColor = VividIndigo
            ) {
                if (it) onTriggerHaptic()
                onStateUpdated(state.copy(btnX = it, btnTurbo = it))
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
                    onStateUpdated(state.copy(btnR2 = it, btnA = it, rightTrigger = if (it) 1f else 0f))
                }
            )

            ShoulderBumper(label = "BOOST (R1)", width = 140.dp, height = 40.dp, accentColor = EmeraldGreen) {
                if (it) onTriggerHaptic()
                onStateUpdated(state.copy(btnR1 = it, btnY = it))
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

                    waitForUpOrCancellation()
                    isPressed = false
                    onPressChange(false)
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
