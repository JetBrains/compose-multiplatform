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

package androidx.compose.ui.test

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.testutils.WithTouchSlop
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingPolicy
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ComposeUiTestTest {

    private var idlingPolicy: IdlingPolicy? = null
    private lateinit var testDescription: Description

    /**
     * Records the current [testDescription] for tests that need to invoke the compose test rule
     * directly.
     */
    @get:Rule
    val testWatcher = object : TestWatcher() {
        override fun starting(description: Description) {
            testDescription = description
        }
    }

    @Before
    fun setup() {
        idlingPolicy = IdlingPolicies.getMasterIdlingPolicy()
    }

    @After
    fun tearDown() {
        idlingPolicy?.let {
            IdlingPolicies.setMasterPolicyTimeout(it.idleTimeout, it.idleTimeoutUnit)
        }
    }

    @Composable
    private fun ClickCounter(
        clicks: MutableState<Int> = remember { mutableStateOf(0) }
    ) {
        Column {
            Button(onClick = { clicks.value++ }) {
                Text("Click me")
            }
            Text("Click count: ${clicks.value}")
        }
    }

    /**
     * Check that basic scenarios work: a composition that is recomposed due to a state change.
     */
    @Test
    fun testStateChange() = runComposeUiTest {
        val clicks = mutableStateOf(0)
        setContent { ClickCounter(clicks) }

        clicks.value++
        onNodeWithText("Click count", substring = true).assertTextEquals("Click count: 1")

        clicks.value++
        onNodeWithText("Click count", substring = true).assertTextEquals("Click count: 2")
    }

    /**
     * Check that basic scenarios with input work: a composition that receives touch input and
     * changes state as a result of that, triggering recomposition.
     */
    @Test
    fun testInputInjection() = runComposeUiTest {
        setContent { ClickCounter() }

        onNodeWithText("Click me").performClick()
        onNodeWithText("Click count", substring = true).assertTextEquals("Click count: 1")

        onNodeWithText("Click me").performClick()
        onNodeWithText("Click count", substring = true).assertTextEquals("Click count: 2")
    }

    /**
     * Check that animation scenarios work: a composition with an animation in its initial state
     * is idle, stays non-idle while the animation animates to a new target and is idle again
     * after that.
     */
    @Test
    fun testAnimation() = runComposeUiTest {
        var target by mutableStateOf(0f)
        setContent {
            val offset = animateFloatAsState(target)
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .size(10.dp)
                        .offset(x = offset.value.dp)
                        .testTag("box")
                )
            }
        }
        onNodeWithTag("box").assertLeftPositionInRootIsEqualTo(0.dp)
        target = 100f
        onNodeWithTag("box").assertLeftPositionInRootIsEqualTo(100.dp)
    }

    /**
     * Check that scrolling and controlling the clock works: a scrollable receives a swipe while
     * the clock is paused, when the clock is resumed it performs the fling.
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun testControlledScrolling() = runComposeUiTest {
        // Define constants used in the test
        val n = 100
        val touchSlop = 16f
        val scrollState = ScrollState(0)
        val flingBehavior = SimpleFlingBehavior(deltas = 20 downTo 1)

        // Set content: a list where the fling is always the same, regardless of the swipe
        setContent {
            WithTouchSlop(touchSlop = touchSlop) {
                // turn off visual overscroll for calculation correctness
                CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    Box(Modifier.fillMaxSize()) {
                        Column(
                            Modifier
                                .requiredSize(200.dp)
                                .verticalScroll(
                                    scrollState,
                                    flingBehavior = flingBehavior
                                )
                                .testTag("list")
                        ) {
                            repeat(n) {
                                Spacer(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Stop auto advancing and perform a swipe. The list will "freeze" in the position where
        // it was at the end of the swipe
        mainClock.autoAdvance = false
        onNodeWithTag("list").performTouchInput {
            down(bottomCenter)
            repeat(10) {
                moveTo(bottomCenter - percentOffset(y = (it + 1) / 10f))
            }
            up()
        }
        waitForIdle()

        // Check that we're in that frozen position
        val expectedViewPortSize = with(density) { 200.dp.toPx() }
        val expectedSwipeDistance = (expectedViewPortSize - touchSlop).roundToInt()
        assertThat(scrollState.value).isEqualTo(expectedSwipeDistance)

        // "Unfreeze" the list and let the fling run. The list will stop at
        // `flingBehavior.totalDistance` pixels further than where it was frozen.
        mainClock.autoAdvance = true
        waitForIdle()
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

    @Test
    fun getActivityTest() = runAndroidComposeUiTest<ComponentActivity> {
        assertThat(activity).isNotNull()
    }

    @Test
    fun effectContextPropagatedToComposition_runComposeUiTest() {
        val testElement = TestCoroutineContextElement()
        runComposeUiTest(effectContext = testElement) {
            lateinit var compositionScope: CoroutineScope
            setContent {
                compositionScope = rememberCoroutineScope()
            }

            runOnIdle {
                val elementFromComposition =
                    compositionScope.coroutineContext[TestCoroutineContextElement]
                assertThat(elementFromComposition).isSameInstanceAs(testElement)
            }
        }
    }

    @Test
    fun effectContextPropagatedToComposition_createComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createComposeRule(testElement)
        val baseStatement = object : Statement() {
            override fun evaluate() {
                rule.setContent {
                    compositionScope = rememberCoroutineScope()
                }
                rule.waitForIdle()
            }
        }
        rule.apply(baseStatement, testDescription)
            .evaluate()

        val elementFromComposition =
            compositionScope.coroutineContext[TestCoroutineContextElement]
        assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun effectContextPropagatedToComposition_createAndroidComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val rule = createAndroidComposeRule<ComponentActivity>(testElement)
        val baseStatement = object : Statement() {
            override fun evaluate() {
                rule.setContent {
                    compositionScope = rememberCoroutineScope()
                }
                rule.waitForIdle()
            }
        }
        rule.apply(baseStatement, testDescription)
            .evaluate()

        val elementFromComposition =
            compositionScope.coroutineContext[TestCoroutineContextElement]
        assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun effectContextPropagatedToComposition_createEmptyComposeRule() {
        val testElement = TestCoroutineContextElement()
        lateinit var compositionScope: CoroutineScope
        val composeRule = createEmptyComposeRule(testElement)
        val activityRule = ActivityScenarioRule(ComponentActivity::class.java)
        val baseStatement = object : Statement() {
            override fun evaluate() {
                activityRule.scenario.onActivity {
                    it.setContent {
                        compositionScope = rememberCoroutineScope()
                    }
                }
                composeRule.waitForIdle()
            }
        }
        activityRule.apply(composeRule.apply(baseStatement, testDescription), testDescription)
            .evaluate()

        val elementFromComposition =
            compositionScope.coroutineContext[TestCoroutineContextElement]
        assertThat(elementFromComposition).isSameInstanceAs(testElement)
    }

    @Test
    fun motionDurationScale_defaultValue() = runComposeUiTest {
        var lastRecordedMotionDurationScale: Float? = null
        setContent {
            val context = rememberCoroutineScope().coroutineContext
            lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
        }

        runOnIdle {
            assertThat(lastRecordedMotionDurationScale).isNull()
        }
    }

    @Test
    fun motionDurationScale_propagatedToCoroutines() {
        val motionDurationScale = object : MotionDurationScale {
            override val scaleFactor: Float get() = 0f
        }
        runComposeUiTest(effectContext = motionDurationScale) {
            var lastRecordedMotionDurationScale: Float? = null
            setContent {
                val context = rememberCoroutineScope().coroutineContext
                lastRecordedMotionDurationScale = context[MotionDurationScale]?.scaleFactor
            }

            runOnIdle {
                assertThat(lastRecordedMotionDurationScale).isEqualTo(0f)
            }
        }
    }

    private class TestCoroutineContextElement : CoroutineContext.Element {
        override val key: CoroutineContext.Key<*> get() = Key

        companion object Key : CoroutineContext.Key<TestCoroutineContextElement>
    }
}
