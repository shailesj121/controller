package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ControllerLayout
import com.example.model.DiscoveredHost
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
fun ConnectionDialog(
    currentIp: String,
    currentPort: Int,
    discoveredHosts: List<DiscoveredHost>,
    isConnected: Boolean,
    isHapticEnabled: Boolean,
    isGyroEnabled: Boolean,
    currentLayout: ControllerLayout,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onSelectLayout: (ControllerLayout) -> Unit,
    onRefreshDiscovery: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputIp by remember { mutableStateOf(currentIp) }
    var inputPort by remember { mutableStateOf(currentPort.toString()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(440.dp)
                .testTag("connection_settings_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isConnected) EmeraldGreen else CoralRed)
                        )
                        Text(
                            text = if (isConnected) "Connected to Screen" else "Connect Controller",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurfaceVariant,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = ElectricCyan
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Wireless Link", fontSize = 13.sp) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Layout & Styles", fontSize = 13.sp) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sensors & Haptics", fontSize = 13.sp) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content based on tab
                when (selectedTab) {
                    0 -> ConnectionTabContent(
                        discoveredHosts = discoveredHosts,
                        inputIp = inputIp,
                        inputPort = inputPort,
                        isConnected = isConnected,
                        onIpChange = { inputIp = it },
                        onPortChange = { inputPort = it },
                        onConnect = { onConnect(inputIp.trim(), inputPort.toIntOrNull() ?: 8888) },
                        onDisconnect = onDisconnect,
                        onRefreshDiscovery = onRefreshDiscovery
                    )
                    1 -> LayoutTabContent(
                        currentLayout = currentLayout,
                        onSelectLayout = onSelectLayout
                    )
                    2 -> SettingsTabContent(
                        isHapticEnabled = isHapticEnabled,
                        isGyroEnabled = isGyroEnabled,
                        onToggleHaptic = onToggleHaptic,
                        onToggleGyro = onToggleGyro
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionTabContent(
    discoveredHosts: List<DiscoveredHost>,
    inputIp: String,
    inputPort: String,
    isConnected: Boolean,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Discovered Devices Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Receivers (Auto-Scan)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            IconButton(onClick = onRefreshDiscovery, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.WifiFind, contentDescription = "Refresh", tint = ElectricCyan, modifier = Modifier.size(18.dp))
            }
        }

        if (discoveredHosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open this app in Receiver mode on your Tablet to auto-pair!",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.height(70.dp)) {
                items(discoveredHosts) { host ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated)
                            .clickable {
                                onIpChange(host.ip)
                                onPortChange(host.port.toString())
                                onConnect()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(host.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                            Text("${host.ip}:${host.port}", fontSize = 11.sp, color = TextSecondary)
                        }
                        Text("Connect >", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Manual IP input
        Text(
            text = "Or Enter Tablet / Receiver IP Manually",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputIp,
                onValueChange = onIpChange,
                label = { Text("IP Address (e.g. 192.168.1.50)", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(2f)
            )

            OutlinedTextField(
                value = inputPort,
                onValueChange = onPortChange,
                label = { Text("Port", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isConnected) {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Connect Link", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LayoutTabContent(
    currentLayout: ControllerLayout,
    onSelectLayout: (ControllerLayout) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ControllerLayout.values().forEach { layout ->
            val isSelected = layout == currentLayout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) DarkSurfaceElevated else DarkSurfaceVariant)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else DarkBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectLayout(layout) }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = layout.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) ElectricCyan else TextPrimary
                    )
                    Text(
                        text = layout.description,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    isHapticEnabled: Boolean,
    isGyroEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Haptic feedback
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Vibration, contentDescription = "Haptics", tint = ElectricCyan)
                Column {
                    Text("Haptic Feedback", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Vibrate on button tap & rumble", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Switch(
                checked = isHapticEnabled,
                onCheckedChange = onToggleHaptic,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricCyan,
                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)
                )
            )
        }

        // Gyro Motion Steering
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Sensors, contentDescription = "Gyro", tint = ElectricCyan)
                Column {
                    Text("Motion Steering (Gyro)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Tilt phone to steer in racing games", fontSize = 12.sp, color = TextSecondary)
                }
            }
            Switch(
                checked = isGyroEnabled,
                onCheckedChange = onToggleGyro,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ElectricCyan,
                    checkedTrackColor = ElectricCyan.copy(alpha = 0.4f)
                )
            )
        }
    }
}
