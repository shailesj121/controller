package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.haptics.HapticFeedbackManager
import com.example.model.AppRole
import com.example.model.BluetoothDeviceItem
import com.example.model.ConnectionMedium
import com.example.model.ControllerLayout
import com.example.model.DiscoveredHost
import com.example.model.GamepadState
import com.example.network.BluetoothGamepadManager
import com.example.network.BluetoothHidGamepadManager
import com.example.network.GamepadClient
import com.example.network.GamepadServer
import com.example.network.NetworkUtils
import com.example.sensors.MotionSensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val hapticManager = HapticFeedbackManager(application)
    val motionManager = MotionSensorManager(application)

    private val _appRole = MutableStateFlow(AppRole.CONTROLLER)
    val appRole: StateFlow<AppRole> = _appRole.asStateFlow()

    private val _connectionMedium = MutableStateFlow(ConnectionMedium.WIFI)
    val connectionMedium: StateFlow<ConnectionMedium> = _connectionMedium.asStateFlow()

    private val _currentLayout = MutableStateFlow(ControllerLayout.MODERN)
    val currentLayout: StateFlow<ControllerLayout> = _currentLayout.asStateFlow()

    private val _isHapticEnabled = MutableStateFlow(true)
    val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled.asStateFlow()

    private val _isGyroEnabled = MutableStateFlow(false)
    val isGyroEnabled: StateFlow<Boolean> = _isGyroEnabled.asStateFlow()

    private val _localGamepadState = MutableStateFlow(GamepadState())
    val localGamepadState: StateFlow<GamepadState> = _localGamepadState.asStateFlow()

    private val _targetIp = MutableStateFlow("192.168.1.100")
    val targetIp: StateFlow<String> = _targetIp.asStateFlow()

    private val _targetPort = MutableStateFlow(8888)
    val targetPort: StateFlow<Int> = _targetPort.asStateFlow()

    val client = GamepadClient(
        scope = viewModelScope,
        onRumbleReceived = { duration, intensity ->
            if (_isHapticEnabled.value) {
                hapticManager.rumble(duration, intensity)
            }
        }
    )

    val server = GamepadServer(scope = viewModelScope)

    val bluetoothManager = BluetoothGamepadManager(
        context = application,
        scope = viewModelScope,
        onStateReceived = { receivedState ->
            // Received on Bluetooth Server (Tablet/Receiver)
            // Forward to server receiver state stream
            server.injectState(receivedState)
        },
        onRumbleReceived = { duration, intensity ->
            // Received on Bluetooth Client (Phone)
            if (_isHapticEnabled.value) {
                hapticManager.rumble(duration, intensity)
            }
        }
    )

    val hidManager = BluetoothHidGamepadManager(
        context = application,
        scope = viewModelScope
    )

    // Unified connection status based on active medium
    val isConnected: StateFlow<Boolean> = combine(
        _connectionMedium,
        client.isConnected,
        bluetoothManager.isConnected,
        hidManager.isConnected
    ) { medium, wifiConnected, btConnected, hidConnected ->
        when (medium) {
            ConnectionMedium.WIFI -> wifiConnected
            ConnectionMedium.BLUETOOTH -> btConnected
            ConnectionMedium.BLUETOOTH_HID -> hidConnected
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        // Automatically default candidate IP from network
        val localIp = NetworkUtils.getLocalIpAddress()
        if (localIp != "127.0.0.1") {
            _targetIp.value = localIp
        }

        // Start server in background so receiver is ready whenever user switches to it
        server.start(8888)

        // Observe motion sensors if enabled
        viewModelScope.launch {
            motionManager.motionFlow.collect { motion ->
                if (_isGyroEnabled.value) {
                    val current = _localGamepadState.value
                    val updated = current.copy(
                        pitch = motion.pitch,
                        roll = motion.roll,
                        yaw = motion.yaw
                    )
                    updateLocalState(updated)
                }
            }
        }
    }

    fun setConnectionMedium(medium: ConnectionMedium) {
        _connectionMedium.value = medium
        when (medium) {
            ConnectionMedium.BLUETOOTH -> {
                bluetoothManager.checkBluetoothStatus()
                if (_appRole.value == AppRole.RECEIVER) {
                    bluetoothManager.startServer()
                }
            }
            ConnectionMedium.BLUETOOTH_HID -> {
                hidManager.refreshPairedDevices()
            }
            ConnectionMedium.WIFI -> {}
        }
    }

    fun connectBluetooth(address: String) {
        _connectionMedium.value = ConnectionMedium.BLUETOOTH
        bluetoothManager.connectToDevice(address)
    }

    fun connectBluetoothHid(address: String) {
        _connectionMedium.value = ConnectionMedium.BLUETOOTH_HID
        hidManager.connectToHost(address)
    }

    fun setAppRole(role: AppRole) {
        _appRole.value = role
        if (role == AppRole.RECEIVER) {
            server.start(_targetPort.value)
            if (_connectionMedium.value == ConnectionMedium.BLUETOOTH) {
                bluetoothManager.startServer()
            }
        } else {
            bluetoothManager.stopServer()
        }
    }

    fun setLayout(layout: ControllerLayout) {
        _currentLayout.value = layout
    }

    fun toggleHaptic(enabled: Boolean) {
        _isHapticEnabled.value = enabled
        hapticManager.isEnabled = enabled
    }

    fun toggleGyro(enabled: Boolean) {
        _isGyroEnabled.value = enabled
        motionManager.isEnabled = enabled
    }

    fun connectToHost(ip: String, port: Int = 8888) {
        _connectionMedium.value = ConnectionMedium.WIFI
        _targetIp.value = ip
        _targetPort.value = port
        client.connect(ip, port)
    }

    fun disconnect() {
        client.disconnect()
        bluetoothManager.disconnect()
        hidManager.disconnect()
    }

    fun updateLocalState(newState: GamepadState) {
        _localGamepadState.value = newState
        when (_connectionMedium.value) {
            ConnectionMedium.WIFI -> client.sendState(newState)
            ConnectionMedium.BLUETOOTH -> bluetoothManager.sendState(newState)
            ConnectionMedium.BLUETOOTH_HID -> hidManager.sendState(newState)
        }
    }

    /**
     * Atomically mutate the local gamepad state with a transform function.
     * Prevents multi-touch race conditions (e.g. moving LS while moving RS or tapping buttons).
     */
    fun updateState(transform: (GamepadState) -> GamepadState) {
        val newState = _localGamepadState.updateAndGet(transform)
        when (_connectionMedium.value) {
            ConnectionMedium.WIFI -> client.sendState(newState)
            ConnectionMedium.BLUETOOTH -> bluetoothManager.sendState(newState)
            ConnectionMedium.BLUETOOTH_HID -> hidManager.sendState(newState)
        }
    }

    fun triggerHapticClick() {
        if (_isHapticEnabled.value) {
            hapticManager.click()
        }
    }

    fun sendServerRumble(durationMs: Long, intensity: Float) {
        server.sendRumble(durationMs, intensity)
        bluetoothManager.sendRumble(durationMs, intensity)
        // Also if running on same test device, vibrate locally
        if (_isHapticEnabled.value) {
            hapticManager.rumble(durationMs, intensity)
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.disconnect()
        server.stop()
        bluetoothManager.destroy()
        hidManager.destroy()
        motionManager.stop()
    }
}
