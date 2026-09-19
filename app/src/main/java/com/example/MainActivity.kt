package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppRole
import com.example.model.ConnectionMedium
import com.example.ui.components.UnifiedConnectionDialog
import com.example.ui.controller.ControllerScreen
import com.example.ui.receiver.ReceiverScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: AppViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Keep screen on during gameplay so phone does not go to sleep while playing
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
          MainContent(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun MainContent(viewModel: AppViewModel) {
  val role by viewModel.appRole.collectAsStateWithLifecycle()
  val layout by viewModel.currentLayout.collectAsStateWithLifecycle()
  val medium by viewModel.connectionMedium.collectAsStateWithLifecycle()
  val isHaptic by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
  val isGyro by viewModel.isGyroEnabled.collectAsStateWithLifecycle()
  val localState by viewModel.localGamepadState.collectAsStateWithLifecycle()

  // Unified & Wi-Fi connection states
  val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
  val isWifiConnected by viewModel.client.isConnected.collectAsStateWithLifecycle()
  val pingMs by viewModel.client.pingMs.collectAsStateWithLifecycle()
  val discoveredHosts by viewModel.client.discoveredHosts.collectAsStateWithLifecycle()
  val targetIp by viewModel.targetIp.collectAsStateWithLifecycle()
  val targetPort by viewModel.targetPort.collectAsStateWithLifecycle()

  // Bluetooth states
  val isBtConnected by viewModel.bluetoothManager.isConnected.collectAsStateWithLifecycle()
  val connectedBtDeviceName by viewModel.bluetoothManager.connectedDeviceName.collectAsStateWithLifecycle()
  val pairedBtDevices by viewModel.bluetoothManager.pairedDevices.collectAsStateWithLifecycle()
  val isBluetoothEnabled by viewModel.bluetoothManager.isBluetoothEnabled.collectAsStateWithLifecycle()
  val btStatusMessage by viewModel.bluetoothManager.statusMessage.collectAsStateWithLifecycle()

  // Bluetooth HID (Minecraft / PC Gamepad) states
  val isHidSupported by viewModel.hidManager.isSupported.collectAsStateWithLifecycle()
  val isHidRegistered by viewModel.hidManager.isRegistered.collectAsStateWithLifecycle()
  val isHidConnected by viewModel.hidManager.isConnected.collectAsStateWithLifecycle()
  val connectedHidDeviceName by viewModel.hidManager.connectedDeviceName.collectAsStateWithLifecycle()
  val hidStatusMessage by viewModel.hidManager.statusMessage.collectAsStateWithLifecycle()
  val pairedHidDevices by viewModel.hidManager.pairedDevices.collectAsStateWithLifecycle()

  // Server states
  val serverIp by viewModel.server.serverIp.collectAsStateWithLifecycle()
  val connectedClientIp by viewModel.server.connectedClient.collectAsStateWithLifecycle()
  val serverGamepadState by viewModel.server.latestState.collectAsStateWithLifecycle()
  val packetRateHz by viewModel.server.packetRateHz.collectAsStateWithLifecycle()

  var showConnectionDialog by remember { mutableStateOf(false) }

  when (role) {
    AppRole.CONTROLLER -> {
      ControllerScreen(
        gamepadState = localState,
        layout = layout,
        isConnected = isConnected,
        connectionMedium = medium,
        pingMs = pingMs,
        isHapticEnabled = isHaptic,
        isGyroEnabled = isGyro,
        onStateUpdated = { viewModel.updateLocalState(it) },
        onUpdateState = { transform -> viewModel.updateState(transform) },
        onOpenSettings = {
          viewModel.bluetoothManager.checkBluetoothStatus()
          viewModel.hidManager.refreshPairedDevices()
          showConnectionDialog = true
        },
        onSwitchToReceiver = { viewModel.setAppRole(AppRole.RECEIVER) },
        onTriggerHaptic = { viewModel.triggerHapticClick() }
      )
    }
    AppRole.RECEIVER -> {
      // In receiver mode, we display the tablet screen with games and visualizer.
      // If local gamepad state has inputs (e.g. testing on single device), blend local or remote state.
      val effectiveState = if (connectedClientIp != null) serverGamepadState else {
        if (localState.toBitmask() != 0 || localState.leftStickX != 0f || localState.leftStickY != 0f) localState else serverGamepadState
      }

      ReceiverScreen(
        serverIp = serverIp,
        serverPort = targetPort,
        connectedClientIp = connectedClientIp,
        packetRateHz = packetRateHz,
        gamepadState = effectiveState,
        onTriggerRumble = { duration, intensity ->
          viewModel.sendServerRumble(duration, intensity)
        },
        onSwitchToController = { viewModel.setAppRole(AppRole.CONTROLLER) }
      )
    }
  }

  if (showConnectionDialog) {
    UnifiedConnectionDialog(
      currentRole = role,
      onSelectRole = { viewModel.setAppRole(it) },
      currentMedium = medium,
      onSelectMedium = { viewModel.setConnectionMedium(it) },
      // Wi-Fi
      currentIp = targetIp,
      currentPort = targetPort,
      discoveredHosts = discoveredHosts,
      isWifiConnected = isWifiConnected,
      onConnectWifi = { ip, port ->
        viewModel.connectToHost(ip, port)
        showConnectionDialog = false
      },
      onDisconnectWifi = {
        viewModel.disconnect()
      },
      onRefreshDiscovery = { viewModel.client.sendDiscoveryBroadcast() },
      // Bluetooth
      isBtConnected = isBtConnected,
      connectedBtDeviceName = connectedBtDeviceName,
      pairedBtDevices = pairedBtDevices,
      isBluetoothEnabled = isBluetoothEnabled,
      btStatusMessage = btStatusMessage,
      onConnectBluetooth = { address ->
        viewModel.connectBluetooth(address)
        showConnectionDialog = false
      },
      onDisconnectBluetooth = {
        viewModel.bluetoothManager.disconnect()
      },
      onRefreshBluetooth = {
        viewModel.bluetoothManager.checkBluetoothStatus()
      },
      // Bluetooth HID (Minecraft / PC Gamepad)
      isHidSupported = isHidSupported,
      isHidRegistered = isHidRegistered,
      isHidConnected = isHidConnected,
      connectedHidDeviceName = connectedHidDeviceName,
      hidStatusMessage = hidStatusMessage,
      pairedHidDevices = pairedHidDevices,
      onConnectHid = { address ->
        viewModel.connectBluetoothHid(address)
        showConnectionDialog = false
      },
      onDisconnectHid = {
        viewModel.hidManager.disconnect()
      },
      onRefreshHid = {
        viewModel.hidManager.refreshPairedDevices()
      },
      // Layout & Settings
      currentLayout = layout,
      onSelectLayout = { viewModel.setLayout(it) },
      isHapticEnabled = isHaptic,
      isGyroEnabled = isGyro,
      onToggleHaptic = { viewModel.toggleHaptic(it) },
      onToggleGyro = { viewModel.toggleGyro(it) },
      onDismiss = { showConnectionDialog = false }
    )
  }
}
