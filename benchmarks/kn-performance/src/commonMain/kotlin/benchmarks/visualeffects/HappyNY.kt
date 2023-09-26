package benchmarks.visualeffects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

import kotlin.math.*
import kotlin.random.Random

const val snowCount = 80
const val starCount = 60
const val rocketPartsCount = 30

data class SnowFlake(
    var x: Dp,
    var y: Dp,
    val scale: Float,
    var v: Double,
    var alpha: Float,
    var angle: Float,
    var rotate: Int,
    var phase: Double
)

data class Star(val x: Dp, val y: Dp, val color: Color, val size: Dp)

val random = Random(123)

fun random(): Float = random.nextFloat()

class DoubleRocket(val particle: Particle) {
    private val STATE_ROCKET = 0
    private val STATE_SMALL_ROCKETS = 1
    var state = STATE_ROCKET
    var rockets: Array<Rocket> = emptyArray()
    private fun checkState(time: Long) {
        if (particle.vy > -3.0 && state == STATE_ROCKET) {
            explode(time)
        }
        if (state == STATE_SMALL_ROCKETS) {
            var done = true
            rockets.forEach {
                if (!it.exploded) {
                    it.checkExplode(time)
                }
                if (!it.checkDone()) {
                    done = false
                }
            }
            if (done) {
                reset()
            }
        }
    }

    private fun reset() {
        state = STATE_ROCKET
        particle.x = 0.0
        particle.y = 1000.0
        particle.vx = 2.1
        particle.vy = -12.5
    }

    private fun explode(time: Long) {
        val colors = arrayOf(Color(0xff, 0, 0), Color(192, 255, 192), Color(192, 212, 255))
        rockets = Array(7) {
            val v = 1.2f + 1.0 * random()
            val angle = 2 * PI * random()
            Rocket(
                Particle(
                    particle.x,
                    particle.y,
                    v * sin(angle) + particle.vx,
                    v * cos(angle) + particle.vy - 0.5f,
                    colors[it % colors.size]
                ), colors[it % colors.size], time
            )
        }
        state = STATE_SMALL_ROCKETS
    }

    fun move(time: Long, prevTime: Long) {
        if (rocket.state == rocket.STATE_ROCKET) {
            rocket.particle.move(time, prevTime)
            rocket.particle.gravity(time, prevTime)
        } else {
            rocket.rockets.forEach {
                it.move(time, prevTime)
            }
        }
        rocket.checkState(time)
    }

    @Composable
    fun draw() {
        if (state == rocket.STATE_ROCKET) {
            particle.draw()
        } else {
            rockets.forEach {
                it.draw()
            }
        }
    }

}

class Rocket(val particle: Particle, val color: Color, val startTime: Long = 0) {
    var exploded = false
    var parts: Array<Particle> = emptyArray()

    fun checkExplode(time: Long) {
        if (time - startTime > 1200000000) {
            explode()
        }
    }

    private fun explode() {
        parts = Array(rocketPartsCount) {
            val v = 0.5f + 1.5 * random()
            val angle = 2 * PI * random()
            Particle(particle.x, particle.y, v * sin(angle) + particle.vx, v * cos(angle) + particle.vy, color, 1)
        }
        exploded = true
    }

    fun checkDone(): Boolean {
        if (!exploded) return false
        parts.forEach {
            if (it.y < 800) return false
        }
        return true
    }

    fun move(time: Long, prevTime: Long) {
        if (!exploded) {
            particle.move(time, prevTime)
            particle.gravity(time, prevTime)
            checkExplode(time)
        } else {
            parts.forEach {
                it.move(time, prevTime)
                it.gravity(time, prevTime)
            }
        }
    }

    @Composable
    fun draw() {
        if (!exploded) {
            particle.draw()
        } else {
            parts.forEach {
                it.draw()
            }
        }
    }
}

class Particle(var x: Double, var y: Double, var vx: Double, var vy: Double, val color: Color, val type: Int = 0) {
    fun move(time: Long, prevTime: Long) {
        x = (x + vx * (time - prevTime) / 30000000)
        y = (y + vy * (time - prevTime) / 30000000)
    }

    fun gravity(time: Long, prevTime: Long) {
        vy = vy + 1.0f * (time - prevTime) / 300000000
    }

    @Composable
    fun draw() {
        val alphaFactor = if (type == 0) 1.0f else 1 / (1 + abs(vy / 5)).toFloat()
        Box(Modifier.size(5.dp).offset(x.dp, y.dp).alpha(alphaFactor).clip(CircleShape).background(color))
        for (i in 1..5) {
            Box(
                Modifier.size(4.dp).offset((x - vx / 2 * i).dp, (y - vy / 2 * i).dp)
                    .alpha(alphaFactor * (1 - 0.18f * i)).clip(CircleShape).background(color)
            )
        }
    }
}

