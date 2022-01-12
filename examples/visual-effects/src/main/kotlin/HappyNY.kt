package org.jetbrains.compose.demo.visuals

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.lang.Math.random
import kotlin.math.*
import kotlin.random.Random

val width = 1200
val height = 800
val snowCount = 80
val starCount = 60
val rocketPartsCount = 30

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

val HNYString = "Happy New Year!"

class DoubleRocket(val particle: Particle) {
    val STATE_ROCKET = 0
    val STATE_SMALL_ROCKETS = 1
    var state = STATE_ROCKET
    var rockets: Array<Rocket> = emptyArray()
    fun checkState(time: Long) {
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

    fun reset() {
        if (particle.vx < 0) return //to stop drawing after the second rocket. This could be commented out
        state = STATE_ROCKET
        particle.x = if (particle.vx > 0) width - 0.0 else 0.0
        particle.y = 1000.0
        particle.vx = -1 * particle.vx
        particle.vy = -12.5
    }

    fun explode(time: Long) {
        val colors = arrayOf(Color(0xff, 0, 0), Color(192, 255, 192), Color(192, 212, 255))
        rockets = Array<Rocket>(7) {
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

    fun explode() {
        parts = Array<Particle>(rocketPartsCount) {
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


@Composable
fun NYWindow(onCloseRequest: () -> Unit) {
    val windowState = remember { WindowState(width = width.dp, height = height.dp) }
    Window(onCloseRequest = {}, undecorated = true, transparent = true, state = windowState) {
        NYContent()
    }
}

fun prepareStarsAndSnowFlakes(stars: SnapshotStateList<Star>, snowFlakes: SnapshotStateList<SnowFlake>) {
    for (i in 0..snowCount) {
        snowFlakes.add(
            SnowFlake(
                (50 + (width - 50) * random()).dp,
                (height * random()).dp,
                0.1f + 0.2f * random().toFloat(),
                1.5 + 3 * random(),
                (0.4f + 0.4 * random()).toFloat(),
                60 * random().toFloat(),
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun NYContent() {
    var time by remember { mutableStateOf(System.nanoTime()) }
    var started by remember { mutableStateOf(false) }
    var startTime = remember { System.nanoTime() }
    var prevTime by remember { mutableStateOf(System.nanoTime()) }
    val snowFlakes = remember { mutableStateListOf<SnowFlake>() }
    val stars = remember { mutableStateListOf<Star>() }
    var flickering2 by remember { mutableStateOf(true) }
    remember { prepareStarsAndSnowFlakes(stars, snowFlakes) }

    Surface(
        modifier = Modifier.fillMaxSize().padding(5.dp).shadow(3.dp, RoundedCornerShape(20.dp))
            .pointerMoveFilter(onMove = { false; },
                onEnter = { false; }, onExit = { false; }),
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

        if (!started) { //animation starts with delay, so there is some time to start recording
            if (time - startTime > 7000000000 && time - startTime < 7100000000) println("ready!")
            if (time - startTime > 10000000000) {
                startTime = time //restarting timer
                started = true
            }
        }

        if (flickering2) {
            if (time - startTime > 15500000000) { //note, that startTime has been updated above
                flickering2 = false
            }
        }
        if (started) {
            rocket.move(time, prevTime)
        }

        with(LocalDensity.current) {
            Box(Modifier.fillMaxSize()) {

                snow(time, prevTime, snowFlakes, startTime)

                starrySky(stars)

                Text(
                    "202",
                    Modifier.scale(10f).align(Alignment.Center).offset(-2.dp, 15.dp)
                        .alpha(if (flickering2) 0.8f else 1.0f),
                    color = Color.White
                )

                val alpha = if (flickering2) flickeringAlpha(time) else 1.0f
                Text(
                    "2",
                    Modifier.scale(10f).align(Alignment.Center).offset(14.dp, 15.dp).alpha(alpha),
                    color = Color.White
                )

                if (started) { //delay to be able start recording
                    //HNY
                    var i = 0
                    val angle = (HNYString.length / 2 * 5) * -1.0f
                    val color = colorHNY(time, startTime)
                    HNYString.forEach {
                        val alpha = alphaHNY(i, time, startTime)
                        Text(
                            it.toString(),
                            color = color,
                            modifier = Modifier.scale(5f).align(Alignment.Center).offset(0.dp, 85.dp)
                                .rotate((angle + 5.0f * i)).offset(0.dp, -90.dp).alpha(alpha)
                        )
                        i++
                    }

                    rocket.draw()
                }

                Text(
                    "Powered by Compose Multiplatform",
                    modifier = Modifier.align(Alignment.BottomEnd).offset(-20.dp, 0.dp),
                    color = Color.White
                )
            }
        }
    }
}

fun colorHNY(time: Long, startTime: Long): Color {
    val periodLength = 60
    val offset = ((time - startTime) / 80000000).toFloat() / periodLength
    val color1 = Color.Red
    val color2 = Color.Yellow
    val color3 = Color.Magenta
    if (offset < 1) return blend(color1, color2, offset)
    if (offset < 2) return blend(color2, color3, offset - 1)
    if (offset < 3) return blend(color3, color1, offset - 2)
    return color1
}

fun blend(color1: Color, color2: Color, fraction: Float): Color {
    if (fraction < 0) return color1
    if (fraction > 1) return color2
    return Color(
        color2.red * fraction + color1.red * (1 - fraction),
        color2.green * fraction + color1.green * (1 - fraction),
        color2.blue * fraction + color1.blue * (1 - fraction)
    )
}

fun alphaHNY(i: Int, time: Long, startTime: Long): Float {
    val period = period(time, startTime, 200) - i
    if (period < 0) return 0.0f
    if (period > 10) return 1.0f
    return 0.1f * period
}

fun period(time: Long, startTime: Long, periodLength: Int, speed: Int = 1): Int {
    val period = 200000000 / speed
    return (((time - startTime) / period) % periodLength).toInt()
}

fun flickeringAlpha(time: Long): Float {
    val time = (time / 10000000) % 100
    var result = 0.2f
    if (time > 75) {
        result = result + 0.6f * ((time - 75) % 3) / 3
    }
    return result
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
fun snow(time: Long, prevTime: Long, snowFlakes: SnapshotStateList<SnowFlake>, startTime: Long) {
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

