package com.example.network

import com.example.model.DiscoveredHost
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
import java.net.SocketTimeoutException

class GamepadClient(
    private val scope: CoroutineScope,
    private val onRumbleReceived: (Long, Float) -> Unit
) {
    private var socket: DatagramSocket? = null
    private var sendJob: Job? = null
    private var receiveJob: Job? = null
    private var discoveryJob: Job? = null

    private var targetAddress: InetAddress? = null
    private var targetPort: Int = 8888

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    private val _discoveredHosts = MutableStateFlow<List<DiscoveredHost>>(emptyList())
    val discoveredHosts: StateFlow<List<DiscoveredHost>> = _discoveredHosts.asStateFlow()

    private var currentSeq = 0L
    private var lastSentState: GamepadState? = null

    init {
        startDiscoveryListener()
    }

    fun startDiscoveryListener() {
        discoveryJob?.cancel()
        discoveryJob = scope.launch(Dispatchers.IO) {
            var discoverySocket: DatagramSocket? = null
            try {
                discoverySocket = DatagramSocket(8889)
                discoverySocket.soTimeout = 2000
                val buffer = ByteArray(512)

                while (isActive) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        discoverySocket.receive(packet)
                        val text = String(packet.data, 0, packet.length).trim()
                        if (text.startsWith("GAMEPAD_BEACON:")) {
                            val parts = text.split(":")
                            if (parts.size >= 3) {
                                val name = parts[1]
                                val port = parts[2].toIntOrNull() ?: 8888
                                val ip = packet.address.hostAddress ?: ""
                                if (ip.isNotEmpty()) {
                                    val newHost = DiscoveredHost(ip, port, name, System.currentTimeMillis())
                                    val current = _discoveredHosts.value.toMutableList()
                                    val existingIdx = current.indexOfFirst { it.ip == ip }
                                    if (existingIdx >= 0) {
                                        current[existingIdx] = newHost
                                    } else {
                                        current.add(newHost)
                                    }
                                    _discoveredHosts.value = current
                                }
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        // send discovery ping
                        sendDiscoveryBroadcast()
                    } catch (e: Exception) {
                        delay(1000)
                    }
                }
            } catch (e: Exception) {
                // Ignore port bind error if running server and client on same device
            } finally {
                discoverySocket?.close()
            }
        }
    }

    fun sendDiscoveryBroadcast() {
        scope.launch(Dispatchers.IO) {
            try {
                val broadcastAddr = NetworkUtils.getBroadcastAddress()
                val socket = DatagramSocket()
                socket.broadcast = true
                val msg = "GAMEPAD_DISCOVER"
                val data = msg.toByteArray()
                val packet = DatagramPacket(data, data.size, broadcastAddr, 8888)
                socket.send(packet)
                socket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun connect(host: String, port: Int = 8888) {
        disconnect()
        scope.launch(Dispatchers.IO) {
            try {
                targetAddress = InetAddress.getByName(host)
                targetPort = port
                socket = DatagramSocket()
                _isConnected.value = true

                // Start receiver loop for ping / rumble
                startReceiveLoop()

                // Send initial handshake
                val pingMsg = "PING:${System.currentTimeMillis()}:0"
                val bytes = pingMsg.toByteArray()
                targetAddress?.let { addr ->
                    socket?.send(DatagramPacket(bytes, bytes.size, addr, targetPort))
                }
            } catch (e: Exception) {
                _isConnected.value = false
            }
        }
    }

    fun disconnect() {
        sendJob?.cancel()
        receiveJob?.cancel()
        try {
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        socket = null
        _isConnected.value = false
        _pingMs.value = 0
    }

    fun sendState(state: GamepadState) {
        val sock = socket ?: return
        val addr = targetAddress ?: return
        if (!_isConnected.value) return

        currentSeq++
        val stateToSend = state.copy(seq = currentSeq, timestamp = System.currentTimeMillis())
        lastSentState = stateToSend

        scope.launch(Dispatchers.IO) {
            try {
                val payload = stateToSend.toPacketString().toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(payload, payload.size, addr, targetPort)
                sock.send(packet)
            } catch (e: Exception) {
                // Handle drop
            }
        }
    }

    private fun startReceiveLoop() {
        receiveJob?.cancel()
        receiveJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(256)
            while (isActive && _isConnected.value) {
                try {
                    val sock = socket ?: break
                    val packet = DatagramPacket(buffer, buffer.size)
                    sock.receive(packet)
                    val msg = String(packet.data, 0, packet.length).trim()

                    if (msg.startsWith("PONG:")) {
                        val parts = msg.split(":")
                        if (parts.size >= 2) {
                            val clientSendTime = parts[1].toLongOrNull() ?: 0L
                            if (clientSendTime > 0) {
                                val rtt = (System.currentTimeMillis() - clientSendTime).toInt()
                                _pingMs.value = rtt.coerceIn(1, 999)
                            }
                        }
                    } else if (msg.startsWith("RUMBLE:")) {
                        val parts = msg.split(":")
                        val duration = parts.getOrNull(1)?.toLongOrNull() ?: 120L
                        val intensity = parts.getOrNull(2)?.toFloatOrNull() ?: 0.7f
                        onRumbleReceived(duration, intensity)
                    }
                } catch (e: Exception) {
                    if (!isActive) break
                    delay(50)
                }
            }
        }
    }
}