val rocket = DoubleRocket(Particle(0.0, 1000.0, 2.1, -12.5, Color.White))

fun prepareStarsAndSnowFlakes(stars: SnapshotStateList<Star>, snowFlakes: SnapshotStateList<SnowFlake>, width: Int, height: Int) {
    for (i in 0..snowCount) {
        snowFlakes.add(
            SnowFlake(
                (50 + (width - 50) * random()).dp,
                (height * random()).dp,
                0.1f + 0.2f * random(),
                1.5 + 3 * random(),
                (0.4f + 0.4 * random()).toFloat(),
                60 * random(),
                Random.nextInt(1, 5) - 3,
                random() * 2 * PI
            )
        )
    }
    val colors = arrayOf(Color.Red, Color.Yellow, Color.Green, Color.Yellow, Color.Cyan, Color.Magenta, Color.White)
    for (i in 0..starCount) {
        stars.add(
            Star(
                (width * random()).dp,
                (height * random()).dp,
                colors[Random.nextInt(colors.size)],
                (3 + 5 * random()).dp
            )
        )
    }
}

@Composable
fun NYContent(width: Int, height: Int) {
    var time by remember { mutableStateOf(0L) }
    val startTime = remember { 0L }
    var prevTime by remember { mutableStateOf(0L) }
    val snowFlakes = remember { mutableStateListOf<SnowFlake>() }
    val stars = remember { mutableStateListOf<Star>() }
    var flickering2 by remember { mutableStateOf(true) }
    remember { prepareStarsAndSnowFlakes(stars, snowFlakes, width, height) }

    Surface(
        modifier = Modifier.fillMaxSize().padding(5.dp).shadow(3.dp, RoundedCornerShape(20.dp)),
        color = Color.Black,
        shape = RoundedCornerShape(20.dp)
    ) {

        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos {
                    prevTime = time
                    time = it
                }
            }
        }

        if (flickering2) {
            if (time - startTime > 15500000000) { //note, that startTime has been updated above
                flickering2 = false
            }
        }

        rocket.move(time, prevTime)

        Box(Modifier.fillMaxSize()) {

            snow(time, prevTime, snowFlakes, startTime, height)

            starrySky(stars)

            rocket.draw()

        }
    }
}


@Composable
fun starrySky(stars: SnapshotStateList<Star>) {
    stars.forEach {
        star(it.x, it.y, it.color, size = it.size)
    }
}

@Composable
fun star(x: Dp, y: Dp, color: Color = Color.White, size: Dp) {
    Box(Modifier.offset(x, y).scale(1.0f, 0.2f).rotate(45f).size(size).background(color))
    Box(Modifier.offset(x, y).scale(0.2f, 1.0f).rotate(45f).size(size).background(color))
}

@Composable
fun snow(time: Long, prevTime: Long, snowFlakes: SnapshotStateList<SnowFlake>, startTime: Long, height: Int) {
    val deltaAngle = (time - startTime) / 100000000
    with(LocalDensity.current) {
        snowFlakes.forEach {
            var y = it.y + ((it.v * (time - prevTime)) / 300000000).dp
            if (y > (height + 20).dp) {
                y = -20.dp
            }
            it.y = y
            val x = it.x + (15 * sin(time.toDouble() / 3000000000 + it.phase)).dp
            snowFlake(Modifier.offset(x, y).scale(it.scale).rotate(it.angle + deltaAngle * it.rotate), it.alpha)
        }
    }
}

@Composable
fun snowFlake(modifier: Modifier, alpha: Float = 0.8f) {
    Box(modifier) {
        snowFlakeInt(0, 0f, 30.dp, 0.dp, alpha)
        snowFlakeInt(0, 60f, 15.dp, 25.dp, alpha)
        snowFlakeInt(0, 120f, -15.dp, 25.dp, alpha)
        snowFlakeInt(0, 180f, -30.dp, 0.dp, alpha)
        snowFlakeInt(0, 240f, -15.dp, -25.dp, alpha)
        snowFlakeInt(0, 300f, 15.dp, -25.dp, alpha)
    }

}

@Composable
fun snowFlakeInt(level: Int, angle: Float, shiftX: Dp, shiftY: Dp, alpha: Float) {
    if (level > 3) return
    Box(
        Modifier.offset(shiftX, shiftY).rotate(angle).width(100.dp).height(10.dp).scale(0.6f).alpha(1f)
            .background(Color.White.copy(alpha = alpha))
    ) {
        snowFlakeInt(level + 1, 30f, 12.dp, 20.dp, alpha * 0.8f)
        snowFlakeInt(level + 1, -30f, 12.dp, -20.dp, alpha * 0.8f)
    }
}

