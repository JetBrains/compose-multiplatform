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

import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ModifierLocalScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.materialize
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
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

    private lateinit var scope: CoroutineScope

    private val VelocityTrackerCalculationThreshold = 50

    private fun ComposeContentTestRule.setContentAndGetScope(content: @Composable () -> Unit) {
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }
            content()
        }
    }

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollable_horizontalScroll() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_mouseWheel() {
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
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_horizontalScroll_reverse() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_reverse_mouseWheel() {
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
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_mouseWheel() {
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
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll_reversed() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_reversed_mouseWheel() {
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
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        rule.runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_disabledWontCallLambda() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    fun scrollable_startWithoutSlop_ifFlinging() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        val expected = prevAfterSomeFling + 115
        assertThat(total).isEqualTo(expected)
    }

    @Test
    fun scrollable_blocksDownEvents_ifFlingingCaught() {
        rule.mainClock.autoAdvance = false
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContent {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            orientation = Orientation.Horizontal,
                            state = controller
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .clickable {
                                assertWithMessage("Clickable shouldn't click when fling caught")
                                    .fail()
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            up()
        }
        // shouldn't assert in clickable lambda
    }

    @Test
    fun scrollable_snappingScrolling() {
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
        rule.waitForIdle()
        assertThat(total).isEqualTo(0f)

        scope.launch {
            controller.animateScrollBy(1000f)
        }
        rule.waitForIdle()
        assertThat(total).isWithin(0.001f).of(1000f)

        scope.launch {
            controller.animateScrollBy(-200f)
        }
        rule.waitForIdle()
        assertThat(total).isWithin(0.001f).of(800f)
    }

    @Test
    fun scrollable_explicitDisposal() {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
    fun scrollable_nestedDrag() {
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

        rule.setContentAndGetScope {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.runOnIdle {
            // values should be the same since no fling
            assertThat(innerDrag).isEqualTo(lastEqualDrag)
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_nestedScroll_disabledForMouseWheel() {
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

        rule.setContentAndGetScope {
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
        rule.onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-200f, ScrollWheel.Horizontal)
        }
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isZero()
            innerDrag
        }
    }

    @Test
    fun scrollable_nestedFling() {
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

        rule.setContentAndGetScope {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.runOnIdle {
            assertThat(innerDrag).isGreaterThan(lastEqualDrag)
            assertThat(outerDrag).isGreaterThan(lastEqualDrag)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_respectsPreConsumption() {
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

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(preConsumingParent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }

        val preFlingValue = rule.runOnIdle { value }
        rule.runOnIdle {
            // if scrollable respects pre-fling consumption, it should fling 0px since we
            // pre-consume all
            assertThat(preFlingValue).isEqualTo(value)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_proxiesPostCycles() {
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

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                val expected = velocityFlung - consumed.x
                assertThat(abs(available.x - expected)).isLessThan(0.1f)
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = controller,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollAbove_reversed_proxiesPostCycles() {
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

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
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

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollBelow_listensDispatches() {
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

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
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

        scope.launch {
            val preFlingConsumed = dispatcher.dispatchPreFling(Velocity(50f, 50f))
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }
        rule.waitForIdle()

        scope.launch {
            dispatcher.dispatchPostFling(
                Velocity(1000f, 1000f),
                Velocity(2000f, 2000f)
            )
        }

        rule.runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value).isGreaterThan(lastValueBeforeFling)
        }
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() {
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

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = parentController,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
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
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        rule.runOnIdle {
            assertThat(childValue).isEqualTo(0f)
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedScroll_disabledConnectionNoOp() {
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

        rule.setContentAndGetScope {
            Box {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .scrollable(
                            state = parentController,
                            orientation = Orientation.Horizontal
                        )
                ) {
                    Box(
                        Modifier
                            .size(200.dp)
                            .scrollable(
                                enabled = false,
                                orientation = Orientation.Horizontal,
                                state = middleController
                            )
                    ) {
                        Box(
                            Modifier
                                .size(200.dp)
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
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        rule.runOnIdle {
            assertThat(childValue).isGreaterThan(0f)
            // disabled middle node doesn't consume
            assertThat(selfValue).isEqualTo(0f)
            // but allow nested scroll to propagate up correctly
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_bothOrientations_proxiesPostFling() {
        val velocityFlung = 5000f
        val outerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerState = ScrollableState(consumeScrollDelta = { 0f })
        val innerFlingBehavior = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                return initialVelocity
            }
        }
        val parent = object : NestedScrollConnection {
            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                assertThat(consumed.x).isEqualTo(0f)
                assertThat(available.x).isWithin(0.1f).of(velocityFlung)
                return available
            }
        }

        rule.setContentAndGetScope {
            Box {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(300.dp)
                        .nestedScroll(parent)
                        .scrollable(
                            state = outerState,
                            orientation = Orientation.Vertical
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .testTag(scrollableBoxTag)
                            .scrollable(
                                state = innerState,
                                flingBehavior = innerFlingBehavior,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        rule.waitForIdle()
    }

    @Test
    fun scrollable_interactionSource() {
        val interactionSource = MutableInteractionSource()
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        setScrollableContent {
            Modifier.scrollable(
                interactionSource = interactionSource,
                orientation = Orientation.Horizontal,
                state = controller
            )
        }

        val interactions = mutableListOf<Interaction>()

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        rule.runOnIdle {
            assertThat(interactions).hasSize(1)
            assertThat(interactions.first()).isInstanceOf(DragInteraction.Start::class.java)
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
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
    fun scrollable_interactionSource_resetWhenDisposed() {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        rule.setContentAndGetScope {
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

        scope.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isLessThan(0.01f)
        assertThat(flingVelocity).isGreaterThan(-0.01f)
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = rule.runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }

        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            up()
        }

        rule.runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_setsModifierLocalScrollableContainer() {
        val controller = ScrollableState { it }

        var isOuterInScrollableContainer: Boolean? = null
        var isInnerInScrollableContainer: Boolean? = null
        rule.setContent {
            Box {
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .then(
                            object : ModifierLocalConsumer {
                                override fun onModifierLocalsUpdated(
                                    scope: ModifierLocalReadScope
                                ) {
                                    with(scope) {
                                        isOuterInScrollableContainer =
                                            ModifierLocalScrollableContainer.current
                                    }
                                }
                            }
                        )
                        .scrollable(
                            state = controller,
                            orientation = Orientation.Horizontal
                        )
                        .then(
                            object : ModifierLocalConsumer {
                                override fun onModifierLocalsUpdated(
                                    scope: ModifierLocalReadScope
                                ) {
                                    with(scope) {
                                        isInnerInScrollableContainer =
                                            ModifierLocalScrollableContainer.current
                                    }
                                }
                            }
                        )
                )
            }
        }

        rule.runOnIdle {
            assertThat(isOuterInScrollableContainer).isFalse()
            assertThat(isInnerInScrollableContainer).isTrue()
        }
    }

    @Test
    fun scrollable_scrollByWorksWithRepeatableAnimations() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        rule.runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    100f,
                    keyframes {
                        durationMillis = 2500
                        // emulate a repeatable animation:
                        0f at 0
                        100f at 500
                        100f at 1000
                        0f at 1500
                        0f at 2000
                        100f at 2500
                    }
                )
            }
        }

        rule.mainClock.advanceTimeBy(250)
        rule.runOnIdle {
            // in the middle of the first animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 750 ms
        rule.runOnIdle {
            // first animation finished
            assertThat(total).isEqualTo(100)
        }

        rule.mainClock.advanceTimeBy(250) // 1250 ms
        rule.runOnIdle {
            // in the middle of the second animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 1750 ms
        rule.runOnIdle {
            // second animation finished
            assertThat(total).isEqualTo(0)
        }

        rule.mainClock.advanceTimeBy(500) // 2250 ms
        rule.runOnIdle {
            // in the middle of the third animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        rule.mainClock.advanceTimeBy(500) // 2750 ms
        rule.runOnIdle {
            // third animation finished
            assertThat(total).isEqualTo(100)
        }
    }

    @Test
    fun scrollable_cancellingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        lateinit var animateJob: Job

        rule.runOnIdle {
            animateJob = scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        // Stop halfway through the animation
        animateJob.cancel()

        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() {
        rule.mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        rule.runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        rule.mainClock.advanceTimeBy(500)
        rule.runOnIdle {
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
            assertThat(controller.isScrollInProgress).isTrue()
            scope.launch {
                controller.animateScrollBy(
                    -100f,
                    tween(1000)
                )
            }
        }

        rule.runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        rule.mainClock.advanceTimeBy(1000)
        rule.mainClock.advanceTimeByFrame()

        rule.runOnIdle {
            assertThat(total).isGreaterThan(-75f)
            assertThat(total).isLessThan(0f)
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_multiDirectionsShouldPropagateOrthogonalAxisToNextParentWithSameDirection() {
        var innerDelta = 0f
        var middleDelta = 0f
        var outerDelta = 0f

        val outerStateController = ScrollableState {
            outerDelta += it
            it
        }

        val middleController = ScrollableState {
            middleDelta += it
            it / 2
        }

        val innerController = ScrollableState {
            innerDelta += it
            it / 2
        }

        rule.setContentAndGetScope {
            Box(
                modifier = Modifier
                    .testTag("outerScrollable")
                    .size(300.dp)
                    .scrollable(
                        outerStateController,
                        orientation = Orientation.Horizontal
                    )

            ) {
                Box(
                    modifier = Modifier
                        .testTag("middleScrollable")
                        .size(300.dp)
                        .scrollable(
                            middleController,
                            orientation = Orientation.Vertical
                        )

                ) {
                    Box(
                        modifier = Modifier
                            .testTag("innerScrollable")
                            .size(300.dp)
                            .scrollable(
                                innerController,
                                orientation = Orientation.Horizontal
                            )
                    )
                }
            }
        }

        rule.onNodeWithTag("innerScrollable").performTouchInput {
            down(center)
            moveBy(Offset(this.center.x + 100f, this.center.y))
            up()
        }

        rule.runOnIdle {
            assertThat(innerDelta).isGreaterThan(0)
            assertThat(middleDelta).isEqualTo(0)
            assertThat(outerDelta).isEqualTo(innerDelta / 2f)
        }
    }

    @Test
    fun testInspectorValue() {
        val controller = ScrollableState(
            consumeScrollDelta = { it }
        )
        rule.setContentAndGetScope {
            val modifier = Modifier.scrollable(controller, Orientation.Vertical) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scrollable")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "orientation",
                "state",
                "overscrollEffect",
                "enabled",
                "reverseDirection",
                "flingBehavior",
                "interactionSource",
            )
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun producingEqualMaterializedModifierAfterRecomposition() {
        val state = ScrollableState { it }
        val counter = mutableStateOf(0)
        var materialized: Modifier? = null

        rule.setContent {
            counter.value // just to trigger recomposition
            materialized = currentComposer.materialize(
                Modifier.scrollable(
                    state,
                    Orientation.Vertical,
                    NoOpOverscrollEffect
                )
            )
        }

        lateinit var first: Modifier
        rule.runOnIdle {
            first = requireNotNull(materialized)
            materialized = null
            counter.value++
        }

        rule.runOnIdle {
            val second = requireNotNull(materialized)
            assertThat(first).isEqualTo(second)
        }
    }

    @Test
    fun focusStaysInScrollableEvenThoughThereIsACloserItemOutside() {
        lateinit var focusManager: FocusManager
        val initialFocus = FocusRequester()
        var nextItemIsFocused = false
        rule.setContent {
            focusManager = LocalFocusManager.current
            Column {
                Column(
                    Modifier
                        .size(10.dp)
                        .verticalScroll(rememberScrollState())) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .focusRequester(initialFocus)
                            .focusable()
                    )
                    Box(Modifier.size(10.dp))
                    Box(
                        Modifier
                            .size(10.dp)
                            .onFocusChanged { nextItemIsFocused = it.isFocused }
                            .focusable()
                    )
                }
                Box(
                    Modifier
                        .size(10.dp)
                        .focusable())
            }
        }

        rule.runOnIdle { initialFocus.requestFocus() }
        rule.runOnIdle { focusManager.moveFocus(FocusDirection.Down) }

        rule.runOnIdle { assertThat(nextItemIsFocused).isTrue() }
    }

    @Test
    fun verticalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = VelocityTracker()
        var initialTouchPosition = 0f
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { delta ->
            initialTouchPosition += delta
            tracker.addPosition(rule.mainClock.currentTime, Offset(0f, initialTouchPosition))
            0f
        }

        setScrollableContent {
            Modifier
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Vertical)
        }

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            tracker.addPosition(rule.mainClock.currentTime, Offset(0f, 0f))
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            tracker.addPosition(rule.mainClock.currentTime, Offset(0f, 0f))
            swipeDown()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun horizontalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() {
        // arrange
        val tracker = VelocityTracker()
        var initialTouchPosition = 0f
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { delta ->
            initialTouchPosition += delta
            tracker.addPosition(rule.mainClock.currentTime, Offset(initialTouchPosition, 0f))
            0f
        }

        setScrollableContent {
            Modifier
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Horizontal)
        }

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            tracker.addPosition(rule.mainClock.currentTime, Offset(0f, 0f))
            swipeLeft()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        rule.onNodeWithTag(scrollableBoxTag).performTouchInput {
            tracker.addPosition(rule.mainClock.currentTime, Offset(0f, 0f))
            swipeRight()
        }

        // assert
        rule.runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun offsetsScrollable_velocityCalculationShouldConsiderLocalPositions() {
        // arrange
        var velocity = Velocity.Zero
        val fullScreen = mutableStateOf(false)
        lateinit var scrollState: LazyListState
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        rule.setContent {
            scrollState = rememberLazyListState()
            Column(modifier = Modifier.nestedScroll(capturingScrollConnection)) {
                if (!fullScreen.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .height(400.dp)
                    )
                }

                LazyColumn(state = scrollState) {
                    items(100) {
                        Box(
                            modifier = Modifier
                                .padding(10.dp)
                                .background(Color.Red)
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        }
        // act
        // Register generated velocity with offset
        composeViewSwipeUp()
        rule.waitForIdle()
        val previousVelocity = velocity
        velocity = Velocity.Zero
        // Remove offset and restart scroll
        fullScreen.value = true
        rule.runOnIdle {
            runBlocking {
                scrollState.scrollToItem(0)
            }
        }
        rule.waitForIdle()
        // Register generated velocity without offset, should be larger as there was more
        // screen to cover.
        composeViewSwipeUp()

        // assert
        rule.runOnIdle {
            assertThat(abs(previousVelocity.y)).isNotEqualTo(abs(velocity.y))
        }
    }

    private fun composeViewSwipeUp() {
        onView(allOf(instanceOf(AbstractComposeView::class.java)))
            .perform(
                espressoSwipe(
                    GeneralLocation.BOTTOM_CENTER,
                    GeneralLocation.CENTER
                )
            )
    }

    private fun espressoSwipe(
        start: CoordinatesProvider,
        end: CoordinatesProvider
    ): GeneralSwipeAction {
        return GeneralSwipeAction(
            Swipe.FAST, start, end,
            Press.FINGER
        )
    }

    private fun setScrollableContent(scrollableModifierFactory: @Composable () -> Modifier) {
        rule.setContentAndGetScope {
            Box {
                val scrollable = scrollableModifierFactory()
                Box(
                    modifier = Modifier
                        .testTag(scrollableBoxTag)
                        .size(100.dp)
                        .then(scrollable)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val NoOpOverscrollEffect = object : OverscrollEffect {

    override fun consumePreScroll(
        scrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ): Offset = Offset.Zero

    override fun consumePostScroll(
        initialDragDelta: Offset,
        overscrollDelta: Offset,
        pointerPosition: Offset?,
        source: NestedScrollSource
    ) {
    }

    override suspend fun consumePreFling(velocity: Velocity): Velocity = Velocity.Zero

    override suspend fun consumePostFling(velocity: Velocity) {}

    override var isEnabled: Boolean = false

    override val isInProgress: Boolean
        get() = false

    override val effectModifier: Modifier get() = Modifier
}
