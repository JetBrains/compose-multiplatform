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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.tooling.animation.AnimationSearch
import androidx.compose.ui.tooling.animation.InfiniteTransitionComposeAnimation.Companion.parse
import androidx.compose.ui.tooling.animation.Utils.nullableFloatConverter
import androidx.compose.ui.tooling.animation.Utils.searchForAnimation
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class InfiniteTransitionClockTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun checkAnimatedPropertiesForAnimateFloat() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0.2f, 2.1f, infiniteRepeatable(tween(300), RepeatMode.Reverse), label = "Test label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnUiThread {
            // Default state
            clock.getAnimatedProperties().let {
                assertEquals(1, it.size)
                assertEquals(0.2f, it[0].value)
                assertEquals("Test label", it[0].label)
            }
        }
    }

    @Test
    fun checkAnimatedPropertiesForAnimateValue() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateValue(
                30,
                40,
                Int.VectorConverter,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
                label = "Test label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnUiThread {
            // Default state
            clock.getAnimatedProperties().let {
                assertEquals(1, it.size)
                assertEquals(30, it[0].value)
                assertEquals("Test label", it[0].label)
            }
        }
    }

    @Test
    fun checkAnimatedPropertiesForAnimateColor() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateColor(
                Color.Red,
                Color.Green,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
                label = "Test label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnUiThread {
            // Default state
            clock.getAnimatedProperties().let {
                assertEquals(1, it.size)
                assertEquals(Color.Red, it[0].value)
                assertEquals("Test label", it[0].label)
            }
        }
    }

    @Test
    fun checkAnimatedPropertiesForNullableAnimateValue() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateValue(
                30f,
                null,
                nullableFloatConverter,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
                label = "Test label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnUiThread {
            // Default state
            clock.getAnimatedProperties().let {
                assertEquals(1, it.size)
                assertEquals(30f, it[0].value)
                assertEquals("Test label", it[0].label)
            }
        }
    }

    @Test
    fun checkTransitions() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0.2f,
                2.1f,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
                label = "Float label"
            )
            infiniteTransition.animateValue(
                20,
                30,
                Int.VectorConverter,
                infiniteRepeatable(tween(500), RepeatMode.Restart),
                label = "Int label"
            )
            infiniteTransition.animateColor(
                Color.Red,
                Color.White,
                infiniteRepeatable(tween(400), RepeatMode.Reverse),
                label = "Color label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnIdle {
            val transitions = clock.getTransitions(100)
            assertEquals(3, transitions.size)
            transitions[0].let {
                assertEquals("Float label", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(800, it.endTimeMillis)
                assertTrue(it.specType.contains("InfiniteRepeatableSpec"))
                assertTrue(it.values.size >= 3)
                assertTrue(it.values.keys.distinct().size >= 3)
                assertTrue(it.values.values.distinct().size >= 3)
            }
            transitions[1].let {
                assertEquals("Int label", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(800, it.endTimeMillis)
                assertTrue(it.specType.contains("InfiniteRepeatableSpec"))
                assertTrue(it.values.size >= 3)
                assertTrue(it.values.keys.distinct().size >= 3)
                assertTrue(it.values.values.distinct().size >= 3)
            }
            transitions[2].let {
                assertEquals("Color label", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(800, it.endTimeMillis)
                assertTrue(it.specType.contains("InfiniteRepeatableSpec"))
                assertTrue(it.values.size >= 3)
                assertTrue(it.values.keys.distinct().size >= 3)
                assertTrue(it.values.values.distinct().size >= 3)
            }
        }
    }

    @Test
    fun checkNullableTransitions() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateValue(
                30f,
                null,
                nullableFloatConverter,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
                label = "Test label"
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnIdle {
            val transitions = clock.getTransitions(100)
            assertEquals(1, transitions.size)
            transitions[0].let {
                assertEquals("Test label", it.label)
                assertEquals(0, it.startTimeMillis)
                assertEquals(600, it.endTimeMillis)
                assertTrue(it.specType.contains("InfiniteRepeatableSpec"))
                assertTrue(it.values.size >= 3)
                assertTrue(it.values.keys.distinct().size >= 3)
                assertTrue(it.values.values.distinct().size >= 3)
            }
        }
    }

    @Test
    fun checkDurationOfReverseAnimation() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(300), RepeatMode.Reverse),
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnIdle {
            assertEquals(600, clock.getMaxDurationPerIteration())
            assertEquals(600, clock.getMaxDuration())
        }
    }

    @Test
    fun checkDurationOfRestartAnimation() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(300, 50), RepeatMode.Restart),
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnIdle {
            assertEquals(350, clock.getMaxDurationPerIteration())
            assertEquals(350, clock.getMaxDuration())
        }
    }

    @Test
    fun maxDurationIsCorrect() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(100), RepeatMode.Restart),
            )
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(300), RepeatMode.Restart),
            )
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(500), RepeatMode.Restart),
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!)
        rule.runOnIdle {
            assertEquals(500, clock.getMaxDurationPerIteration())
            assertEquals(500, clock.getMaxDuration())
        }
    }

    @Test
    fun maxDurationFromOtherAnimations() {
        val search = AnimationSearch.InfiniteTransitionSearch { }
        rule.searchForAnimation(search) {
            val infiniteTransition = rememberInfiniteTransition()
            infiniteTransition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(100), RepeatMode.Restart),
            )
        }
        val clock = InfiniteTransitionClock(search.animations.first().parse()!!) { 1300 }
        rule.runOnIdle {
            assertEquals(100, clock.getMaxDurationPerIteration())
            assertEquals(1300, clock.getMaxDuration())
        }
    }
}