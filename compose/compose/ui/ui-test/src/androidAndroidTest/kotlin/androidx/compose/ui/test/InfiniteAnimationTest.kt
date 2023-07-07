/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.util.ClickableTestBox
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class InfiniteAnimationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testInfiniteTransition_finishes() {
        rule.setContent {
            val transition = rememberInfiniteTransition()
            val animationSpec = infiniteRepeatable<Float>(tween(), RepeatMode.Reverse)
            val offset = transition.animateFloat(0f, 100f, animationSpec)
            ClickableTestBox(Modifier.padding(start = offset.value.dp))
        }
        rule.runOnIdle {}
    }

    @Test
    fun testInfiniteAnimate_finishes() {
        rule.setContent {
            val offset = remember { mutableStateOf(1f) }
            LaunchedEffect(Unit) {
                animate(
                    initialValue = 1f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(), RepeatMode.Reverse)
                ) { value, _ ->
                    offset.value = value
                }
            }
            ClickableTestBox(Modifier.padding(start = offset.value.dp))
        }
        rule.runOnIdle {}
    }

    @Test
    fun testInfiniteAnimation_autoAdvancing_doesNotRun() {
        testInfiniteAnimation_autoAdvance(true)
    }

    @Test
    fun testInfiniteAnimation_manualAdvancing_doesRun() {
        testInfiniteAnimation_autoAdvance(false)
    }

    fun testInfiniteAnimation_autoAdvance(autoAdvance: Boolean) {
        rule.mainClock.autoAdvance = autoAdvance

        // If animating, animation moves by 8.dp per frame
        rule.setContent {
            val transition = rememberInfiniteTransition()
            val animationSpec = infiniteRepeatable<Float>(
                tween(durationMillis = 200, easing = LinearEasing),
                RepeatMode.Reverse
            )
            val offset = transition.animateFloat(0f, 100f, animationSpec)
            ClickableTestBox(Modifier.padding(start = offset.value.dp))
        }

        // 1st composition: animation not yet kicked off
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertLeftPositionInRootIsEqualTo(0.dp)
        rule.mainClock.advanceTimeByFrame()
        // 2nd composition: animation kicked off, at start position
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertLeftPositionInRootIsEqualTo(0.dp)
        rule.mainClock.advanceTimeByFrame()
        // 3rd composition: animation running, one frame done
        rule.onNodeWithTag(ClickableTestBox.defaultTag).assertLeftPositionInRootIsEqualTo(
            if (autoAdvance) 0.dp else 8.dp
        )
    }
}
