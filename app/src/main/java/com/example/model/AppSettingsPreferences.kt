package com.example.model

import android.content.Context
import android.content.SharedPreferences

class AppSettingsPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CONTROLLER_LAYOUT = "key_controller_layout"
        private const val KEY_HAPTIC_ENABLED = "key_haptic_enabled"
        private const val KEY_GYRO_ENABLED = "key_gyro_enabled"
        private const val KEY_CONNECTION_MEDIUM = "key_connection_medium"
        private const val KEY_TARGET_IP = "key_target_ip"
        private const val KEY_TARGET_PORT = "key_target_port"
        private const val KEY_APP_ROLE = "key_app_role"
        private const val KEY_RACING_JOYSTICK_MODE = "key_racing_joystick_mode"
    }

    fun getControllerLayout(): ControllerLayout {
        val saved = prefs.getString(KEY_CONTROLLER_LAYOUT, null) ?: return ControllerLayout.MODERN
        return try {
            ControllerLayout.valueOf(saved)
        } catch (e: Exception) {
            ControllerLayout.MODERN
        }
    }

    fun setControllerLayout(layout: ControllerLayout) {
        prefs.edit().putString(KEY_CONTROLLER_LAYOUT, layout.name).apply()
    }

    fun isHapticEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_ENABLED, true)
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_ENABLED, enabled).apply()
    }

    fun isGyroEnabled(): Boolean {
        return prefs.getBoolean(KEY_GYRO_ENABLED, false)
    }

    fun setGyroEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GYRO_ENABLED, enabled).apply()
    }

    fun getConnectionMedium(): ConnectionMedium {
        val saved = prefs.getString(KEY_CONNECTION_MEDIUM, null) ?: return ConnectionMedium.WIFI
        return try {
            ConnectionMedium.valueOf(saved)
        } catch (e: Exception) {
            ConnectionMedium.WIFI
        }
    }

    fun setConnectionMedium(medium: ConnectionMedium) {
        prefs.edit().putString(KEY_CONNECTION_MEDIUM, medium.name).apply()
    }

    fun getTargetIp(defaultIp: String = "192.168.1.100"): String {
        val saved = prefs.getString(KEY_TARGET_IP, null)
        return if (!saved.isNullOrBlank()) saved else defaultIp
    }

    fun setTargetIp(ip: String) {
        prefs.edit().putString(KEY_TARGET_IP, ip).apply()
    }

    fun getTargetPort(): Int {
        return prefs.getInt(KEY_TARGET_PORT, 8888)
    }

    fun setTargetPort(port: Int) {
        prefs.edit().putInt(KEY_TARGET_PORT, port).apply()
    }

    fun getAppRole(): AppRole {
        val saved = prefs.getString(KEY_APP_ROLE, null) ?: return AppRole.CONTROLLER
        return try {
            AppRole.valueOf(saved)
        } catch (e: Exception) {
            AppRole.CONTROLLER
        }
    }

    fun setAppRole(role: AppRole) {
        prefs.edit().putString(KEY_APP_ROLE, role.name).apply()
    }

    fun isRacingJoystickMode(): Boolean {
        return prefs.getBoolean(KEY_RACING_JOYSTICK_MODE, false)
    }

    fun setRacingJoystickMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RACING_JOYSTICK_MODE, enabled).apply()
    }
}
