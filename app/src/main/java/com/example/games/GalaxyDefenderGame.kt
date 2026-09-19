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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GamepadState
import com.example.ui.theme.AmberOrange
import com.example.ui.theme.ButtonAColor
import com.example.ui.theme.ButtonBColor
import com.example.ui.theme.ButtonXColor
import com.example.ui.theme.ButtonYColor
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividIndigo
import kotlin.math.sin
import kotlin.random.Random

data class Laser(var x: Float, var y: Float, val isPlayer: Boolean)
data class Enemy(var x: Float, var y: Float, val speed: Float, var hp: Int, val maxHp: Int, val color: Color)
data class ExplosionParticle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val color: Color)
data class Star(val x: Float, var y: Float, val speed: Float, val size: Float, val alpha: Float)

@Composable
fun GalaxyDefenderGame(
    gamepadState: GamepadState,
    onTriggerRumble: (durationMs: Long, intensity: Float) -> Unit
) {
    var playerX by remember { mutableFloatStateOf(0.5f) }
    var playerY by remember { mutableFloatStateOf(0.85f) }
    var playerHealth by remember { mutableFloatStateOf(100f) }
    var shieldEnergy by remember { mutableFloatStateOf(100f) }
    var score by remember { mutableIntStateOf(0) }
    var wave by remember { mutableIntStateOf(1) }
    var isGameOver by remember { mutableStateOf(false) }

    val lasers = remember { mutableStateListOf<Laser>() }
    val enemies = remember { mutableStateListOf<Enemy>() }
    val particles = remember { mutableStateListOf<ExplosionParticle>() }
    val stars = remember {
        mutableStateListOf<Star>().apply {
            repeat(50) {
                add(Star(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 1.5f + 0.5f, Random.nextFloat() * 2.5f + 1f, Random.nextFloat() * 0.8f + 0.2f))
            }
        }
    }

    var lastShotTime by remember { mutableStateOf(0L) }
    var lastBombTime by remember { mutableStateOf(0L) }
    var lastEnemySpawnTime by remember { mutableStateOf(0L) }

    fun restartGame() {
        playerX = 0.5f
        playerY = 0.85f
        playerHealth = 100f
        shieldEnergy = 100f
        score = 0
        wave = 1
        isGameOver = false
        lasers.clear()
        enemies.clear()
        particles.clear()
    }

    // Game loop
    LaunchedEffect(isGameOver) {
        var lastFrameTime = System.currentTimeMillis()

        while (!isGameOver) {
            withInfiniteAnimationFrameMillis { frameTime ->
                val now = System.currentTimeMillis()
                val dt = ((now - lastFrameTime).coerceIn(8, 33) / 1000f)
                lastFrameTime = now

                // 1. Move Player from Controller Inputs (Stick or DPad)
                var moveX = gamepadState.leftStickX
                var moveY = gamepadState.leftStickY
                if (gamepadState.dpadLeft) moveX = -1f
                if (gamepadState.dpadRight) moveX = 1f
                if (gamepadState.dpadUp) moveY = -1f
                if (gamepadState.dpadDown) moveY = 1f

                val speed = 0.6f * dt
                playerX = (playerX + moveX * speed).coerceIn(0.06f, 0.94f)
                playerY = (playerY + moveY * speed).coerceIn(0.10f, 0.94f)

                // 2. Stars scrolling
                stars.forEach { star ->
                    star.y += star.speed * dt * 0.2f
                    if (star.y > 1f) star.y = 0f
                }

                // 3. Fire Laser (Button A or R2 or Turbo)
                val isFiring = gamepadState.btnA || gamepadState.btnR2 || gamepadState.btnTurbo
                val fireInterval = if (gamepadState.btnTurbo) 100L else 180L
                if (isFiring && now - lastShotTime > fireInterval) {
                    lasers.add(Laser(playerX - 0.025f, playerY - 0.04f, true))
                    lasers.add(Laser(playerX + 0.025f, playerY - 0.04f, true))
                    lastShotTime = now
                }

                // 4. Shield (Button B)
                val isShieldActive = gamepadState.btnB && shieldEnergy > 5f
                if (isShieldActive) {
                    shieldEnergy = (shieldEnergy - 35f * dt).coerceAtLeast(0f)
                } else {
                    shieldEnergy = (shieldEnergy + 15f * dt).coerceAtMost(100f)
                }

                // 5. Bomb / Special (Button X or Y)
                if ((gamepadState.btnX || gamepadState.btnY) && now - lastBombTime > 2500L) {
                    lastBombTime = now
                    onTriggerRumble(250L, 1.0f)
                    // Clear all enemies on screen and create big particle field
                    enemies.forEach { enemy ->
                        repeat(12) {
                            val angle = Random.nextFloat() * 6.28f
                            val vel = Random.nextFloat() * 0.4f + 0.1f
                            particles.add(ExplosionParticle(enemy.x, enemy.y, kotlin.math.cos(angle) * vel, kotlin.math.sin(angle) * vel, 1f, enemy.color))
                        }
                    }
                    score += enemies.size * 50
                    enemies.clear()
                }

                // 6. Spawn Enemies
                val spawnInterval = (1400 - (wave * 100)).coerceAtLeast(400)
                if (now - lastEnemySpawnTime > spawnInterval) {
                    lastEnemySpawnTime = now
                    val enemyType = Random.nextInt(3)
                    val (color, hp, spd) = when (enemyType) {
                        0 -> Triple(CoralRed, 1, 0.25f + wave * 0.02f)
                        1 -> Triple(VividIndigo, 2, 0.18f + wave * 0.015f)
                        else -> Triple(ButtonYColor, 3, 0.12f + wave * 0.01f)
                    }
                    enemies.add(Enemy(Random.nextFloat() * 0.8f + 0.1f, 0.02f, spd, hp, hp, color))
                }

                // 7. Update Lasers
                val laserIterator = lasers.iterator()
                while (laserIterator.hasNext()) {
                    val laser = laserIterator.next()
                    laser.y -= 0.9f * dt
                    if (laser.y < -0.05f) {
                        laserIterator.remove()
                    }
                }

                // 8. Update Enemies & Collisions
                val enemyIterator = enemies.iterator()
                while (enemyIterator.hasNext()) {
                    val enemy = enemyIterator.next()
                    enemy.y += enemy.speed * dt

                    // Enemy vs Lasers
                    val hitLaser = lasers.firstOrNull { laser ->
                        val dx = laser.x - enemy.x
                        val dy = laser.y - enemy.y
                        kotlin.math.sqrt(dx * dx + dy * dy) < 0.05f
                    }
                    if (hitLaser != null) {
                        lasers.remove(hitLaser)
                        enemy.hp--
                        if (enemy.hp <= 0) {
                            score += enemy.maxHp * 20
                            if (score / 300 + 1 > wave) {
                                wave++
                            }
                            // Spawn particles
                            repeat(8) {
                                val angle = Random.nextFloat() * 6.28f
                                val vel = Random.nextFloat() * 0.3f + 0.05f
                                particles.add(ExplosionParticle(enemy.x, enemy.y, kotlin.math.cos(angle) * vel, kotlin.math.sin(angle) * vel, 0.8f, enemy.color))
                            }
                            enemyIterator.remove()
                            continue
                        }
                    }

                    // Enemy vs Player
                    val distToPlayer = kotlin.math.sqrt((enemy.x - playerX) * (enemy.x - playerX) + (enemy.y - playerY) * (enemy.y - playerY))
                    if (distToPlayer < 0.06f) {
                        enemyIterator.remove()
                        if (isShieldActive) {
                            shieldEnergy = (shieldEnergy - 25f).coerceAtLeast(0f)
                            onTriggerRumble(100L, 0.4f)
                        } else {
                            playerHealth -= 25f
                            onTriggerRumble(250L, 0.9f)
                            if (playerHealth <= 0f) {
                                isGameOver = true
                            }
                        }
                    } else if (enemy.y > 1.05f) {
                        enemyIterator.remove()
                    }
                }

                // 9. Update particles
                val particleIterator = particles.iterator()
                while (particleIterator.hasNext()) {
                    val p = particleIterator.next()
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.life -= 2f * dt
                    if (p.life <= 0f) {
                        particleIterator.remove()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Space Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Draw Stars
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = star.size,
                    center = Offset(star.x * w, star.y * h)
                )
            }

            // Draw Lasers
            lasers.forEach { laser ->
                drawRoundRect(
                    color = ElectricCyan,
                    topLeft = Offset(laser.x * w - 3.dp.toPx(), laser.y * h - 8.dp.toPx()),
                    size = Size(6.dp.toPx(), 16.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }

            // Draw Enemies
            enemies.forEach { enemy ->
                val ex = enemy.x * w
                val ey = enemy.y * h
                val eSize = 22.dp.toPx()

                val path = Path().apply {
                    moveTo(ex, ey + eSize)
                    lineTo(ex - eSize * 0.8f, ey - eSize * 0.6f)
                    lineTo(ex, ey - eSize * 0.2f)
                    lineTo(ex + eSize * 0.8f, ey - eSize * 0.6f)
                    close()
                }
                drawPath(path, color = enemy.color, style = Fill)
                drawPath(path, color = Color.White.copy(alpha = 0.6f), style = Stroke(width = 1.5.dp.toPx()))
            }

            // Draw Particles
            particles.forEach { p ->
                drawCircle(
                    color = p.color.copy(alpha = p.life.coerceIn(0f, 1f)),
                    radius = 3.dp.toPx() * p.life,
                    center = Offset(p.x * w, p.y * h)
                )
            }

            // Draw Player Spaceship
            val px = playerX * w
            val py = playerY * h
            val sSize = 26.dp.toPx()

            // Engine thruster flame
            drawCircle(
                color = AmberOrange.copy(alpha = (Random.nextFloat() * 0.5f + 0.5f)),
                radius = 7.dp.toPx(),
                center = Offset(px, py + sSize * 0.8f)
            )

            // Ship Body
            val shipPath = Path().apply {
                moveTo(px, py - sSize)
                lineTo(px + sSize * 0.7f, py + sSize * 0.7f)
                lineTo(px, py + sSize * 0.3f)
                lineTo(px - sSize * 0.7f, py + sSize * 0.7f)
                close()
            }
            drawPath(
                shipPath,
                brush = Brush.verticalGradient(
                    listOf(ElectricCyan, VividIndigo),
                    startY = py - sSize,
                    endY = py + sSize
                )
            )
            drawPath(shipPath, color = Color.White, style = Stroke(width = 1.8.dp.toPx()))

            // Energy Shield (if holding button B)
            val isShieldActive = gamepadState.btnB && shieldEnergy > 5f
            if (isShieldActive) {
                drawCircle(
                    color = ElectricCyan.copy(alpha = 0.3f),
                    radius = sSize * 1.4f,
                    center = Offset(px, py)
                )
                drawCircle(
                    color = ElectricCyan,
                    radius = sSize * 1.4f,
                    center = Offset(px, py),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // HUD Overlay
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
                Column {
                    Text("SCORE: $score", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Text("WAVE $wave", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Health bar
                    Column(horizontalAlignment = Alignment.End) {
                        Text("HULL: ${playerHealth.toInt()}%", fontSize = 11.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { playerHealth / 100f },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (playerHealth > 30) EmeraldGreen else CoralRed,
                            trackColor = DarkSurfaceVariant
                        )
                    }

                    // Shield bar
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SHIELD: ${shieldEnergy.toInt()}%", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { shieldEnergy / 100f },
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = ElectricCyan,
                            trackColor = DarkSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            // Controller guide overlay
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🕹️ Move: Left Stick", fontSize = 11.sp, color = TextSecondary)
                Text("🅰️ Fire Laser", fontSize = 11.sp, color = ButtonAColor)
                Text("🅱️ Deflector Shield", fontSize = 11.sp, color = ButtonBColor)
                Text("🅧 Super Bomb", fontSize = 11.sp, color = ButtonXColor)
            }
        }

        // Game Over Dialog
        if (isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(320.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("MISSION FAILED", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CoralRed)
                        Text("Final Score: $score", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Wave Reached: $wave", fontSize = 14.sp, color = TextSecondary)

                        Button(
                            onClick = { restartGame() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = Color(0xFF00363D))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Again (or press START)", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
