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

package androidx.compose.foundation

import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.ManualFrameClock
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.animation.smoothScrollBy
import androidx.compose.foundation.gestures.Scrollable
import androidx.compose.foundation.gestures.ScrollableController
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.MockAnimationClock
import androidx.compose.testutils.advanceClockOnMainThreadMillis
import androidx.compose.testutils.monotonicFrameAnimationClockOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.nestedscroll.NestedScrollConnection
import androidx.compose.ui.gesture.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.gesture.nestedscroll.NestedScrollSource
import androidx.compose.ui.gesture.nestedscroll.nestedScroll
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.runBlockingWithManualClock
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.milliseconds
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ScrollableTest {

    @get:Rule
    val rule = createComposeRule()

    private val scrollableBoxTag = "scrollableBox"

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_horizontalScroll() = runBlockingWithManualClock { clock ->
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_verticalScroll() = runBlockingWithManualClock { clock ->
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_startStop_notify() = runBlockingWithManualClock(true) { clock ->
        var startTrigger = 0f
        var stopTrigger = 0f
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Horizontal,
                onScrollStarted = { startTrigger++ },
                onScrollStopped = { stopTrigger++ }
            )
        }
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(0)
            assertThat(stopTrigger).isEqualTo(0)
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        // don't wait for animation so stop is 0, as we flinging still
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(0)
        }
        advanceClockWhileAwaitersExist(clock)
        // after wait we expect stop to trigger
        rule.runOnIdle {
            assertThat(startTrigger).isEqualTo(1)
            assertThat(stopTrigger).isEqualTo(1)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_disabledWontCallLambda() = runBlockingWithManualClock(true) { clock ->
        val enabled = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Horizontal,
                enabled = enabled.value
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)
        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_velocityProxy() = runBlockingWithManualClock {
        var velocityTriggered = 0f
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Horizontal,
                onScrollStopped = { velocity ->
                    velocityTriggered = velocity
                }
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                endVelocity = 112f,
                duration = 100.milliseconds

            )
        }
        // don't advance clocks, so animation won't trigger yet
        // and interrupt
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                endVelocity = 312f,
                duration = 100.milliseconds

            )
        }
        rule.runOnIdle {
            // should be first velocity, as fling was disrupted
            assertThat(velocityTriggered - 112f).isLessThan(0.1f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_startWithoutSlop_ifFlinging() = runBlockingWithManualClock {
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(
                controller = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                duration = 100.milliseconds
            )
        }
        // don't advance clocks
        val prevTotal = rule.runOnUiThread {
            assertThat(total).isGreaterThan(0f)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 114f, this.center.y),
                duration = 100.milliseconds
            )
        }
        rule.runOnIdle {
            // last swipe should add exactly 114 as we don't advance clocks and already flinging
            val expected = prevTotal + 114
            assertThat(total - expected).isLessThan(0.1f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_cancel_callsDragStop() = runBlocking {
        var total by mutableStateOf(0f)
        var dragStopped = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            if (total < 20) {
                Modifier.scrollable(
                    controller = controller,
                    orientation = Orientation.Horizontal,
                    onScrollStopped = {
                        dragStopped++
                    }
                )
            } else {
                Modifier
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100, this.center.y),
                duration = 100.milliseconds
            )
        }
        rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(dragStopped).isEqualTo(1f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_snappingScrolling() = runBlocking {
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            Modifier.scrollable(orientation = Orientation.Vertical, controller = controller)
        }
        rule.awaitIdle()
        assertThat(total).isEqualTo(0f)

        (controller as Scrollable).smoothScrollBy(1000f)
        assertThat(total).isWithin(0.001f).of(1000f)

        (controller as Scrollable).smoothScrollBy(-200f)
        assertThat(total).isWithin(0.001f).of(800f)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_explicitDisposal() = runBlockingWithManualClock { clock ->
        val disposed = mutableStateOf(false)
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                assertWithMessage("Animating after dispose!").that(disposed.value).isFalse()
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        )
        setScrollableContent {
            if (!disposed.value) {
                Modifier.scrollable(orientation = Orientation.Vertical, controller = controller)
            } else {
                Modifier
            }
        }
        launch {
            controller.smoothScrollBy(300f)
        }
        advanceClockWhileAwaitersExist(clock)
        assertThat(total).isEqualTo(300f)

        launch {
            controller.smoothScrollBy(200f)
        }
        // don't advance clocks yet, toggle disposed value
        disposed.value = true

        // Modifier should now have been disposed and cancelled the scroll, advance clocks to
        // confirm that it does not animate (checked in consumeScrollDelta)
        advanceClockWhileAwaitersExist(clock)

        // still 300 and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(300f)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedDrag() = runBlockingWithManualClock { clock ->
        var innerDrag = 0f
        var outerDrag = 0f
        val animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        val outerState = ScrollableController(
            consumeScrollDelta = {
                outerDrag += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = animationClock
        )
        val innerState = ScrollableController(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = animationClock
        )

        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .preferredSize(300.dp)
                        .scrollable(
                            controller = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier.testTag(scrollableBoxTag)
                            .preferredSize(300.dp)
                            .scrollable(
                                controller = innerState,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                duration = 300.milliseconds,
                endVelocity = 0f
            )
        }
        val lastEqualDrag = rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
            innerDrag
        }
        advanceClockWhileAwaitersExist(clock)
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            // values should be the same since no fling
            assertThat(innerDrag).isEqualTo(lastEqualDrag)
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedFling() = runBlockingWithManualClock { clock ->
        var innerDrag = 0f
        var outerDrag = 0f
        val animationClock = monotonicFrameAnimationClockOf(coroutineContext)
        val outerState = ScrollableController(
            consumeScrollDelta = {
                outerDrag += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = animationClock
        )
        val innerState = ScrollableController(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = animationClock
        )

        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .preferredSize(300.dp)
                        .scrollable(
                            controller = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .preferredSize(300.dp)
                            .scrollable(
                                controller = innerState,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        // swipe again with velocity
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                duration = 300.milliseconds
            )
        }
        val lastEqualDrag = rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
            innerDrag
        }
        // advance clocks, triggering fling
        advanceClockWhileAwaitersExist(clock)
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(lastEqualDrag)
            assertThat(outerDrag).isGreaterThan(lastEqualDrag)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_respectsPreConsumption() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            var lastReceivedPreScrollAvailable = 0f
            val preConsumeFraction = 0.7f
            val animationClock = monotonicFrameAnimationClockOf(coroutineContext)
            val controller = ScrollableController(
                consumeScrollDelta = {
                    val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                    assertThat(it - expected).isWithin(0.01f)
                    value += it
                    it
                },
                flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
                animationClock = animationClock
            )
            val preConsumingParent = object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    lastReceivedPreScrollAvailable = available.x
                    return available * preConsumeFraction
                }

                override fun onPreFling(available: Velocity): Velocity {
                    // consume all velocity
                    return available
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .preferredSize(300.dp)
                            .nestedScroll(preConsumingParent)
                    ) {
                        Box(
                            modifier = Modifier.preferredSize(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    controller = controller,
                                    orientation = Orientation.Horizontal
                                )
                        )
                    }
                }
            }

            rule.onNodeWithTag(scrollableBoxTag).performGesture {
                this.swipe(
                    start = this.center,
                    end = Offset(this.center.x + 200f, this.center.y),
                    duration = 300.milliseconds
                )
            }

            val preFlingValue = rule.runOnIdle { value }
            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)
            rule.runOnIdle {
                // if scrollable respects prefling consumption, it should fling 0px since we
                // preconsume all
                assertThat(preFlingValue).isEqualTo(value)
            }
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_proxiesPostCycles() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            var expectedLeft = 0f
            val velocityFlung = 5000f
            val animationClock = monotonicFrameAnimationClockOf(coroutineContext)
            val controller = ScrollableController(
                consumeScrollDelta = {
                    val toConsume = it * 0.345f
                    value += toConsume
                    expectedLeft = it - toConsume
                    toConsume
                },
                flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
                animationClock = animationClock
            )
            val parent = object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // we should get in post scroll as much as left in controller callback
                    assertThat(available.x).isEqualTo(expectedLeft)
                    return available
                }

                override fun onPostFling(
                    consumed: Velocity,
                    available: Velocity,
                    onFinished: (Velocity) -> Unit
                ) {
                    assertThat(available)
                        .isEqualTo(
                            Velocity(x = velocityFlung, y = 0f) - consumed
                        )
                    onFinished.invoke(available)
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .preferredSize(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.preferredSize(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    controller = controller,
                                    orientation = Orientation.Horizontal
                                )
                        )
                    }
                }
            }

            rule.onNodeWithTag(scrollableBoxTag).performGesture {
                this.swipeWithVelocity(
                    start = this.center,
                    end = Offset(this.center.x + 500f, this.center.y),
                    duration = 300.milliseconds,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollBelow_listensDispatches() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            var expectedConsumed = 0f
            val animationClock = monotonicFrameAnimationClockOf(coroutineContext)
            val controller = ScrollableController(
                consumeScrollDelta = {
                    expectedConsumed = it * 0.3f
                    value += expectedConsumed
                    expectedConsumed
                },
                flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
                animationClock = animationClock
            )
            val child = object : NestedScrollConnection {}
            val dispatcher = NestedScrollDispatcher()

            rule.setContent {
                Box {
                    Box(
                        modifier = Modifier.preferredSize(300.dp)

                            .scrollable(
                                controller = controller,
                                orientation = Orientation.Horizontal
                            )
                    ) {
                        Box(
                            Modifier.preferredSize(200.dp)
                                .testTag(scrollableBoxTag)
                                .nestedScroll(child, dispatcher)
                        )
                    }
                }
            }

            val lastValueBeforeFling = rule.runOnIdle {
                val preScrollConsumed = dispatcher
                    .dispatchPreScroll(Offset(20f, 20f), NestedScrollSource.Drag)
                // scrollable is not interested in pre scroll
                assertThat(preScrollConsumed).isEqualTo(Offset.Zero)

                val consumed = dispatcher.dispatchPostScroll(
                    Offset(20f, 20f),
                    Offset(50f, 50f),
                    NestedScrollSource.Drag
                )
                assertThat(consumed.x - expectedConsumed).isWithin(0.001f)

                val preFlingConsumed = dispatcher
                    .dispatchPreFling(Velocity(50f, 50f))
                // scrollable won't participate in the pre fling
                assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)

                dispatcher.dispatchPostFling(
                    Velocity(1000f, 1000f),
                    Velocity(2000f, 2000f)
                )
                value
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            rule.runOnIdle {
                // catch that scrollable caught our post fling and flung
                assertThat(value).isGreaterThan(lastValueBeforeFling)
            }
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_interactionState() = runBlocking {
        val interactionState = InteractionState()
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext),
            interactionState = interactionState
        )

        setScrollableContent {
            Modifier.scrollable(
                Orientation.Horizontal,
                controller = controller
            ) {}
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                up()
            }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_interactionState_resetWhenDisposed() = runBlocking {
        val interactionState = InteractionState()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller = ScrollableController(
            consumeScrollDelta = {
                total += it
                it
            },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = monotonicFrameAnimationClockOf(coroutineContext),
            interactionState = interactionState
        )

        rule.setContent {
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .preferredSize(100.dp)
                            .scrollable(
                                orientation = Orientation.Horizontal,
                                controller = controller
                            ) {}
                    )
                }
            }
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactionState.value).contains(Interaction.Dragged)
        }

        // Dispose scrollable
        rule.runOnIdle {
            emitScrollableBox = false
        }

        rule.runOnIdle {
            assertThat(interactionState.value).doesNotContain(Interaction.Dragged)
        }
    }

    @Test
    fun testInspectorValue() {
        val controller = ScrollableController(
            consumeScrollDelta = { it },
            flingConfig = FlingConfig(decayAnimation = FloatExponentialDecaySpec()),
            animationClock = MockAnimationClock()
        )
        rule.setContent {
            val modifier = Modifier.scrollable(Orientation.Vertical, controller) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "controller",
                "enabled",
                "reverseDirection",
                "canScroll",
                "onScrollStarted",
                "onScrollStopped",
            )
        }
    }

    private fun setScrollableContent(scrollableModifierFactory: @Composable () -> Modifier) {
        rule.setContent {
            Box {
                val scrollable = scrollableModifierFactory()
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .preferredSize(100.dp).then(scrollable)
                )
            }
        }
    }

    @ExperimentalTestApi
    private suspend fun advanceClockWhileAwaitersExist(clock: ManualFrameClock) {
        rule.awaitIdle()
        yield()
        while (clock.hasAwaiters) {
            clock.advanceClockOnMainThreadMillis(5000L)
        }
    }
}
