package com.example.network

import android.os.Build
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class GamepadServer(
    private val scope: CoroutineScope
) {
    private var serverSocket: DatagramSocket? = null
    private var listenJob: Job? = null
    private var beaconJob: Job? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _latestState = MutableStateFlow(GamepadState())
    val latestState: StateFlow<GamepadState> = _latestState.asStateFlow()

    private val _connectedClient = MutableStateFlow<String?>(null)
    val connectedClient: StateFlow<String?> = _connectedClient.asStateFlow()

    private val _packetRateHz = MutableStateFlow(0)
    val packetRateHz: StateFlow<Int> = _packetRateHz.asStateFlow()

    private val _serverIp = MutableStateFlow("0.0.0.0")
    val serverIp: StateFlow<String> = _serverIp.asStateFlow()

    private var lastClientAddr: InetAddress? = null
    private var lastClientPort: Int = 0

    private var packetCounter = 0
    private var lastRateCheckMs = System.currentTimeMillis()

    fun start(port: Int = 8888) {
        if (_isRunning.value) return
        _serverIp.value = NetworkUtils.getLocalIpAddress()

        listenJob = scope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket(port)
                serverSocket = socket
                _isRunning.value = true
                val buffer = ByteArray(1024)

                while (isActive && !socket.isClosed) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        socket.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()

                        lastClientAddr = packet.address
                        lastClientPort = packet.port
                        _connectedClient.value = packet.address.hostAddress

                        if (text.startsWith("PING:")) {
                            val parts = text.split(":")
                            val clientTime = parts.getOrNull(1) ?: "0"
                            val pongMsg = "PONG:$clientTime"
                            val pongBytes = pongMsg.toByteArray()
                            socket.send(DatagramPacket(pongBytes, pongBytes.size, packet.address, packet.port))
                        } else if (text == "GAMEPAD_DISCOVER") {
                            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".take(20)
                            val beaconMsg = "GAMEPAD_BEACON:$deviceName:$port"
                            val beaconBytes = beaconMsg.toByteArray()
                            socket.send(DatagramPacket(beaconBytes, beaconBytes.size, packet.address, 8889))
                        } else if (text.startsWith("GP|")) {
                            val parsed = GamepadState.fromPacketString(text)
                            if (parsed != null) {
                                _latestState.value = parsed
                                packetCounter++
                                val now = System.currentTimeMillis()
                                if (now - lastRateCheckMs >= 1000) {
                                    _packetRateHz.value = packetCounter
                                    packetCounter = 0
                                    lastRateCheckMs = now
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (!isActive) break
                    }
                }
            } catch (e: Exception) {
                // Port in use or socket error
            } finally {
                _isRunning.value = false
            }
        }

        // Start broadcasting discovery beacon
        beaconJob = scope.launch(Dispatchers.IO) {
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".take(20)
            val beaconMsg = "GAMEPAD_BEACON:$deviceName:$port"
            val beaconBytes = beaconMsg.toByteArray()

            while (isActive) {
                try {
                    val broadcastAddr = NetworkUtils.getBroadcastAddress()
                    val bSocket = DatagramSocket()
                    bSocket.broadcast = true
                    val packet = DatagramPacket(beaconBytes, beaconBytes.size, broadcastAddr, 8889)
                    bSocket.send(packet)
                    bSocket.close()
                } catch (e: Exception) {
                    // Ignore broadcast errors
                }
                delay(1500)
            }
        }
    }

    fun injectState(state: GamepadState) {
        _latestState.value = state
        _connectedClient.value = "Bluetooth Controller"
        packetCounter++
        val now = System.currentTimeMillis()
        if (now - lastRateCheckMs >= 1000) {
            _packetRateHz.value = packetCounter
            packetCounter = 0
            lastRateCheckMs = now
        }
    }

    fun stop() {
        listenJob?.cancel()
        beaconJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        serverSocket = null
        _isRunning.value = false
        _connectedClient.value = null
        _packetRateHz.value = 0
    }

    fun sendRumble(durationMs: Long = 150, intensity: Float = 0.8f) {
        val socket = serverSocket ?: return
        val addr = lastClientAddr ?: return
        val port = lastClientPort
        if (port == 0) return

        scope.launch(Dispatchers.IO) {
            try {
                val msg = "RUMBLE:$durationMs:$intensity"
                val bytes = msg.toByteArray()
                socket.send(DatagramPacket(bytes, bytes.size, addr, port))
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
