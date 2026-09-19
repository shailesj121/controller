package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.ControllerLayout
import com.example.model.GamepadState
import com.example.ui.controller.ControllerScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun controller_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ControllerScreen(
          gamepadState = GamepadState(),
          layout = ControllerLayout.MODERN,
          isConnected = true,
          pingMs = 4,
          isHapticEnabled = true,
          isGyroEnabled = false,
          onStateUpdated = {},
          onOpenSettings = {},
          onSwitchToReceiver = {},
          onTriggerHaptic = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/controller.png")
  }
}
