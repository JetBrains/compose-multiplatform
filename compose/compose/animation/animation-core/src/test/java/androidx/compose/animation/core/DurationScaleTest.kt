/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DurationScaleTest {
    @Test
    fun testAnimatable() = runBlocking {
        val clock = SuspendAnimationTest.TestFrameClock()
        withContext(clock + object : MotionDurationScale {
            override val scaleFactor: Float = 5f
        }) {
            var playTime = 0
            clock.frame(0L)
            animate(0f, 500f, animationSpec = tween(100, easing = LinearEasing)) { value, _ ->
                assertEquals(playTime.toFloat(), value)
                launch {
                    playTime += 10
                    clock.frame(playTime * 1_000_000L)
                }
            }
        }

        withContext(clock + object : MotionDurationScale {
            override val scaleFactor: Float = 0f
        }) {
            clock.frame(0L)
            animate(0f, 500f, animationSpec = tween(100, easing = LinearEasing)) { value, _ ->
                // This should finish right away
                assertEquals(500f, value)
            }
            clock.frame(0L)
            animate(0f, 100f, animationSpec = infiniteRepeatable(tween(100))) { value, _ ->
                // This should finish right away
                assertEquals(100f, value)
            }
        }
    }
}