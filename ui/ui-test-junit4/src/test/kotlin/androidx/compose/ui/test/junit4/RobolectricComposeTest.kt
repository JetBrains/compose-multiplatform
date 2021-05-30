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

package androidx.compose.ui.test.junit4

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.bottomCenter
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.espresso.AppNotIdleException
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@Config(minSdk = 21)
class RobolectricComposeTest {
    @get:Rule
    val rule = createComposeRule()

    private var masterTimeout: IdlingPolicy? = null

    @Before
    fun setup() {
        masterTimeout = IdlingPolicies.getMasterIdlingPolicy()
    }

    @After
    fun tearDown() {
        masterTimeout?.let {
            IdlingPolicies.setMasterPolicyTimeout(it.idleTimeout, it.idleTimeoutUnit)
        }
    }

    /**
     * Check that basic scenarios work: a composition that is recomposed due to a state change.
     */
    @Test
    fun testStateChange() {
        runClickScenario { clicks ->
            clicks.value++
        }
    }

    /**
     * Check that basic scenarios with input work: a composition that receives touch input and
     * changes state as a result of that, triggering recomposition.
     */
    @Test
    fun testInputInjection() {
        runClickScenario {
            rule.onNodeWithText("Click me").performClick()
        }
    }

    private fun runClickScenario(interaction: (MutableState<Int>) -> Unit) {
        val clicks = mutableStateOf(0)
        rule.setContent {
            Button(onClick = { clicks.value++ }) {
                Text("Click me")
            }
            Text("Click count: ${clicks.value}")
        }
        rule.onNodeWithText("Click me").assertExists()

        interaction.invoke(clicks)
        rule.onNodeWithText("Click count", substring = true)
            .assertTextEquals("Click count: 1")

        interaction.invoke(clicks)
        rule.onNodeWithText("Click count", substring = true)
            .assertTextEquals("Click count: 2")
    }

    /**
     * Check that animation scenarios work: a composition with an animation in its initial state
     * is idle, stays non-idle while the animation animates to a new target and is idle again
     * after that.
     */
    @Test
    fun testAnimation() {
        var target by mutableStateOf(0f)
        rule.setContent {
            val offset = animateFloatAsState(target)
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(10.dp).offset(x = offset.value.dp).testTag("box"))
            }
        }
        rule.onNodeWithTag("box").assertLeftPositionInRootIsEqualTo(0.dp)
        target = 100f
        rule.onNodeWithTag("box").assertLeftPositionInRootIsEqualTo(100.dp)
    }

    /**
     * Check that we catch a potential infinite composition loop caused by a measure lambda that
     * triggers itself.
     */
    @Test(timeout = 5000)
    fun testTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(2, TimeUnit.SECONDS)
        expectError<AppNotIdleException>(
            expectedMessage = "Compose did not get idle after [0-9]* attempts in 2 SECONDS\\..*"
        ) {
            rule.setContent {
                var x by remember { mutableStateOf(0) }
                Box(Modifier.requiredSize(100.dp)) {
                    Layout({ Box(Modifier.size(10.dp)) }) { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }

                        // read x, so we need to relayout when x changes
                        val offset = if (x >= 0) 0 else -1
                        val width = offset + placeables.maxOf { it.width }
                        val height = offset + placeables.maxOf { it.height }

                        // woops, we're always changing x during layout!
                        x = if (x == 0) 1 else 0

                        layout(width, height) {
                            placeables.forEach { it.place(0, 0) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check that scrolling and controlling the clock works: a scrollable receives a swipe while
     * the clock is paused, when the clock is resumed it performs the fling.
     */
    @Test
    fun testControlledScrolling() {
        // Define constants used in the test
        val n = 100
        val touchSlop = 16f
        val scrollState = ScrollState(0)
        val flingBehavior = SimpleFlingBehavior(deltas = 20 downTo 1)

        // Set content: a list where the fling is always the same, regardless of the swipe
        rule.setContent {
            WithTouchSlop(touchSlop = touchSlop) {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.requiredSize(200.dp).verticalScroll(
                            scrollState,
                            flingBehavior = flingBehavior
                        ).testTag("list")
                    ) {
                        repeat(n) {
                            Spacer(Modifier.fillMaxWidth().height(30.dp))
                        }
                    }
                }
            }
        }

        // Stop auto advancing and perform a swipe. The list will "freeze" in the position where
        // it was at the end of the swipe
        rule.mainClock.autoAdvance = false
        rule.onNodeWithTag("list").performGesture {
            down(bottomCenter)
            repeat(10) {
                moveTo(bottomCenter - percentOffset(y = (it + 1) / 10f))
            }
            up()
        }
        rule.waitForIdle()

        // Check that we're in that frozen position
        val expectedViewPortSize = with(rule.density) { 200.dp.toPx() }
        val expectedSwipeDistance = (expectedViewPortSize - touchSlop).roundToInt()
        assertThat(scrollState.value).isEqualTo(expectedSwipeDistance)

        // "Unfreeze" the list and let the fling run. The list will stop at
        // `flingBehavior.totalDistance` pixels further than where it was frozen.
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        val expectedFlingDistance = flingBehavior.totalDistance
        assertThat(scrollState.value).isEqualTo(expectedSwipeDistance + expectedFlingDistance)
    }

    /**
     * A simple [FlingBehavior] that scrolls one [delta][deltas] every frame regardless of velocity.
     */
    private class SimpleFlingBehavior(private val deltas: IntProgression) : FlingBehavior {
        val totalDistance = deltas.sum()

        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            for (delta in deltas) {
                withFrameNanos {
                    scrollBy(delta.toFloat())
                }
            }
            return 0f
        }
    }
}
