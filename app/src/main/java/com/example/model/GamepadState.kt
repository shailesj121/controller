package com.example.model

import java.util.Locale

data class GamepadState(
    val seq: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    // Digital Buttons
    val btnA: Boolean = false,
    val btnB: Boolean = false,
    val btnX: Boolean = false,
    val btnY: Boolean = false,
    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
    val btnL1: Boolean = false,
    val btnR1: Boolean = false,
    val btnL2: Boolean = false,
    val btnR2: Boolean = false,
    val btnL3: Boolean = false,
    val btnR3: Boolean = false,
    val btnSelect: Boolean = false,
    val btnStart: Boolean = false,
    val btnHome: Boolean = false,
    val btnTurbo: Boolean = false,
    // Analog Sticks (-1.0f to 1.0f)
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    // Analog Triggers (0.0f to 1.0f)
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    // Motion / Gyro
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val yaw: Float = 0f
) {
    fun toBitmask(): Int {
        var mask = 0
        if (btnA) mask = mask or (1 shl 0)
        if (btnB) mask = mask or (1 shl 1)
        if (btnX) mask = mask or (1 shl 2)
        if (btnY) mask = mask or (1 shl 3)
        if (dpadUp) mask = mask or (1 shl 4)
        if (dpadDown) mask = mask or (1 shl 5)
        if (dpadLeft) mask = mask or (1 shl 6)
        if (dpadRight) mask = mask or (1 shl 7)
        if (btnL1) mask = mask or (1 shl 8)
        if (btnR1) mask = mask or (1 shl 9)
        if (btnL2) mask = mask or (1 shl 10)
        if (btnR2) mask = mask or (1 shl 11)
        if (btnL3) mask = mask or (1 shl 12)
        if (btnR3) mask = mask or (1 shl 13)
        if (btnSelect) mask = mask or (1 shl 14)
        if (btnStart) mask = mask or (1 shl 15)
        if (btnHome) mask = mask or (1 shl 16)
        if (btnTurbo) mask = mask or (1 shl 17)
        return mask
    }

    /**
     * Compact high-speed UDP packet representation
     * Format: GP|<seq>|<bitmask>|<lx>|<ly>|<rx>|<ry>|<lt>|<rt>|<pitch>|<roll>
     */
    fun toPacketString(): String {
        return String.format(
            Locale.US,
            "GP|%d|%d|%.2f|%.2f|%.2f|%.2f|%.2f|%.2f|%.1f|%.1f",
            seq,
            toBitmask(),
            leftStickX,
            leftStickY,
            rightStickX,
            rightStickY,
            leftTrigger,
            rightTrigger,
            pitch,
            roll
        )
    }

    /**
     * JSON format for PC / browser / API consumers
     */
    fun toJsonString(): String {
        return """
            {
              "seq": $seq,
              "timestamp": $timestamp,
              "buttons": {
                "A": $btnA,
                "B": $btnB,
                "X": $btnX,
                "Y": $btnY,
                "dpadUp": $dpadUp,
                "dpadDown": $dpadDown,
                "dpadLeft": $dpadLeft,
                "dpadRight": $dpadRight,
                "L1": $btnL1,
                "R1": $btnR1,
                "L2": $btnL2,
                "R2": $btnR2,
                "L3": $btnL3,
                "R3": $btnR3,
                "select": $btnSelect,
                "start": $btnStart,
                "home": $btnHome,
                "turbo": $btnTurbo
              },
              "axes": {
                "leftStickX": $leftStickX,
                "leftStickY": $leftStickY,
                "rightStickX": $rightStickX,
                "rightStickY": $rightStickY,
                "leftTrigger": $leftTrigger,
                "rightTrigger": $rightTrigger
              },
              "motion": {
                "pitch": $pitch,
                "roll": $roll,
                "yaw": $yaw
              }
            }
        """.trimIndent()
    }

    companion object {
        fun fromPacketString(packet: String): GamepadState? {
            if (!packet.startsWith("GP|")) return null
            val parts = packet.split("|")
            if (parts.size < 11) return null
            return try {
                val seq = parts[1].toLong()
                val mask = parts[2].toInt()
                val lx = parts[3].toFloat()
                val ly = parts[4].toFloat()
                val rx = parts[5].toFloat()
                val ry = parts[6].toFloat()
                val lt = parts[7].toFloat()
                val rt = parts[8].toFloat()
                val pitch = parts[9].toFloat()
                val roll = parts[10].toFloat()

                GamepadState(
                    seq = seq,
                    timestamp = System.currentTimeMillis(),
                    btnA = (mask and (1 shl 0)) != 0,
                    btnB = (mask and (1 shl 1)) != 0,
                    btnX = (mask and (1 shl 2)) != 0,
                    btnY = (mask and (1 shl 3)) != 0,
                    dpadUp = (mask and (1 shl 4)) != 0,
                    dpadDown = (mask and (1 shl 5)) != 0,
                    dpadLeft = (mask and (1 shl 6)) != 0,
                    dpadRight = (mask and (1 shl 7)) != 0,
                    btnL1 = (mask and (1 shl 8)) != 0,
                    btnR1 = (mask and (1 shl 9)) != 0,
                    btnL2 = (mask and (1 shl 10)) != 0,
                    btnR2 = (mask and (1 shl 11)) != 0,
                    btnL3 = (mask and (1 shl 12)) != 0,
                    btnR3 = (mask and (1 shl 13)) != 0,
                    btnSelect = (mask and (1 shl 14)) != 0,
                    btnStart = (mask and (1 shl 15)) != 0,
                    btnHome = (mask and (1 shl 16)) != 0,
                    btnTurbo = (mask and (1 shl 17)) != 0,
                    leftStickX = lx,
                    leftStickY = ly,
                    rightStickX = rx,
                    rightStickY = ry,
                    leftTrigger = lt,
                    rightTrigger = rt,
                    pitch = pitch,
                    roll = roll
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class ControllerLayout(val title: String, val description: String) {
    MODERN("Modern Dual-Stick", "Full analog dual sticks, triggers, bumpers & action buttons"),
    RETRO_ARCADE("Retro Arcade", "Direct 8-way D-Pad, oversized arcade action buttons & turbo"),
    RACING("Racing & Pedals", "Tilt/wheel steering with gas pedal, brake pedal & handbrake")
}

enum class AppRole(val title: String) {
    CONTROLLER("Controller (Phone)"),
    RECEIVER("Game Console / Receiver (Tablet)")
}

enum class ConnectionMedium(val title: String) {
    WIFI("Wi-Fi (UDP LAN)"),
    BLUETOOTH("Bluetooth (Direct Link)"),
    BLUETOOTH_HID("Bluetooth Gamepad (Minecraft / PC)")
}

data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val isPaired: Boolean = true
)

data class DiscoveredHost(
    val ip: String,
    val port: Int,
    val name: String,
    val lastSeenMs: Long = System.currentTimeMillis()
)
