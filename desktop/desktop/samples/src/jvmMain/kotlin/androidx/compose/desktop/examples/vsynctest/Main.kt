/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.desktop.examples.vsynctest

import androidx.compose.desktop.LocalAppWindow
import androidx.compose.desktop.Window
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize

private val frameLogCount = 1000
private val red = Color(255, 128, 128)
private val cyan = Color(128, 255, 255)

fun main() {
    window()
    window()
}

fun window() {
    var t1 = Long.MAX_VALUE
    val frameDeltas = ArrayList<Long>(10000)
    var heuristicExpectedFrameTime = -1L

    fun logFrame() {
        val t2 = System.nanoTime()
        val dt = (t2 - t1).coerceAtLeast(0)
        frameDeltas.add(dt)
        t1 = t2

        if (heuristicExpectedFrameTime > 0 && dt > heuristicExpectedFrameTime * 1.5) {
            val dtMillis = dt / 1E6
            val expectedMillis = heuristicExpectedFrameTime / 1E6
            println("Too long frame %.2f (expected %.2f)".format(dtMillis, expectedMillis))
        }

        if (frameDeltas.size % frameLogCount == 0) {
            val fps = 1E9 / frameDeltas.average()

            // it is more precise than
            // window.window.graphicsConfiguration.device.displayMode.refreshRate
            // if vsync is supported
            heuristicExpectedFrameTime = frameDeltas.median()

            val actualFrameCount = frameDeltas.sum() / heuristicExpectedFrameTime
            val missedFrames = (actualFrameCount - frameDeltas.size).coerceAtLeast(0)
            val missedFrameCountPercent = 100.0 * missedFrames / frameDeltas.size
            println("FPS %.2f, missed frames %.2f%%".format(fps, missedFrameCountPercent))
            frameDeltas.clear()
        }
    }

    Window(size = IntSize(800, 200)) {
        val window = LocalAppWindow.current
        val width = (LocalDensity.current.density * window.window.width).toInt()
        val singleFrameMillis = remember {
            1000 / window.window.graphicsConfiguration.device.displayMode.refreshRate
        }
        var position1 by remember { mutableStateOf(0L) }
        var position2 by remember { mutableStateOf(0L) }
        var isOddFrame by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis {
                    position1 = it % width
                    position2 = (it / 4) % width
                }
            }
        }

        Canvas(Modifier.fillMaxSize()) {
            for (x in 0..width step singleFrameMillis) {
                drawLine(Color.Black, Offset(x.toFloat(), 0f), Offset(x.toFloat(), 10f))
            }

            drawRect(Color.Red, Offset(position1.toFloat(), 10f), Size(32f, 32f))
            drawRect(Color.Red, Offset(position2.toFloat(), 50f), Size(32f, 32f))

            // test similar to https://www.vsynctester.com/
            drawRect(if (isOddFrame) red else cyan, Offset(10f, 120f), Size(50f, 50f))
            isOddFrame = !isOddFrame

            logFrame()
        }
    }
}

private fun List<Long>.median() = sorted()[size / 2]