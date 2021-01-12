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

package androidx.compose.animation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.animation.core.createAnimation
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Rect
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

const val epsilon = 0.00001f

@RunWith(JUnit4::class)
class PropertyKeyTest {
    @Test
    fun testRectPropKey() {
        val rectProp = RectPropKey()
        val start = Rect(0f, -60f, 50f, 500f)
        val end = Rect(350f, -420f, 550f, 70f)
        val transitionDef = transitionDefinition<Int> {
            state(0) {
                this[rectProp] = start
            }
            state(1) {
                this[rectProp] = end
            }
            transition {
                rectProp using tween(
                    durationMillis = 400,
                    easing = FastOutLinearInEasing
                )
            }
        }

        val clock = ManualAnimationClock(0)
        val anim = transitionDef.createAnimation(clock, 0)
        assertEquals(anim[rectProp], start)
        anim.toState(1)

        while (anim.isRunning) {
            val fraction = FastOutLinearInEasing.transform(clock.clockTimeMillis / 400f)
            val left = start.left * (1 - fraction) + end.left * fraction
            val top = start.top * (1 - fraction) + end.top * fraction
            val right = start.right * (1 - fraction) + end.right * fraction
            val bottom = start.bottom * (1 - fraction) + end.bottom * fraction
            val rect = anim[rectProp]
            assertEquals(left, rect.left, epsilon)
            assertEquals(top, rect.top, epsilon)
            assertEquals(right, rect.right, epsilon)
            assertEquals(bottom, rect.bottom, epsilon)
            clock.clockTimeMillis += 100
        }
    }
}