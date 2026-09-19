package com.example.ui.receiver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadState
import com.example.ui.theme.ButtonAColor
import com.example.ui.theme.ButtonBColor
import com.example.ui.theme.ButtonXColor
import com.example.ui.theme.ButtonYColor
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
fun GamepadVisualizer(
    state: GamepadState,
    packetRateHz: Int,
    connectedClientIp: String?,
    onSendTestRumble: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "LINK STATUS",
                value = if (connectedClientIp != null) "ACTIVE" else "WAITING",
                accentColor = if (connectedClientIp != null) EmeraldGreen else Color(0xFFFFD600),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "CONNECTED CONTROLLER",
                value = connectedClientIp ?: "None",
                accentColor = ElectricCyan,
                modifier = Modifier.weight(1.3f)
            )
            StatCard(
                label = "INPUT RATE",
                value = "$packetRateHz Hz",
                accentColor = VividIndigo,
                modifier = Modifier.weight(0.9f)
            )
        }

        // Live Schematic Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Shoulder & Trigger row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TriggerStatusBadge("L2 Trigger", state.btnL2 || state.leftTrigger > 0.1f, state.leftTrigger)
                    TriggerStatusBadge("L1 Bumper", state.btnL1, if (state.btnL1) 1f else 0f)
                    TriggerStatusBadge("R1 Bumper", state.btnR1, if (state.btnR1) 1f else 0f)
                    TriggerStatusBadge("R2 Trigger", state.btnR2 || state.rightTrigger > 0.1f, state.rightTrigger)
                }

                // Middle Gamepad Schematic: Left stick, D-Pad, Center, Right buttons, Right stick
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Stick Radar
                    StickRadar(
                        label = "Left Stick (LS)",
                        x = state.leftStickX,
                        y = state.leftStickY,
                        isClicked = state.btnL3
                    )

                    // D-Pad Status Visualizer
                    DPadVisualizerGrid(state)

                    // Center Buttons
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PillStatusBadge("SELECT", state.btnSelect)
                        PillStatusBadge("START", state.btnStart)
                        PillStatusBadge("TURBO", state.btnTurbo)
                    }

                    // Action Diamond Buttons Visualizer
                    DiamondButtonsVisualizer(state)

                    // Right Stick Radar
                    StickRadar(
                        label = "Right Stick (RS)",
                        x = state.rightStickX,
                        y = state.rightStickY,
                        isClicked = state.btnR3
                    )
                }

                // Bottom motion & rumble test bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Sensors, contentDescription = "Gyro", tint = ElectricCyan)
                        Text("Motion Pitch: ${state.pitch.toInt()}°", fontSize = 12.sp, color = TextPrimary)
                        Text("Roll: ${state.roll.toInt()}°", fontSize = 12.sp, color = TextPrimary)
                    }

                    Button(
                        onClick = onSendTestRumble,
                        colors = ButtonDefaults.buttonColors(containerColor = VividIndigo),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Vibration, contentDescription = "Test Rumble", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Rumble to Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

@Composable
private fun TriggerStatusBadge(label: String, isActive: Boolean, value: Float) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) ElectricCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
            .border(1.dp, if (isActive) ElectricCyan else DarkBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isActive) ElectricCyan else TextSecondary)
        Text("${(value * 100).toInt()}%", fontSize = 10.sp, color = if (isActive) Color.White else TextSecondary)
    }
}

@Composable
private fun StickRadar(label: String, x: Float, y: Float, isClicked: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A))
                .border(1.dp, DarkBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                // Crosshairs
                drawLine(Color(0xFF334155), Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 1.dp.toPx())
                drawLine(Color(0xFF334155), Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 1.dp.toPx())

                // Stick pointer
                val maxOffset = radius * 0.75f
                val dotX = center.x + x * maxOffset
                val dotY = center.y + y * maxOffset

                drawCircle(
                    color = if (isClicked) EmeraldGreen else ElectricCyan,
                    radius = 12.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("X: ${"%.2f".format(x)}  Y: ${"%.2f".format(y)}", fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun DPadVisualizerGrid(state: GamepadState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("D-PAD", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DPadButtonBox("▲", state.dpadUp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DPadButtonBox("◀", state.dpadLeft)
                DPadButtonBox("●", false)
                DPadButtonBox("▶", state.dpadRight)
            }
            DPadButtonBox("▼", state.dpadDown)
        }
    }
}

@Composable
private fun DPadButtonBox(symbol: String, isPressed: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPressed) ElectricCyan else DarkSurfaceVariant)
            .border(1.dp, if (isPressed) ElectricCyan else DarkBorder, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isPressed) Color(0xFF00363D) else TextSecondary)
    }
}

@Composable
private fun DiamondButtonsVisualizer(state: GamepadState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ACTION BUTTONS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        Spacer(modifier = Modifier.height(6.dp))

        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            VisualizerButton("Y", state.btnY, ButtonYColor, Modifier.align(Alignment.TopCenter))
            VisualizerButton("A", state.btnA, ButtonAColor, Modifier.align(Alignment.BottomCenter))
            VisualizerButton("X", state.btnX, ButtonXColor, Modifier.align(Alignment.CenterStart))
            VisualizerButton("B", state.btnB, ButtonBColor, Modifier.align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun VisualizerButton(label: String, isPressed: Boolean, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isPressed) color else DarkSurfaceVariant)
            .border(1.5.dp, if (isPressed) Color.White else color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.White else color
        )
    }
}

@Composable
private fun PillStatusBadge(label: String, isPressed: Boolean) {
    Box(
        modifier = Modifier
            .width(68.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) ElectricCyan else DarkSurfaceVariant)
            .border(1.dp, if (isPressed) ElectricCyan else DarkBorder, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color(0xFF00363D) else TextSecondary
        )
    }
}
