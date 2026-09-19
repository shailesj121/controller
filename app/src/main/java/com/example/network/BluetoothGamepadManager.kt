package com.example.network

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Build
import com.example.model.BluetoothDeviceItem
import com.example.model.GamepadState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.UUID

/**
 * Manages standard Bluetooth RFCOMM connections for low-latency direct gamepad transmission
 * between Phone (Client) and Tablet (Server) without requiring Wi-Fi.
 */
class BluetoothGamepadManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStateReceived: (GamepadState) -> Unit,
    private val onRumbleReceived: (Long, Float) -> Unit
) {
    companion object {
        // Standard SPP UUID for custom serial data transfer
        val GAMEPAD_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "GamepadDirectLink"
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isServerListening = MutableStateFlow(false)
    val isServerListening: StateFlow<Boolean> = _isServerListening.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedDeviceName = MutableStateFlow<String?>(null)
    val connectedDeviceName: StateFlow<String?> = _connectedDeviceName.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceItem>> = _pairedDevices.asStateFlow()

    private val _statusMessage = MutableStateFlow("Bluetooth Ready")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var serverSocket: BluetoothServerSocket? = null
    private var activeSocket: BluetoothSocket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    private var serverJob: Job? = null
    private var clientJob: Job? = null
    private var readJob: Job? = null

    fun checkBluetoothStatus() {
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        refreshPairedDevices()
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
        } catch (e: SecurityException) {
            _statusMessage.value = "Bluetooth permissions required"
        }
    }

    /**
     * Start Bluetooth Server (Typically on Tablet / Receiver)
     */
    @SuppressLint("MissingPermission")
    fun startServer() {
        stopServer()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusMessage.value = "Bluetooth is turned off"
            return
        }

        serverJob = scope.launch(Dispatchers.IO) {
            try {
                _statusMessage.value = "Starting Bluetooth listener..."
                serverSocket = try {
                    bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(SERVICE_NAME, GAMEPAD_UUID)
                } catch (_: Exception) {
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, GAMEPAD_UUID)
                }
                _isServerListening.value = true
                _statusMessage.value = "Listening for Bluetooth Gamepad..."

                while (isActive && _isServerListening.value) {
                    try {
                        val socket = serverSocket?.accept() ?: break
                        handleConnectedSocket(socket, isServer = true)
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: SecurityException) {
                _statusMessage.value = "Bluetooth permission not granted"
            } catch (e: Exception) {
                _statusMessage.value = "Server error: ${e.localizedMessage}"
            } finally {
                _isServerListening.value = false
            }
        }
    }

    fun stopServer() {
        _isServerListening.value = false
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    /**
     * Connect to Bluetooth Server (Typically from Phone / Controller)
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        disconnect()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusMessage.value = "Bluetooth is turned off"
            return
        }

        clientJob = scope.launch(Dispatchers.IO) {
            try {
                _statusMessage.value = "Connecting to $deviceAddress..."
                val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)
                
                // Cancel discovery as it slows down connection
                bluetoothAdapter.cancelDiscovery()
                delay(250)

                var socket: BluetoothSocket? = null
                var lastException: Exception? = null

                // Strategy 1: Try Insecure RFCOMM (avoids PIN prompt and pairing handshake dropouts)
                try {
                    val s = device.createInsecureRfcommSocketToServiceRecord(GAMEPAD_UUID)
                    s.connect()
                    socket = s
                } catch (e1: Exception) {
                    lastException = e1
                    // Strategy 2: Try Secure RFCOMM
                    try {
                        val s = device.createRfcommSocketToServiceRecord(GAMEPAD_UUID)
                        s.connect()
                        socket = s
                    } catch (e2: Exception) {
                        lastException = e2
                        // Strategy 3: Try Reflection on RFCOMM Channel 1 (classic Android workaround for "read ret: -1")
                        try {
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            val s = method.invoke(device, 1) as BluetoothSocket
                            s.connect()
                            socket = s
                        } catch (e3: Exception) {
                            lastException = e3
                        }
                    }
                }

                if (socket != null && socket.isConnected) {
                    handleConnectedSocket(socket, isServer = false)
                } else {
                    throw lastException ?: Exception("Could not establish RFCOMM connection")
                }
            } catch (e: SecurityException) {
                _statusMessage.value = "Bluetooth permission missing"
                _isConnected.value = false
            } catch (e: Exception) {
                _statusMessage.value = "Connection failed: ${e.message}"
                _isConnected.value = false
                disconnect()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleConnectedSocket(socket: BluetoothSocket, isServer: Boolean) {
        activeSocket = socket
        _isConnected.value = true
        val devName = try {
            socket.remoteDevice?.name ?: socket.remoteDevice?.address ?: "Connected Device"
        } catch (_: SecurityException) {
            "Connected Device"
        }
        _connectedDeviceName.value = devName
        _statusMessage.value = "Connected to $devName"

        writer = PrintWriter(OutputStreamWriter(socket.outputStream, Charsets.UTF_8), true)
        reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))

        // Start read loop
        readJob = scope.launch(Dispatchers.IO) {
            try {
                val inReader = reader ?: return@launch
                while (isActive && _isConnected.value) {
                    val line = inReader.readLine() ?: break
                    if (line.startsWith("RUMBLE|")) {
                        val parts = line.split("|")
                        if (parts.size >= 3) {
                            val duration = parts[1].toLongOrNull() ?: 150L
                            val intensity = parts[2].toFloatOrNull() ?: 0.7f
                            onRumbleReceived(duration, intensity)
                        }
                    } else {
                        val parsed = GamepadState.fromPacketString(line)
                        if (parsed != null) {
                            onStateReceived(parsed)
                        }
                    }
                }
            } catch (e: Exception) {
                // Socket disconnected
            } finally {
                _isConnected.value = false
                _connectedDeviceName.value = null
                _statusMessage.value = "Disconnected"
                disconnect()
            }
        }
    }

    /**
     * Sends controller state over Bluetooth RFCOMM stream
     */
    fun sendState(state: GamepadState) {
        if (!_isConnected.value) return
        scope.launch(Dispatchers.IO) {
            try {
                writer?.println(state.toPacketString())
            } catch (e: Exception) {
                disconnect()
            }
        }
    }

    /**
     * Sends rumble pulse from Tablet/Receiver to Phone
     */
    fun sendRumble(durationMs: Long, intensity: Float) {
        if (!_isConnected.value) return
        scope.launch(Dispatchers.IO) {
            try {
                writer?.println("RUMBLE|$durationMs|$intensity")
            } catch (e: Exception) {
                disconnect()
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        clientJob?.cancel()
        readJob = null
        clientJob = null
        try {
            writer?.close()
            reader?.close()
            activeSocket?.close()
        } catch (_: Exception) {}
        writer = null
        reader = null
        activeSocket = null
        _isConnected.value = false
        _connectedDeviceName.value = null
    }

    fun destroy() {
        disconnect()
        stopServer()
    }
}
