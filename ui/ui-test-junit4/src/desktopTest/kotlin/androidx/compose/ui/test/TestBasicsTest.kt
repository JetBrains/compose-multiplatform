/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.Rule


/**
 * Basic tests of the testing framework itself.
 */
@OptIn(ExperimentalTestApi::class)
class TestBasicsTest {

    @get:Rule
    val rule = createComposeRule()

    // See https://github.com/JetBrains/compose-multiplatform/issues/3117
    @Test
    fun recompositionCompletesBeforeSetContentReturns() = repeat(1000) {
        runSkikoComposeUiTest {
            var globalValue by atomic(0)
            setContent {
                var localValue by remember{ mutableStateOf(0) }

                remember(localValue) {
                    globalValue = localValue
                }

                Layout(
                    {},
                    Modifier,
                    measurePolicy = { _, constraints ->
                        localValue = 100
                        layout(constraints.maxWidth, constraints.maxHeight) {}
                    }
                )
            }

            assertEquals(100, globalValue)
        }
    }

    @Test
    fun inputEventAdvancesClock() {
        rule.setContent {
            Box(Modifier.testTag("box"))
        }

        val clockBefore = rule.mainClock.currentTime
        rule.onNodeWithTag("box").performClick()
        val clockAfter = rule.mainClock.currentTime
        assertTrue(clockAfter > clockBefore, "performClick did not advance the test clock")
    }

    @Test
    fun advancingClockRunsRecomposition() {
        rule.mainClock.autoAdvance = false

        rule.setContent {
            var text by remember { mutableStateOf("1") }
            Text(text, modifier = Modifier.testTag("text"))

            LaunchedEffect(Unit){
                delay(1_000)
                text = "2"
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("1")
        rule.mainClock.advanceTimeBy(999, ignoreFrameDuration = true)
        rule.onNodeWithTag("text").assertTextEquals("1")
        rule.mainClock.advanceTimeBy(1, ignoreFrameDuration = true)
        rule.onNodeWithTag("text").assertTextEquals("2")
    }

    @Test
    fun obtainingSemanticsNodeInteractionWaitsUntilIdle() {
        var text by mutableStateOf("1")

        rule.setContent {
            Text(text, modifier = Modifier.testTag("text"))
        }

        rule.onNodeWithTag("text").assertTextEquals("1")
        text = "2"
        rule.onNodeWithTag("text").assertTextEquals("2")
    }

    @Test
    fun testCaptureToImage() {
        val color = Color.Green
        rule.setContent {
            Box(Modifier.testTag("box").size(20.dp).background(color))
        }

        val screenshot = rule.onNodeWithTag("box").captureToImage()

        assertEquals(20, screenshot.width)
        assertEquals(20, screenshot.height)

        IntArray(20*20).let { buffer ->
            screenshot.readPixels(buffer)
            val expectedPixel = color.toArgb()
            for (pixel in buffer) {
                assertEquals(expectedPixel, pixel)
            }
        }
    }

    @Test
    fun testIdlingResource() {
        var text by mutableStateOf("")
        rule.setContent {
            Text(
                text = text,
                modifier = Modifier.testTag("text")
            )
        }

        var isIdle = true
        val idlingResource = object: IdlingResource {
            override val isIdleNow: Boolean
                get() = isIdle
        }

        fun test(expectedValue: String) {
            text = "first"
            isIdle = false
            val job = CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                text = "second"
                isIdle = true
            }
            try {
                rule.onNodeWithTag("text").assertTextEquals(expectedValue)
            } finally {
                job.cancel()
            }
        }

        // With the idling resource registered, we expect the test to wait until the second value
        // has been set.
        rule.registerIdlingResource(idlingResource)
        test(expectedValue = "second")

        // Without the idling resource registered, we expect the test to see the first value
        rule.unregisterIdlingResource(idlingResource)
        test(expectedValue = "first")
    }
}