package com.example.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.BluetoothDeviceItem
import com.example.model.ConnectionMedium
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
fun UnifiedConnectionDialog(
    currentMedium: ConnectionMedium,
    onSelectMedium: (ConnectionMedium) -> Unit,
    // Wi-Fi params
    currentIp: String,
    currentPort: Int,
    discoveredHosts: List<DiscoveredHost>,
    isWifiConnected: Boolean,
    onConnectWifi: (String, Int) -> Unit,
    onDisconnectWifi: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    // Bluetooth params
    isBtConnected: Boolean,
    connectedBtDeviceName: String?,
    pairedBtDevices: List<BluetoothDeviceItem>,
    isBluetoothEnabled: Boolean,
    btStatusMessage: String,
    onConnectBluetooth: (String) -> Unit,
    onDisconnectBluetooth: () -> Unit,
    onRefreshBluetooth: () -> Unit,
    // Layout & settings
    currentLayout: ControllerLayout,
    onSelectLayout: (ControllerLayout) -> Unit,
    isHapticEnabled: Boolean,
    isGyroEnabled: Boolean,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleGyro: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var inputIp by remember { mutableStateOf(currentIp) }
    var inputPort by remember { mutableStateOf(currentPort.toString()) }

    val isConnected = when (currentMedium) {
        ConnectionMedium.WIFI -> isWifiConnected
        ConnectionMedium.BLUETOOTH -> isBtConnected
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(490.dp)
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
                            text = if (isConnected) "Connected (${currentMedium.title})" else "Connect Controller",
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
                        text = { Text("Connection Link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Layout & Styles", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Sensors & Haptics", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        selectedContentColor = ElectricCyan,
                        unselectedContentColor = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content based on tab
                when (selectedTab) {
                    0 -> DualConnectionTab(
                        currentMedium = currentMedium,
                        onSelectMedium = onSelectMedium,
                        // Wi-Fi
                        discoveredHosts = discoveredHosts,
                        inputIp = inputIp,
                        inputPort = inputPort,
                        isWifiConnected = isWifiConnected,
                        onIpChange = { inputIp = it },
                        onPortChange = { inputPort = it },
                        onConnectWifi = { onConnectWifi(inputIp.trim(), inputPort.toIntOrNull() ?: 8888) },
                        onDisconnectWifi = onDisconnectWifi,
                        onRefreshDiscovery = onRefreshDiscovery,
                        // Bluetooth
                        isBtConnected = isBtConnected,
                        connectedBtDeviceName = connectedBtDeviceName,
                        pairedBtDevices = pairedBtDevices,
                        isBluetoothEnabled = isBluetoothEnabled,
                        btStatusMessage = btStatusMessage,
                        onConnectBluetooth = onConnectBluetooth,
                        onDisconnectBluetooth = onDisconnectBluetooth,
                        onRefreshBluetooth = onRefreshBluetooth
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
private fun DualConnectionTab(
    currentMedium: ConnectionMedium,
    onSelectMedium: (ConnectionMedium) -> Unit,
    // Wi-Fi
    discoveredHosts: List<DiscoveredHost>,
    inputIp: String,
    inputPort: String,
    isWifiConnected: Boolean,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectWifi: () -> Unit,
    onDisconnectWifi: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    // Bluetooth
    isBtConnected: Boolean,
    connectedBtDeviceName: String?,
    pairedBtDevices: List<BluetoothDeviceItem>,
    isBluetoothEnabled: Boolean,
    btStatusMessage: String,
    onConnectBluetooth: (String) -> Unit,
    onDisconnectBluetooth: () -> Unit,
    onRefreshBluetooth: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshBluetooth()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Medium Toggle Bar (Wi-Fi vs Bluetooth)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (currentMedium == ConnectionMedium.WIFI) ElectricCyan else Color.Transparent)
                    .clickable { onSelectMedium(ConnectionMedium.WIFI) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentMedium == ConnectionMedium.WIFI) Color(0xFF00363D) else TextSecondary
                    )
                    Text(
                        "Wi-Fi (UDP LAN)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentMedium == ConnectionMedium.WIFI) Color(0xFF00363D) else TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (currentMedium == ConnectionMedium.BLUETOOTH) VividIndigo else Color.Transparent)
                    .clickable {
                        onSelectMedium(ConnectionMedium.BLUETOOTH)
                        onRefreshBluetooth()
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (currentMedium == ConnectionMedium.BLUETOOTH) Color.White else TextSecondary
                    )
                    Text(
                        "Bluetooth (Direct)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (currentMedium == ConnectionMedium.BLUETOOTH) Color.White else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (currentMedium == ConnectionMedium.WIFI) {
            // Wi-Fi Connection UI
            WifiConnectionSection(
                discoveredHosts = discoveredHosts,
                inputIp = inputIp,
                inputPort = inputPort,
                isWifiConnected = isWifiConnected,
                onIpChange = onIpChange,
                onPortChange = onPortChange,
                onConnectWifi = onConnectWifi,
                onDisconnectWifi = onDisconnectWifi,
                onRefreshDiscovery = onRefreshDiscovery
            )
        } else {
            // Bluetooth Connection UI
            BluetoothConnectionSection(
                isBtConnected = isBtConnected,
                connectedBtDeviceName = connectedBtDeviceName,
                pairedBtDevices = pairedBtDevices,
                isBluetoothEnabled = isBluetoothEnabled,
                btStatusMessage = btStatusMessage,
                onConnectBluetooth = onConnectBluetooth,
                onDisconnectBluetooth = onDisconnectBluetooth,
                onRefreshBluetooth = onRefreshBluetooth
            )
        }
    }
}

@Composable
private fun WifiConnectionSection(
    discoveredHosts: List<DiscoveredHost>,
    inputIp: String,
    inputPort: String,
    isWifiConnected: Boolean,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onConnectWifi: () -> Unit,
    onDisconnectWifi: () -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nearby Tablet Receivers (Auto-Scan)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            IconButton(onClick = onRefreshDiscovery, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.WifiFind, contentDescription = "Refresh", tint = ElectricCyan, modifier = Modifier.size(16.dp))
            }
        }

        if (discoveredHosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No Wi-Fi receivers detected yet. Open Receiver Mode on tablet!",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.height(72.dp)) {
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
                                onConnectWifi()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(host.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                            Text("${host.ip}:${host.port}", fontSize = 10.sp, color = TextSecondary)
                        }
                        Text("Connect >", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Manual IP input
        Text(
            text = "Or Enter Tablet IP & Port Manually:",
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
                label = { Text("IP (e.g. 192.168.1.50)", fontSize = 11.sp) },
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
                label = { Text("Port", fontSize = 11.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            if (isWifiConnected) {
                Button(
                    onClick = onDisconnectWifi,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Disconnect Wi-Fi Link", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onConnectWifi,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connect via Wi-Fi", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BluetoothConnectionSection(
    isBtConnected: Boolean,
    connectedBtDeviceName: String?,
    pairedBtDevices: List<BluetoothDeviceItem>,
    isBluetoothEnabled: Boolean,
    btStatusMessage: String,
    onConnectBluetooth: (String) -> Unit,
    onDisconnectBluetooth: () -> Unit,
    onRefreshBluetooth: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Paired Bluetooth Devices (Phone ↔ Tablet / PC)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            IconButton(onClick = onRefreshBluetooth, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = VividIndigo, modifier = Modifier.size(16.dp))
            }
        }

        // Bluetooth status badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(if (isBluetoothEnabled) DarkSurfaceElevated else CoralRed.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isBluetoothEnabled) btStatusMessage else "Bluetooth is turned off",
                fontSize = 11.sp,
                color = if (isBluetoothEnabled) ElectricCyan else CoralRed
            )
            if (!isBluetoothEnabled) {
                Text(
                    text = "Open Settings",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralRed,
                    modifier = Modifier.clickable {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (pairedBtDevices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No paired Bluetooth devices found.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pair your phone and tablet once in Android Bluetooth settings, then tap Refresh here!",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Open Bluetooth Settings >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = VividIndigo,
                        modifier = Modifier.clickable {
                            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.height(115.dp)) {
                items(pairedBtDevices) { device ->
                    val isThisConnected = isBtConnected && connectedBtDeviceName?.contains(device.name, ignoreCase = true) == true

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isThisConnected) EmeraldGreen.copy(alpha = 0.15f) else DarkSurfaceElevated)
                            .clickable {
                                if (isThisConnected) onDisconnectBluetooth() else onConnectBluetooth(device.address)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                if (isThisConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = if (isThisConnected) EmeraldGreen else VividIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(device.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
                                Text(device.address, fontSize = 10.sp, color = TextSecondary)
                            }
                        }

                        Text(
                            text = if (isThisConnected) "Disconnect" else "Pair Link >",
                            color = if (isThisConnected) CoralRed else VividIndigo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isBtConnected) {
            Button(
                onClick = onDisconnectBluetooth,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Disconnect Bluetooth", fontWeight = FontWeight.Bold)
            }
        }
    }
}
