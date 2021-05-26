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

import androidx.compose.animation.core.ManualFrameClock
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.testutils.advanceClockOnMainThreadMillis
import androidx.compose.testutils.runBlockingWithManualClock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

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
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
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
                durationMillis = 100
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
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_horizontalScroll_reverse() = runBlockingWithManualClock { clock ->
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
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
                durationMillis = 100
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
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
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
                durationMillis = 100
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
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_verticalScroll_reversed() = runBlockingWithManualClock { clock ->
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                reverseDirection = true,
                state = controller,
                orientation = Orientation.Vertical
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
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
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_disabledWontCallLambda() = runBlockingWithManualClock { clock ->
        val enabled = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal,
                enabled = enabled.value
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
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
                durationMillis = 100
            )
        }
        advanceClockWhileAwaitersExist(clock)
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)
        val prev = total
        // pump frames twice to start fling animation
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        val expected = prevAfterSomeFling + 115
        rule.mainClock.advanceTimeBy(1000)
        assertThat(total).isEqualTo(expected)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_snappingScrolling() = runBlocking(AutoTestFrameClock()) {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setScrollableContent {
            Modifier.scrollable(
                orientation = Orientation.Vertical,
                state = controller
            )
        }
        rule.awaitIdle()
        assertThat(total).isEqualTo(0f)

        controller.animateScrollBy(1000f)
        assertThat(total).isWithin(0.001f).of(1000f)

        controller.animateScrollBy(-200f)
        assertThat(total).isWithin(0.001f).of(800f)
    }

    @Test
    fun scrollable_explicitDisposal() = runBlocking(AutoTestFrameClock()) {
        rule.mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                assertWithMessage("Animating after dispose!").that(expectEmission.value).isTrue()
                total += it
                it
            }
        )
        setScrollableContent {
            if (emit.value) {
                Modifier.scrollable(
                    orientation = Orientation.Horizontal,
                    state = controller
                )
            } else {
                Modifier
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)

        // start the fling for a few frames
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        // flip the emission
        rule.runOnUiThread {
            emit.value = false
        }
        // propagate the emit flip and record the value
        rule.mainClock.advanceTimeByFrame()
        val prevTotal = total
        // make sure we don't receive any deltas
        rule.runOnUiThread {
            expectEmission.value = false
        }

        // pump the clock until idle
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()

        // still same and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedDrag() = runBlockingWithManualClock { clock ->
        var innerDrag = 0f
        var outerDrag = 0f
        val outerState = ScrollableState(
            consumeScrollDelta = {
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier.testTag(scrollableBoxTag)
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
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
                durationMillis = 300,
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
        val outerState = ScrollableState(
            consumeScrollDelta = {
                outerDrag += it
                it
            }
        )
        val innerState = ScrollableState(
            consumeScrollDelta = {
                innerDrag += it / 2
                it / 2
            }
        )

        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(300.dp)
                            .scrollable(
                                state = innerState,
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
                durationMillis = 300
            )
        }
        assertThat(innerDrag).isGreaterThan(0f)
        assertThat(outerDrag).isGreaterThan(0f)
        // we consumed half delta in child, so exactly half should go to the parent
        assertThat(outerDrag).isEqualTo(innerDrag)
        val lastEqualDrag = innerDrag
        // advance clocks, triggering fling
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
            val controller = ScrollableState(
                consumeScrollDelta = {
                    val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                    assertThat(it - expected).isWithin(0.01f)
                    value += it
                    it
                }
            )
            val preConsumingParent = object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    lastReceivedPreScrollAvailable = available.x
                    return available * preConsumeFraction
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    // consume all velocity
                    return available
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(preConsumingParent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
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
                    durationMillis = 300
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
    fun scrollable_nestedScrollAbove_proxiesPostFling() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            val velocityFlung = 5000f
            val controller = ScrollableState(
                consumeScrollDelta = {
                    value += it
                    it
                }
            )
            val parent = object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    val expected = velocityFlung - consumed.x
                    assertThat(abs(available.x - expected)).isLessThan(0.1f)
                    return available
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
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
                    durationMillis = 300,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_proxiesPostScroll() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            var expectedLeft = 0f
            val velocityFlung = 5000f
            val controller = ScrollableState(
                consumeScrollDelta = {
                    val toConsume = it * 0.345f
                    value += toConsume
                    expectedLeft = it - toConsume
                    toConsume
                }
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
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
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
                    durationMillis = 300,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_reversed_proxiesPostScroll() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            var expectedLeft = 0f
            val velocityFlung = 5000f
            val controller = ScrollableState(
                consumeScrollDelta = {
                    val toConsume = it * 0.345f
                    value += toConsume
                    expectedLeft = it - toConsume
                    toConsume
                }
            )
            val parent = object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    // we should get in post scroll as much as left in controller callback
                    assertThat(available.x).isEqualTo(-expectedLeft)
                    return available
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    reverseDirection = true,
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
                    durationMillis = 300,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_reversed_proxiesPostFling() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            val velocityFlung = 5000f
            val controller = ScrollableState(
                consumeScrollDelta = {
                    value += it
                    it
                }
            )
            val parent = object : NestedScrollConnection {
                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    val expected = velocityFlung - consumed.x
                    assertThat(consumed.x).isLessThan(velocityFlung)
                    assertThat(abs(available.x - expected)).isLessThan(0.1f)
                    return available
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    reverseDirection = true,
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
                    durationMillis = 300,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollAbove_velocityAccountsForPreConsumption() =
        runBlockingWithManualClock { clock ->
            var value = 0f
            val velocityFlung = 5000f
            val controller = ScrollableState(
                consumeScrollDelta = {
                    value += it
                    it
                }
            )
            val parent = object : NestedScrollConnection {

                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    // consume everything
                    return available
                }
            }
            val assertionFlingBehavior = object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    // velocity should be zero (or almost) as we consumed everything in the pre
                    // cycle
                    assertThat(abs(initialVelocity)).isLessThan(1f)
                    return 0f
                }
            }

            rule.setContent {
                Box {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(300.dp)
                            .nestedScroll(parent)
                    ) {
                        Box(
                            modifier = Modifier.size(300.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    state = controller,
                                    orientation = Orientation.Horizontal,
                                    flingBehavior = assertionFlingBehavior
                                )
                        )
                    }
                }
            }

            rule.onNodeWithTag(scrollableBoxTag).performGesture {
                this.swipeWithVelocity(
                    start = this.center,
                    end = Offset(this.center.x + 500f, this.center.y),
                    durationMillis = 300,
                    endVelocity = velocityFlung
                )
            }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            // all assertions in callback above
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScrollBelow_listensDispatches() = runBlocking(AutoTestFrameClock()) {
        var value = 0f
        var expectedConsumed = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                expectedConsumed = it * 0.3f
                value += expectedConsumed
                expectedConsumed
            }
        )
        val child = object : NestedScrollConnection {}
        val dispatcher = NestedScrollDispatcher()

        rule.setContent {
            Box {
                Box(
                    modifier = Modifier.size(300.dp)
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier.size(200.dp)
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
            value
        }

        val preFlingConsumed = dispatcher
            .dispatchPreFling(Velocity(50f, 50f))
        rule.runOnIdle {
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }

        dispatcher.dispatchPostFling(
            Velocity(1000f, 1000f),
            Velocity(2000f, 2000f)
        )

        rule.runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value).isGreaterThan(lastValueBeforeFling)
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScroll_allowParentWhenDisabled() =
        runBlockingWithManualClock { clock ->
            var childValue = 0f
            var parentValue = 0f
            val childController = ScrollableState(
                consumeScrollDelta = {
                    childValue += it
                    it
                }
            )
            val parentController = ScrollableState(
                consumeScrollDelta = {
                    parentValue += it
                    it
                }
            )

            rule.setContent {
                Box {
                    Box(
                        modifier = Modifier.size(300.dp)
                            .scrollable(
                                state = parentController,
                                orientation = Orientation.Horizontal
                            )
                    ) {
                        Box(
                            Modifier.size(200.dp)
                                .testTag(scrollableBoxTag)
                                .scrollable(
                                    enabled = false,
                                    orientation = Orientation.Horizontal,
                                    state = childController
                                )
                        )
                    }
                }
            }

            rule.runOnIdle {
                assertThat(parentValue).isEqualTo(0f)
                assertThat(childValue).isEqualTo(0f)
            }

            rule.onNodeWithTag(scrollableBoxTag)
                .performGesture {
                    swipe(center, center.copy(x = center.x + 100f))
                }

            advanceClockWhileAwaitersExist(clock)
            advanceClockWhileAwaitersExist(clock)

            rule.runOnIdle {
                assertThat(childValue).isEqualTo(0f)
                assertThat(parentValue).isGreaterThan(0f)
            }
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_nestedScroll_disabledConnectionNoOp() =
        runBlockingWithManualClock { clock ->
            var childValue = 0f
            var parentValue = 0f
            var selfValue = 0f
            val childController = ScrollableState(
                consumeScrollDelta = {
                    childValue += it / 2
                    it / 2
                }
            )
            val middleController = ScrollableState(
                consumeScrollDelta = {
                    selfValue += it / 2
                    it / 2
                }
            )
            val parentController = ScrollableState(
                consumeScrollDelta = {
                    parentValue += it / 2
                    it / 2
                }
            )

            rule.setContent {
                Box {
                    Box(
                        modifier = Modifier.size(300.dp)
                            .scrollable(
                                state = parentController,
                                orientation = Orientation.Horizontal
                            )
                    ) {
                        Box(
                            Modifier.size(200.dp)
                                .scrollable(
                                    enabled = false,
                                    orientation = Orientation.Horizontal,
                                    state = middleController
                                )
                        ) {
                            Box(
                                Modifier.size(200.dp)
                                    .testTag(scrollableBoxTag)
                                    .scrollable(
                                        orientation = Orientation.Horizontal,
                                        state = childController
                                    )
                            )
                        }
                    }
                }
            }

            rule.runOnIdle {
                assertThat(parentValue).isEqualTo(0f)
                assertThat(selfValue).isEqualTo(0f)
                assertThat(childValue).isEqualTo(0f)
            }

            rule.onNodeWithTag(scrollableBoxTag)
                .performGesture {
                    swipe(center, center.copy(x = center.x + 100f))
                }

            advanceClockWhileAwaitersExist(clock)

            rule.runOnIdle {
                assertThat(childValue).isGreaterThan(0f)
                // disabled middle node doesn't consume
                assertThat(selfValue).isEqualTo(0f)
                // but allow nested scroll to propagate up correctly
                assertThat(parentValue).isGreaterThan(0f)
            }
        }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_interactionSource() = runBlocking {
        val interactionSource = MutableInteractionSource()
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        var scope: CoroutineScope? = null

        setScrollableContent {
            scope = rememberCoroutineScope()
            Modifier.scrollable(
                interactionSource = interactionSource,
                orientation = Orientation.Horizontal,
                state = controller
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                up()
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Stop::class.java)
            assertThat((interactions[1] as DragInteraction.Stop).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun scrollable_interactionSource_resetWhenDisposed() = runBlocking {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Box {
                if (emitScrollableBox) {
                    Box(
                        modifier = Modifier
                            .testTag(scrollableBoxTag)
                            .size(100.dp)
                            .scrollable(
                                interactionSource = interactionSource,
                                orientation = Orientation.Horizontal,
                                state = controller
                            )
                    )
                }
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performGesture {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        // Dispose scrollable
        rule.runOnIdle {
            emitScrollableBox = false
        }

        rule.runOnIdle {
            assertThat(interactions).hasSize(2)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
            assertThat(interactions[1]).isInstanceOf(DragInteraction.Cancel::class.java)
            assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_whenVelocity0() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                flingCalled++
                flingVelocity = initialVelocity
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isEqualTo(0f)
    }

    @Test
    fun scrollable_flingBehaviourCalled() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                flingCalled++
                flingVelocity = initialVelocity
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(5f).of(1000f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed() {
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        var flingCalled = 0
        var flingVelocity: Float = Float.MAX_VALUE
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                flingCalled++
                flingVelocity = initialVelocity
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                reverseDirection = true,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isWithin(5f).of(-1000f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_correctScope() {
        var total = 0f
        var returned = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                returned = scrollBy(123f)
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            up()
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed_correctScope() {
        var total = 0f
        var returned = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        val flingBehaviour = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                returned = scrollBy(123f)
                return 0f
            }
        }
        setScrollableContent {
            Modifier.scrollable(
                state = controller,
                reverseDirection = true,
                flingBehavior = flingBehaviour,
                orientation = Orientation.Horizontal
            )
        }
        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performGesture {
            up()
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun testInspectorValue() {
        val controller = ScrollableState(
            consumeScrollDelta = { it }
        )
        rule.setContent {
            val modifier = Modifier.scrollable(controller, Orientation.Vertical) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "state",
                "enabled",
                "reverseDirection",
                "flingBehavior",
                "interactionSource",
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
                        .size(100.dp).then(scrollable)
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
