package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.GamepadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Game Controller", appName)
  }

  @Test
  fun `verify gamepad state serialization roundtrip`() {
    val original = GamepadState(
      seq = 42,
      btnA = true,
      btnB = false,
      btnX = true,
      btnY = false,
      dpadUp = true,
      leftStickX = 0.75f,
      leftStickY = -0.5f,
      rightStickX = -0.2f,
      rightStickY = 0.9f,
      leftTrigger = 0.8f,
      rightTrigger = 1.0f
    )

    val packetString = original.toPacketString()
    val restored = GamepadState.fromPacketString(packetString)

    assertTrue(restored != null)
    assertEquals(original.seq, restored?.seq)
    assertEquals(original.btnA, restored?.btnA)
    assertEquals(original.btnX, restored?.btnX)
    assertEquals(original.dpadUp, restored?.dpadUp)
    assertEquals(original.leftStickX, restored?.leftStickX ?: 0f, 0.01f)
    assertEquals(original.leftStickY, restored?.leftStickY ?: 0f, 0.01f)
  }
}
