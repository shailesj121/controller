package com.example.model

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Configuration for a single controllable element group on screen.
 * @param offsetX X offset in density-independent pixels (dp) from default position
 * @param offsetY Y offset in density-independent pixels (dp) from default position
 * @param scale Uniform scale factor (0.50f to 1.75f, default 1.0f)
 * @param visible Whether this element group is rendered on screen
 */
data class ElementLayoutConfig(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1.0f,
    val visible: Boolean = true
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("x", offsetX.toDouble())
            put("y", offsetY.toDouble())
            put("scale", scale.toDouble())
            put("visible", visible)
        }
    }

    companion object {
        fun fromJson(json: JSONObject?): ElementLayoutConfig {
            if (json == null) return ElementLayoutConfig()
            return ElementLayoutConfig(
                offsetX = json.optDouble("x", 0.0).toFloat(),
                offsetY = json.optDouble("y", 0.0).toFloat(),
                scale = json.optDouble("scale", 1.0).toFloat().coerceIn(0.5f, 1.75f),
                visible = json.optBoolean("visible", true)
            )
        }
    }
}

/**
 * Comprehensive customizable controller layout containing position and scale for all elements.
 */
data class CustomLayoutConfig(
    val leftStick: ElementLayoutConfig = ElementLayoutConfig(),
    val rightStick: ElementLayoutConfig = ElementLayoutConfig(),
    val dpad: ElementLayoutConfig = ElementLayoutConfig(),
    val actionButtons: ElementLayoutConfig = ElementLayoutConfig(),
    val shoulderLeft: ElementLayoutConfig = ElementLayoutConfig(),
    val shoulderRight: ElementLayoutConfig = ElementLayoutConfig(),
    val centerPills: ElementLayoutConfig = ElementLayoutConfig(),
    val touchpad: ElementLayoutConfig = ElementLayoutConfig(visible = true)
) {
    fun getElement(key: String): ElementLayoutConfig {
        return when (key) {
            KEY_LEFT_STICK -> leftStick
            KEY_RIGHT_STICK -> rightStick
            KEY_DPAD -> dpad
            KEY_ACTION_BUTTONS -> actionButtons
            KEY_SHOULDER_LEFT -> shoulderLeft
            KEY_SHOULDER_RIGHT -> shoulderRight
            KEY_CENTER_PILLS -> centerPills
            KEY_TOUCHPAD -> touchpad
            else -> ElementLayoutConfig()
        }
    }

    fun updateElement(key: String, config: ElementLayoutConfig): CustomLayoutConfig {
        return when (key) {
            KEY_LEFT_STICK -> copy(leftStick = config)
            KEY_RIGHT_STICK -> copy(rightStick = config)
            KEY_DPAD -> copy(dpad = config)
            KEY_ACTION_BUTTONS -> copy(actionButtons = config)
            KEY_SHOULDER_LEFT -> copy(shoulderLeft = config)
            KEY_SHOULDER_RIGHT -> copy(shoulderRight = config)
            KEY_CENTER_PILLS -> copy(centerPills = config)
            KEY_TOUCHPAD -> copy(touchpad = config)
            else -> this
        }
    }

    fun toJsonString(): String {
        return JSONObject().apply {
            put(KEY_LEFT_STICK, leftStick.toJson())
            put(KEY_RIGHT_STICK, rightStick.toJson())
            put(KEY_DPAD, dpad.toJson())
            put(KEY_ACTION_BUTTONS, actionButtons.toJson())
            put(KEY_SHOULDER_LEFT, shoulderLeft.toJson())
            put(KEY_SHOULDER_RIGHT, shoulderRight.toJson())
            put(KEY_CENTER_PILLS, centerPills.toJson())
            put(KEY_TOUCHPAD, touchpad.toJson())
        }.toString()
    }

    companion object {
        const val KEY_LEFT_STICK = "left_stick"
        const val KEY_RIGHT_STICK = "right_stick"
        const val KEY_DPAD = "dpad"
        const val KEY_ACTION_BUTTONS = "action_buttons"
        const val KEY_SHOULDER_LEFT = "shoulder_left"
        const val KEY_SHOULDER_RIGHT = "shoulder_right"
        const val KEY_CENTER_PILLS = "center_pills"
        const val KEY_TOUCHPAD = "touchpad"

        fun fromJsonString(jsonStr: String?): CustomLayoutConfig {
            if (jsonStr.isNullOrBlank()) return CustomLayoutConfig()
            return try {
                val obj = JSONObject(jsonStr)
                CustomLayoutConfig(
                    leftStick = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_LEFT_STICK)),
                    rightStick = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_RIGHT_STICK)),
                    dpad = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_DPAD)),
                    actionButtons = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_ACTION_BUTTONS)),
                    shoulderLeft = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_SHOULDER_LEFT)),
                    shoulderRight = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_SHOULDER_RIGHT)),
                    centerPills = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_CENTER_PILLS)),
                    touchpad = ElementLayoutConfig.fromJson(obj.optJSONObject(KEY_TOUCHPAD))
                )
            } catch (_: Exception) {
                CustomLayoutConfig()
            }
        }
    }
}

/**
 * Storage manager for persisting custom layout preferences.
 */
class CustomLayoutPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("controller_custom_layout_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PREF_KEY_CUSTOM_LAYOUT = "saved_custom_layout_json"
    }

    fun loadLayoutConfig(): CustomLayoutConfig {
        val saved = prefs.getString(PREF_KEY_CUSTOM_LAYOUT, null)
        return CustomLayoutConfig.fromJsonString(saved)
    }

    fun saveLayoutConfig(config: CustomLayoutConfig) {
        prefs.edit().putString(PREF_KEY_CUSTOM_LAYOUT, config.toJsonString()).apply()
    }

    fun resetLayoutConfig(): CustomLayoutConfig {
        prefs.edit().remove(PREF_KEY_CUSTOM_LAYOUT).apply()
        return CustomLayoutConfig()
    }
}
