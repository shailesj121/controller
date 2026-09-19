package com.example.games

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadState
import com.example.ui.theme.ButtonAColor
import com.example.ui.theme.ButtonBColor
import com.example.ui.theme.ButtonXColor
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividIndigo
import kotlin.random.Random

data class TrafficCar(var x: Float, var y: Float, val speed: Float, val color: Color)
data class BoostPad(var x: Float, var y: Float)

@Composable
fun ApexRacerGame(
    gamepadState: GamepadState,
    onTriggerRumble: (durationMs: Long, intensity: Float) -> Unit
) {
    var carX by remember { mutableFloatStateOf(0.5f) }
    var currentSpeedKmh by remember { mutableFloatStateOf(0f) }
    var distanceMeters by remember { mutableFloatStateOf(0f) }
    var lapSeconds by remember { mutableFloatStateOf(0f) }
    var nitroFuel by remember { mutableFloatStateOf(100f) }
    var isGameOver by remember { mutableStateOf(false) }

    val rivals = remember { mutableStateListOf<TrafficCar>() }
    val boostPads = remember { mutableStateListOf<BoostPad>() }
    var roadOffset by remember { mutableFloatStateOf(0f) }

    var lastSpawnTime by remember { mutableStateOf(0L) }
    var lastPadSpawnTime by remember { mutableStateOf(0L) }

    fun restartRace() {
        carX = 0.5f
        currentSpeedKmh = 0f
        distanceMeters = 0f
        lapSeconds = 0f
        nitroFuel = 100f
        isGameOver = false
        rivals.clear()
        boostPads.clear()
    }

    LaunchedEffect(isGameOver) {
        var lastTime = System.currentTimeMillis()

        while (!isGameOver) {
            withInfiniteAnimationFrameMillis {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTime).coerceIn(8, 33) / 1000f)
                lastTime = now

                lapSeconds += dt

                // 1. Steering from Touchpad, Left Stick OR Gyro Tilt Roll
                var steerInput = if (gamepadState.touchpadX != 0f) gamepadState.touchpadX.coerceIn(-1f, 1f) else gamepadState.leftStickX
                if (gamepadState.dpadLeft) steerInput = -1f
                if (gamepadState.dpadRight) steerInput = 1f

                // If phone is tilted in gyro mode, blend in roll angle
                if (kotlin.math.abs(gamepadState.roll) > 5f) {
                    steerInput = (gamepadState.roll / 35f).coerceIn(-1f, 1f)
                }

                // 2. Acceleration / Gas & Brake
                val isGas = gamepadState.btnA || gamepadState.btnR2 || gamepadState.rightTrigger > 0.2f
                val isBrake = gamepadState.btnB || gamepadState.btnL2 || gamepadState.leftTrigger > 0.2f
                val isNitro = (gamepadState.btnX || gamepadState.btnTurbo) && nitroFuel > 5f

                val maxSpeed = if (isNitro) 260f else 200f
                val accelRate = if (isNitro) 110f else 65f

                if (isNitro) {
                    nitroFuel = (nitroFuel - 40f * dt).coerceAtLeast(0f)
                } else {
                    nitroFuel = (nitroFuel + 8f * dt).coerceAtMost(100f)
                }

                if (isGas) {
                    currentSpeedKmh = (currentSpeedKmh + accelRate * dt).coerceAtMost(maxSpeed)
                } else if (isBrake) {
                    currentSpeedKmh = (currentSpeedKmh - 120f * dt).coerceAtLeast(0f)
                } else {
                    // Friction deceleration
                    currentSpeedKmh = (currentSpeedKmh - 30f * dt).coerceAtLeast(0f)
                }

                // Steer speed depends on car moving
                val speedRatio = currentSpeedKmh / 200f
                carX = (carX + steerInput * 0.75f * speedRatio * dt).coerceIn(0.18f, 0.82f)

                // Road scroll
                roadOffset += currentSpeedKmh * dt * 0.05f
                if (roadOffset > 100f) roadOffset = 0f
                distanceMeters += currentSpeedKmh * (dt / 3.6f)

                // 3. Spawn rival cars
                if (now - lastSpawnTime > 1600L && currentSpeedKmh > 30f) {
                    lastSpawnTime = now
                    val colors = listOf(CoralRed, VividIndigo, EmeraldGreen, Color(0xFFFFD600))
                    val spawnLanes = listOf(0.28f, 0.42f, 0.58f, 0.72f)
                    val chosenLane = spawnLanes.random()
                    rivals.add(TrafficCar(chosenLane, -0.1f, Random.nextFloat() * 40f + 70f, colors.random()))
                }

                // 4. Spawn Nitro boost pads
                if (now - lastPadSpawnTime > 4000L && currentSpeedKmh > 40f) {
                    lastPadSpawnTime = now
                    val lanes = listOf(0.32f, 0.50f, 0.68f)
                    boostPads.add(BoostPad(lanes.random(), -0.1f))
                }

                // 5. Update Rivals & Collisions
                val rivalIter = rivals.iterator()
                while (rivalIter.hasNext()) {
                    val rival = rivalIter.next()
                    // Relative speed
                    val relativeSpeed = (currentSpeedKmh - rival.speed) / 100f
                    rival.y += relativeSpeed * dt * 0.8f

                    // Collision check
                    val dx = kotlin.math.abs(rival.x - carX)
                    val dy = kotlin.math.abs(rival.y - 0.80f)
                    if (dx < 0.08f && dy < 0.08f) {
                        // Crash!
                        onTriggerRumble(200L, 0.85f)
                        currentSpeedKmh = (currentSpeedKmh * 0.4f).coerceAtLeast(10f)
                        rivalIter.remove()
                    } else if (rival.y > 1.2f || rival.y < -0.3f) {
                        rivalIter.remove()
                    }
                }

                // 6. Update Boost Pads
                val padIter = boostPads.iterator()
                while (padIter.hasNext()) {
                    val pad = padIter.next()
                    pad.y += (currentSpeedKmh / 100f) * dt * 0.8f
                    val dx = kotlin.math.abs(pad.x - carX)
                    val dy = kotlin.math.abs(pad.y - 0.80f)
                    if (dx < 0.08f && dy < 0.06f) {
                        // Collected Boost!
                        currentSpeedKmh = (currentSpeedKmh + 50f).coerceAtMost(260f)
                        nitroFuel = 100f
                        onTriggerRumble(100L, 0.5f)
                        padIter.remove()
                    } else if (pad.y > 1.1f) {
                        padIter.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1523))
    ) {
        // Track Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val roadLeft = w * 0.20f
            val roadRight = w * 0.80f
            val roadWidth = roadRight - roadLeft

            // Draw Asphalt Road
            drawRect(
                color = Color(0xFF1B2232),
                topLeft = Offset(roadLeft, 0f),
                size = Size(roadWidth, h)
            )

            // Red/White curb barriers
            val curbWidth = 14.dp.toPx()
            val segmentH = 40.dp.toPx()
            val offsetPixel = (roadOffset * 10f) % segmentH

            for (i in -1..((h / segmentH).toInt() + 1)) {
                val y = i * segmentH + offsetPixel
                val isRed = (i % 2 == 0)
                val curbColor = if (isRed) CoralRed else Color.White
                // Left curb
                drawRect(curbColor, Offset(roadLeft - curbWidth, y), Size(curbWidth, segmentH))
                // Right curb
                drawRect(curbColor, Offset(roadRight, y), Size(curbWidth, segmentH))
            }

            // Lane Dividers (Dashed yellow/white lines)
            val laneCount = 3
            for (lane in 1..laneCount) {
                val laneX = roadLeft + (roadWidth / (laneCount + 1)) * lane
                val dashH = 30.dp.toPx()
                val gapH = 25.dp.toPx()
                val totalH = dashH + gapH
                val dashOffset = (roadOffset * 15f) % totalH

                var dy = -totalH + dashOffset
                while (dy < h) {
                    drawRect(
                        color = Color(0xFF94A3B8).copy(alpha = 0.6f),
                        topLeft = Offset(laneX - 2.dp.toPx(), dy),
                        size = Size(4.dp.toPx(), dashH)
                    )
                    dy += totalH
                }
            }

            // Draw Boost Pads
            boostPads.forEach { pad ->
                val px = pad.x * w
                val py = pad.y * h
                drawCircle(
                    brush = Brush.radialGradient(listOf(ElectricCyan, Color.Transparent)),
                    radius = 20.dp.toPx(),
                    center = Offset(px, py)
                )
                drawRoundRect(
                    color = ElectricCyan,
                    topLeft = Offset(px - 14.dp.toPx(), py - 18.dp.toPx()),
                    size = Size(28.dp.toPx(), 36.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Draw Rivals
            rivals.forEach { rival ->
                val rx = rival.x * w
                val ry = rival.y * h
                drawCar(this, rx, ry, rival.color, isPlayer = false)
            }

            // Draw Player Sports Car
            val px = carX * w
            val py = 0.80f * h
            drawCar(this, px, py, ElectricCyan, isPlayer = true)
        }

        // Dashboard Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speedometer Gauge
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = "Speed", tint = ElectricCyan)
                        Column {
                            Text(
                                "${currentSpeedKmh.toInt()} KM/H",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentSpeedKmh > 210) CoralRed else ElectricCyan
                            )
                            Text("SPEED", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }

                // Distance & Lap Time
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("${(distanceMeters / 1000f).format(2)} KM", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("NITRO: ${nitroFuel.toInt()}%", fontSize = 11.sp, color = if (nitroFuel > 20) EmeraldGreen else CoralRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🕹️ Steer: Left Stick / Tilt", fontSize = 11.sp, color = TextSecondary)
                Text("🅰️ Gas (R2)", fontSize = 11.sp, color = ButtonAColor)
                Text("🅱️ Brake (L2)", fontSize = 11.sp, color = ButtonBColor)
                Text("🅧 NOS Boost", fontSize = 11.sp, color = ButtonXColor)
            }
        }
    }
}

private fun drawCar(scope: androidx.compose.ui.graphics.drawscope.DrawScope, x: Float, y: Float, bodyColor: Color, isPlayer: Boolean) = with(scope) {
    val carW = 28.dp.toPx()
    val carH = 50.dp.toPx()

    // Tires
    val tireW = 5.dp.toPx()
    val tireH = 12.dp.toPx()
    val tireColor = Color(0xFF1E293B)
    drawRect(tireColor, Offset(x - carW / 2f - tireW, y - carH * 0.35f), Size(tireW, tireH))
    drawRect(tireColor, Offset(x + carW / 2f, y - carH * 0.35f), Size(tireW, tireH))
    drawRect(tireColor, Offset(x - carW / 2f - tireW, y + carH * 0.15f), Size(tireW, tireH))
    drawRect(tireColor, Offset(x + carW / 2f, y + carH * 0.15f), Size(tireW, tireH))

    // Car Body
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(x - carW / 2f, y - carH / 2f),
        size = Size(carW, carH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
    )

    // Windshield
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(x - carW * 0.35f, y - carH * 0.25f),
        size = Size(carW * 0.7f, carH * 0.35f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )

    // Headlights
    drawCircle(if (isPlayer) ElectricCyan else Color.White, radius = 3.dp.toPx(), center = Offset(x - carW * 0.3f, y - carH / 2f + 2.dp.toPx()))
    drawCircle(if (isPlayer) ElectricCyan else Color.White, radius = 3.dp.toPx(), center = Offset(x + carW * 0.3f, y - carH / 2f + 2.dp.toPx()))
}

private fun Float.format(digits: Int) = "%.${digits}f".format(java.util.Locale.US, this)
