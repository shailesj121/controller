package com.example.ui.receiver

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.games.ApexRacerGame
import com.example.games.GalaxyDefenderGame
import com.example.model.GamepadState
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
fun ReceiverScreen(
    serverIp: String,
    serverPort: Int,
    connectedClientIp: String?,
    packetRateHz: Int,
    gamepadState: GamepadState,
    onTriggerRumble: (Long, Float) -> Unit,
    onSwitchToController: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("receiver_screen")
    ) {
        // Top status header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .border(0.5.dp, DarkBorder)
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
                        .background(if (connectedClientIp != null) EmeraldGreen else Color(0xFFFFD600))
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TABLET GAME RECEIVER", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Text("• $serverIp:$serverPort", fontSize = 12.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (connectedClientIp != null) "Phone Linked ($connectedClientIp) • $packetRateHz Hz" else "Auto-Beacon Active • Open Controller on your Phone",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // Button to toggle back to Controller Mode
            Button(
                onClick = onSwitchToController,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = "Controller Mode", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Use as Controller", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurfaceVariant,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = ElectricCyan
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Gamepad Tester", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.SportsEsports, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Galaxy Defender", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Apex Speedster", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.TwoWheeler, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = TextSecondary
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("PC / Bridge Setup", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = ElectricCyan,
                unselectedContentColor = TextSecondary
            )
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> GamepadVisualizer(
                    state = gamepadState,
                    packetRateHz = packetRateHz,
                    connectedClientIp = connectedClientIp,
                    onSendTestRumble = { onTriggerRumble(200L, 0.8f) }
                )
                1 -> GalaxyDefenderGame(
                    gamepadState = gamepadState,
                    onTriggerRumble = onTriggerRumble
                )
                2 -> ApexRacerGame(
                    gamepadState = gamepadState,
                    onTriggerRumble = onTriggerRumble
                )
                3 -> ExternalBridgeGuide(
                    serverIp = serverIp,
                    serverPort = serverPort,
                    gamepadState = gamepadState
                )
            }
        }
    }
}

@Composable
fun ExternalBridgeGuide(
    serverIp: String,
    serverPort: Int,
    gamepadState: GamepadState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Connecting to PC or Other Games", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                Text(
                    "You can also use this app to control PC games, emulators (RetroArch, Dolphin, PPSSPP), or custom web/Unity games! " +
                    "Your phone sends ultra-fast UDP packets to $serverIp:$serverPort.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = VividIndigo, modifier = Modifier.size(20.dp))
                    Text("Bluetooth Direct Serial Link", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(
                    "You can pair your phone and tablet directly via Bluetooth RFCOMM without Wi-Fi! " +
                    "Standard SPP UUID: 00001101-0000-1000-8000-00805F9B34FB. " +
                    "Both Wi-Fi UDP (60-120Hz) and Bluetooth SPP streams automatically drive the games and visualizer on this tablet.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Live Wire Protocol Packet Sample", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Compact binary/pipe format (sub-1ms parsing):", fontSize = 12.sp, color = TextSecondary)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0E17))
                        .padding(12.dp)
                ) {
                    Text(
                        text = gamepadState.toPacketString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = EmeraldGreen
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("JSON Packet Format (for Python/Node.js/Web):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A0E17))
                        .padding(12.dp)
                ) {
                    Text(
                        text = gamepadState.toJsonString(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}
