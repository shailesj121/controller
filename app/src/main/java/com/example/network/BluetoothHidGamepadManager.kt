package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.example.model.BluetoothDeviceItem
import com.example.model.GamepadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Manages standard Bluetooth HID (Human Interface Device) Gamepad emulation.
 * Allows this phone to act as a genuine physical Bluetooth Gamepad recognized natively
 * by Minecraft, Call of Duty, Emulators, Windows PC, iOS, and other Android devices.
 */
class BluetoothHidGamepadManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        const val REPORT_ID_GAMEPAD = 1

        /**
         * Standard USB/Bluetooth HID Gamepad Report Descriptor.
         * Defines:
         * - Dual Analog Sticks (Left X/Y, Right Z/Rz) 8-bit each (0..255, center 128)
         * - Dual Analog Triggers (Rx, Ry) 8-bit each (0..255)
         * - 8-direction D-Pad Hat Switch (4-bit)
         * - 16 Action Buttons (Button A, B, X, Y, L1, R1, L2, R2, Select, Start, L3, R3, Home)
         */
        val GAMEPAD_REPORT_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(),         // USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x05.toByte(),         // USAGE (Game Pad)
            0xA1.toByte(), 0x01.toByte(),         // COLLECTION (Application)
            0x85.toByte(), 0x01.toByte(),         //   REPORT_ID (1)

            // Left Stick (X, Y): 2 bytes (0 to 255, center 128)
            0x05.toByte(), 0x01.toByte(),         //   USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x30.toByte(),         //   USAGE (X)
            0x09.toByte(), 0x31.toByte(),         //   USAGE (Y)
            0x15.toByte(), 0x00.toByte(),         //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
            0x75.toByte(), 0x08.toByte(),         //   REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(),         //   REPORT_COUNT (2)
            0x81.toByte(), 0x02.toByte(),         //   INPUT (Data, Var, Abs)

            // Right Stick (Z, Rz): 2 bytes (0 to 255, center 128)
            0x09.toByte(), 0x32.toByte(),         //   USAGE (Z)
            0x09.toByte(), 0x35.toByte(),         //   USAGE (Rz)
            0x15.toByte(), 0x00.toByte(),         //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
            0x75.toByte(), 0x08.toByte(),         //   REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(),         //   REPORT_COUNT (2)
            0x81.toByte(), 0x02.toByte(),         //   INPUT (Data, Var, Abs)

            // Analog Triggers (Rx, Ry): 2 bytes (0 to 255)
            0x09.toByte(), 0x33.toByte(),         //   USAGE (Rx - Left Trigger)
            0x09.toByte(), 0x34.toByte(),         //   USAGE (Ry - Right Trigger)
            0x15.toByte(), 0x00.toByte(),         //   LOGICAL_MINIMUM (0)
            0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // LOGICAL_MAXIMUM (255)
            0x75.toByte(), 0x08.toByte(),         //   REPORT_SIZE (8)
            0x95.toByte(), 0x02.toByte(),         //   REPORT_COUNT (2)
            0x81.toByte(), 0x02.toByte(),         //   INPUT (Data, Var, Abs)

            // D-Pad Hat Switch: 1 byte (8 bits: 0=Null/Released, 1=Up, 2=Up-Right, ..., 8=Up-Left)
            0x05.toByte(), 0x01.toByte(),         //   USAGE_PAGE (Generic Desktop)
            0x09.toByte(), 0x39.toByte(),         //   USAGE (Hat switch)
            0x15.toByte(), 0x01.toByte(),         //   LOGICAL_MINIMUM (1) - 0 is outside range (Null state)
            0x25.toByte(), 0x08.toByte(),         //   LOGICAL_MAXIMUM (8)
            0x35.toByte(), 0x00.toByte(),         //   PHYSICAL_MINIMUM (0)
            0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // PHYSICAL_MAXIMUM (315 deg)
            0x65.toByte(), 0x14.toByte(),         //   UNIT (Eng Rot: Angular Pos)
            0x75.toByte(), 0x08.toByte(),         //   REPORT_SIZE (8)
            0x95.toByte(), 0x01.toByte(),         //   REPORT_COUNT (1)
            0x81.toByte(), 0x42.toByte(),         //   INPUT (Data, Var, Abs, Null)

            // 16 Buttons: 2 bytes (16 bits)
            0x05.toByte(), 0x09.toByte(),         //   USAGE_PAGE (Button)
            0x19.toByte(), 0x01.toByte(),         //   USAGE_MINIMUM (Button 1)
            0x29.toByte(), 0x10.toByte(),         //   USAGE_MAXIMUM (Button 16)
            0x15.toByte(), 0x00.toByte(),         //   LOGICAL_MINIMUM (0)
            0x25.toByte(), 0x01.toByte(),         //   LOGICAL_MAXIMUM (1)
            0x75.toByte(), 0x01.toByte(),         //   REPORT_SIZE (1)
            0x95.toByte(), 0x10.toByte(),         //   REPORT_COUNT (16)
            0x81.toByte(), 0x02.toByte(),         //   INPUT (Data, Var, Abs)

            0xC0.toByte()                         // END_COLLECTION
        )
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isSupported = MutableStateFlow(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bluetoothAdapter != null)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    private val _isRegistered = MutableStateFlow(false)
    val isRegistered: StateFlow<Boolean> = _isRegistered.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _statusMessage = MutableStateFlow("Bluetooth HID Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var lastReportBytes = ByteArray(9) { if (it < 4) 128.toByte() else 0 }
    private val executor = Executors.newSingleThreadExecutor()

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                hidDevice = proxy as BluetoothHidDevice
                registerHidApp()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                _isRegistered.value = false
                _isConnected.value = false
                _statusMessage.value = "HID Device service disconnected"
            }
        }
    }

    private val hidCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                _isRegistered.value = registered
                if (registered) {
                    _statusMessage.value = "Gamepad Ready. Pair in host Bluetooth Settings."
                    refreshPairedDevices()
                } else {
                    _statusMessage.value = "HID Registration failed"
                }
            }

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedHost = device
                        _isConnected.value = true
                        val name = try { device?.name ?: device?.address } catch (_: SecurityException) { "Host Device" }
                        _connectedDeviceName.value = name
                        _statusMessage.value = "Gamepad Linked to $name! Open Minecraft."
                        sendState(GamepadState())
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        _statusMessage.value = "Connecting to ${device?.address}..."
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectedHost = null
                        _isConnected.value = false
                        _connectedDeviceName.value = null
                        _statusMessage.value = "Gamepad Disconnected. Waiting for host..."
                    }
                }
            }

            override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && id.toInt() == REPORT_ID_GAMEPAD) {
                    try {
                        hidDevice?.replyReport(device, type, id, lastReportBytes)
                    } catch (_: SecurityException) {}
                }
            }

            override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                    } catch (_: SecurityException) {}
                }
            }

            override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                    } catch (_: SecurityException) {}
                }
            }
        }
    } else null

    private val stateChannel = kotlinx.coroutines.channels.Channel<GamepadState>(kotlinx.coroutines.channels.Channel.CONFLATED)

    init {
        initService()
        scope.launch(Dispatchers.IO) {
            for (state in stateChannel) {
                sendStateInternal(state)
            }
        }
    }

    fun initService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && bluetoothAdapter != null) {
            try {
                bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.HID_DEVICE)
                refreshPairedDevices()
            } catch (e: Exception) {
                _statusMessage.value = "HID Proxy error: ${e.localizedMessage}"
            }
        } else {
            _statusMessage.value = "HID Gamepad requires Android 9 (Pie) or higher"
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerHidApp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || hidDevice == null) return

        try {
            val sdp = BluetoothHidDeviceAppSdpSettings(
                "Game Controller",
                "Android Bluetooth Gamepad",
                "Antigravity",
                BluetoothHidDevice.SUBCLASS2_GAMEPAD,
                GAMEPAD_REPORT_DESCRIPTOR
            )

            hidCallback?.let { callback ->
                hidDevice?.registerApp(sdp, null, null, executor, callback)
            }
        } catch (e: SecurityException) {
            _statusMessage.value = "Bluetooth permissions required for HID"
        } catch (e: Exception) {
            _statusMessage.value = "Failed to register HID Gamepad: ${e.message}"
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }

        try {
            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            _pairedDevices.value = bonded.map { device ->
                BluetoothDeviceItem(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    isPaired = true
                )
            }
        } catch (_: SecurityException) {}
    }

    /**
     * Connect to an existing paired host (Phone 2, Tablet, PC)
     */
    @SuppressLint("MissingPermission")
    fun connectToHost(deviceAddress: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P || hidDevice == null) {
            _statusMessage.value = "HID profile not ready"
            return
        }

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress) ?: return
            _statusMessage.value = "Connecting Gamepad to ${device.name ?: deviceAddress}..."
            hidDevice?.connect(device)
        } catch (e: SecurityException) {
            _statusMessage.value = "Bluetooth permission missing"
        } catch (e: Exception) {
            _statusMessage.value = "Connect error: ${e.message}"
        }
    }

    /**
     * Disconnect from current host
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectedHost?.let { hidDevice?.disconnect(it) }
            } catch (_: SecurityException) {}
        }
        connectedHost = null
        _isConnected.value = false
        _connectedDeviceName.value = null
        _statusMessage.value = "Disconnected"
    }

    /**
     * Transmit current controller state to the host via standard HID report.
     * Recognized by Minecraft and all Android/PC games automatically!
     */
    @SuppressLint("MissingPermission")
    fun sendState(state: GamepadState) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        stateChannel.trySend(state)
    }

    @SuppressLint("MissingPermission")
    private fun sendStateInternal(state: GamepadState) {
        val host = connectedHost ?: return
        val dev = hidDevice ?: return

        try {
            val report = ByteArray(9)

            // 1. Left Stick: X, Y (0..255, center 128) with deadzone
            val lx = if (kotlin.math.abs(state.leftStickX) < 0.05f) 128 else ((state.leftStickX + 1f) * 127.5f).toInt().coerceIn(0, 255)
            val ly = if (kotlin.math.abs(state.leftStickY) < 0.05f) 128 else ((state.leftStickY + 1f) * 127.5f).toInt().coerceIn(0, 255)
            report[0] = lx.toByte()
            report[1] = ly.toByte()

            // 2. Right Stick: Z, Rz (0..255, center 128) with deadzone
            val rx = if (kotlin.math.abs(state.rightStickX) < 0.05f) 128 else ((state.rightStickX + 1f) * 127.5f).toInt().coerceIn(0, 255)
            val ry = if (kotlin.math.abs(state.rightStickY) < 0.05f) 128 else ((state.rightStickY + 1f) * 127.5f).toInt().coerceIn(0, 255)
            report[2] = rx.toByte()
            report[3] = ry.toByte()

            // 3. Triggers: Left (Rx), Right (Ry) (0..255)
            val lt = if (state.btnL2 && state.leftTrigger == 0f) 1f else state.leftTrigger
            val rt = if (state.btnR2 && state.rightTrigger == 0f) 1f else state.rightTrigger
            report[4] = (lt * 255f).toInt().coerceIn(0, 255).toByte()
            report[5] = (rt * 255f).toInt().coerceIn(0, 255).toByte()

            // 4. D-Pad Hat Switch (1 byte: 0=Null/released, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W, 8=NW)
            val hat = when {
                state.dpadUp && state.dpadRight -> 2
                state.dpadDown && state.dpadRight -> 4
                state.dpadDown && state.dpadLeft -> 6
                state.dpadUp && state.dpadLeft -> 8
                state.dpadUp -> 1
                state.dpadRight -> 3
                state.dpadDown -> 5
                state.dpadLeft -> 7
                else -> 0 // 0 is outside [1, 8] range, correctly signaling NULL (released)
            }
            report[6] = hat.toByte()

            // 5. 16 Action Buttons (Standard USB/Bluetooth HID Gamepad button sequence):
            // Button 1 (bit 0): A
            // Button 2 (bit 1): B
            // Button 3 (bit 2): X
            // Button 4 (bit 3): Y
            // Button 5 (bit 4): L1 (LB)
            // Button 6 (bit 5): R1 (RB)
            // Button 7 (bit 6): L2 (LT click)
            // Button 8 (bit 7): R2 (RT click)
            // Button 9 (bit 8): Select / Back
            // Button 10 (bit 9): Start / Menu
            // Button 11 (bit 10): L3 (Left thumb click)
            // Button 12 (bit 11): R3 (Right thumb click)
            // Button 13 (bit 12): Home / Guide
            // Button 14 (bit 13): Turbo / Share
            var buttons = 0
            if (state.btnA) buttons = buttons or (1 shl 0)
            if (state.btnB) buttons = buttons or (1 shl 1)
            if (state.btnX) buttons = buttons or (1 shl 2)
            if (state.btnY) buttons = buttons or (1 shl 3)
            if (state.btnL1) buttons = buttons or (1 shl 4)
            if (state.btnR1) buttons = buttons or (1 shl 5)
            if (state.btnL2 || state.leftTrigger > 0.5f) buttons = buttons or (1 shl 6)
            if (state.btnR2 || state.rightTrigger > 0.5f) buttons = buttons or (1 shl 7)
            if (state.btnSelect) buttons = buttons or (1 shl 8)
            if (state.btnStart) buttons = buttons or (1 shl 9)
            if (state.btnL3) buttons = buttons or (1 shl 10)
            if (state.btnR3) buttons = buttons or (1 shl 11)
            if (state.btnHome) buttons = buttons or (1 shl 12)
            if (state.btnTurbo) buttons = buttons or (1 shl 13)

            report[7] = (buttons and 0xFF).toByte()
            report[8] = ((buttons shr 8) and 0xFF).toByte()

            lastReportBytes = report
            dev.sendReport(host, REPORT_ID_GAMEPAD, report)
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    fun destroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                hidDevice?.let { dev ->
                    hidCallback?.let { dev.unregisterApp() }
                    bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, dev)
                }
            } catch (_: Exception) {}
        }
        executor.shutdown()
    }
}
