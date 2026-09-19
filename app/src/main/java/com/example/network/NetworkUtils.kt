package com.example.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {
    /**
     * Finds the local IPv4 address (e.g. 192.168.1.X or 10.0.X.X)
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            var candidateFallback: String? = null

            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (address is Inet4Address && !address.isLoopbackAddress) {
                        val ip = address.hostAddress ?: continue
                        // Prioritize Wi-Fi interfaces (wlan, eth)
                        val name = networkInterface.name.lowercase()
                        if (name.contains("wlan") || name.contains("eth") || name.contains("ap0")) {
                            return ip
                        }
                        if (candidateFallback == null) {
                            candidateFallback = ip
                        }
                    }
                }
            }
            return candidateFallback ?: "127.0.0.1"
        } catch (e: Exception) {
            return "127.0.0.1"
        }
    }

    /**
     * Determines subnet broadcast address for UDP beaconing (e.g. 192.168.1.255)
     */
    fun getBroadcastAddress(): InetAddress {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return InetAddress.getByName("255.255.255.255")
            for (networkInterface in interfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue
                for (interfaceAddress in networkInterface.interfaceAddresses) {
                    val broadcast = interfaceAddress.broadcast
                    if (broadcast != null && broadcast is Inet4Address) {
                        return broadcast
                    }
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return InetAddress.getByName("255.255.255.255")
    }
}
