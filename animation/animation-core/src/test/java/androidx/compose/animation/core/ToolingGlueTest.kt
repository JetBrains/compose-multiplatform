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

@file:Suppress("DEPRECATION")

package androidx.compose.animation.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.Long.max

@RunWith(JUnit4::class)
@OptIn(InternalAnimationApi::class)
class ToolingGlueTest {
    @Test
    fun testSeekableAnimation() {
        val animation = SeekableAnimation(def, "start", "end")
        val defaultAnim = FloatSpringSpec()
        assertEquals(
            max(defaultAnim.getDurationMillis(0f, 100f, 0f), 500L),
            animation.duration
        )

        var playtime = 0L
        while (playtime <= animation.duration) {
            val expectedAlpha = 1.0f - playtime / 1000f
            val expectedScale = defaultAnim.getValue(playtime, 0f, 100f, 0f)
            val animValues = animation.getAnimValuesAt(playtime)

            @Suppress("UNCHECKED_CAST")
            val actualAlpha = animValues[alpha as PropKey<Any, AnimationVector>] as Float
            @Suppress("UNCHECKED_CAST")
            val actualScale = animValues[scale as PropKey<Any, AnimationVector>] as Float

            assertEquals(actualAlpha, expectedAlpha, 0.01f)
            assertEquals(actualScale, expectedScale, 0.01f)
            playtime += 50L
        }
    }

    @Test
    fun testCreatingSeekableAnimation() {
        val anim = def.createAnimation(ManualAnimationClock(0L)).createSeekableAnimation(
            "end", "start"
        )
        assertEquals(600, anim.duration)
        assertEquals(def, anim.def)
    }

    @Test
    fun testGetStates() {
        val anim = def.createAnimation(ManualAnimationClock(0L))
        val states = anim.getStates()
        assertEquals(2, states.size)
        assertTrue(states.contains("start"))
        assertTrue(states.contains("end"))
    }

    @Test
    fun testMaxDurationPerIteration() {
        val anim = def2.createAnimation(ManualAnimationClock(0L)).createSeekableAnimation(
            "end", "start"
        )
        assertEquals(1200L, anim.maxDurationPerIteration)

        val anim2 = def2.createAnimation(ManualAnimationClock(0L)).createSeekableAnimation(
            "start", "end"
        )
        assertEquals(2000L, anim2.maxDurationPerIteration)
    }
}

private val scale = FloatPropKey("Scale")
private val alpha = FloatPropKey("Alpha")
private val test = FloatPropKey()

private val def = transitionDefinition<String> {
    state("start") {
        this[scale] = 0f
        this[alpha] = 1f
    }

    state("end") {
        this[scale] = 100f
        this[alpha] = 0.5f
    }

    transition("start" to "end") {
        alpha using tween(
            easing = LinearEasing,
            durationMillis = 500
        )
    }

    transition("end" to "start") {
        scale using tween(
            durationMillis = 600
        )
        alpha using tween(
            durationMillis = 600
        )
    }
}

private val def2 = transitionDefinition<String> {
    state("start") {
        this[scale] = 0f
        this[alpha] = 1f
        this[test] = 10f
    }

    state("end") {
        this[scale] = 100f
        this[alpha] = 0.5f
        this[test] = 0f
    }

    transition("start" to "end") {
        alpha using repeatable(
            100,
            tween(
                easing = LinearEasing,
                durationMillis = 200
            )
        )
        scale using repeatable(
            18,
            tween(
                easing = LinearEasing,
                durationMillis = 1000
            )
        )
        test using tween(durationMillis = 2000)
    }

    transition("end" to "start") {
        alpha using repeatable(
            100,
            tween(
                easing = LinearEasing,
                durationMillis = 200
            )
        )
        scale using repeatable(
            18,
            tween(
                easing = LinearEasing,
                durationMillis = 1000,
                delayMillis = 200
            )
        )
        test using tween(durationMillis = 200)
    }
}