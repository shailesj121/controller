package com.example

import com.example.model.CustomLayoutConfig
import com.example.model.ElementLayoutConfig
import com.example.model.GamepadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLayoutConfigTest {

    @Test
    fun testDefaultCustomLayoutConfig() {
        val config = CustomLayoutConfig()
        assertEquals(1.0f, config.actionButtons.scale, 0.001f)
        assertEquals(0.0f, config.actionButtons.offsetX, 0.001f)
        assertEquals(0.0f, config.actionButtons.offsetY, 0.001f)
        assertTrue(config.touchpad.visible)
    }

    @Test
    fun testJsonSerializationAndDeserialization() {
        val original = CustomLayoutConfig(
            actionButtons = ElementLayoutConfig(offsetX = 15f, offsetY = -20f, scale = 1.3f, visible = true),
            touchpad = ElementLayoutConfig(offsetX = 0f, offsetY = 10f, scale = 0.9f, visible = false)
        )

        val jsonString = original.toJsonString()
        val restored = CustomLayoutConfig.fromJsonString(jsonString)

        assertEquals(15f, restored.actionButtons.offsetX, 0.001f)
        assertEquals(-20f, restored.actionButtons.offsetY, 0.001f)
        assertEquals(1.3f, restored.actionButtons.scale, 0.001f)
        assertTrue(restored.actionButtons.visible)

        assertEquals(0.9f, restored.touchpad.scale, 0.001f)
        assertFalse(restored.touchpad.visible)
    }

    @Test
    fun testUpdateElement() {
        val config = CustomLayoutConfig()
        val updated = config.updateElement(
            CustomLayoutConfig.KEY_LEFT_STICK,
            ElementLayoutConfig(offsetX = 50f, offsetY = 30f, scale = 1.25f)
        )

        assertEquals(50f, updated.leftStick.offsetX, 0.001f)
        assertEquals(30f, updated.leftStick.offsetY, 0.001f)
        assertEquals(1.25f, updated.leftStick.scale, 0.001f)
    }

    @Test
    fun testGamepadStateWithTouchpad() {
        val state = GamepadState(
            btnTouchpad = true,
            touchpadX = 0.45f,
            touchpadY = -0.65f
        )

        val mask = state.toBitmask()
        assertTrue("Bit 18 should be set for btnTouchpad", (mask and (1 shl 18)) != 0)

        val packet = state.toPacketString()
        val parsed = GamepadState.fromPacketString(packet)

        assertTrue(parsed != null)
        assertTrue(parsed!!.btnTouchpad)
        assertEquals(0.45f, parsed.touchpadX, 0.05f)
        assertEquals(-0.65f, parsed.touchpadY, 0.05f)
    }
}
