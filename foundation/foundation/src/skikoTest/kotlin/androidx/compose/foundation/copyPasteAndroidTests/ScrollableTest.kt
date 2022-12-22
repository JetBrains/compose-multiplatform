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

package androidx.compose.foundation.copyPasteAndroidTests

import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.assertThat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.containsExactly
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ModifierLocalScrollableContainer
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.hasSize
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isEmpty
import androidx.compose.foundation.isEqualTo
import androidx.compose.foundation.isFalse
import androidx.compose.foundation.isGreaterThan
import androidx.compose.foundation.isLessThan
import androidx.compose.foundation.isNull
import androidx.compose.foundation.isTrue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.materialize
import androidx.compose.ui.modifier.ModifierLocalConsumer
import androidx.compose.ui.modifier.ModifierLocalReadScope
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ScrollWheel
import androidx.compose.ui.test.SkikoComposeUiTest
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalTestApi::class)
class ScrollableTest {


    private val scrollableBoxTag = "scrollableBox"

    private lateinit var scope: CoroutineScope

    private fun SkikoComposeUiTest.setContentAndGetScope(content: @Composable () -> Unit) {
        setContent {
            val actualScope = rememberCoroutineScope()
            SideEffect { scope = actualScope }
            content()
        }
    }

    @BeforeTest
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @AfterTest
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @Test
    fun scrollable_horizontalScroll() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_mouseWheel() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_horizontalScroll_reverse() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        val lastTotal = runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x - 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_horizontalScroll_reverse_mouseWheel() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        val lastTotal = runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Horizontal)
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_mouseWheel() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = runOnIdle {
            assertThat(total).isGreaterThan(0)
            total
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_verticalScroll_reversed() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y + 100f),
                durationMillis = 100
            )
        }

        val lastTotal = runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x, this.center.y - 100f),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_verticalScroll_reversed_mouseWheel() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Vertical)
        }

        val lastTotal = runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }

        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-100f, ScrollWheel.Horizontal)
        }

        runOnIdle {
            assertThat(total).isEqualTo(lastTotal)
        }
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(100f, ScrollWheel.Vertical)
        }
        runOnIdle {
            assertThat(total).isLessThan(0.01f)
        }
    }

    @Test
    fun scrollable_disabledWontCallLambda() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        val prevTotal = runOnIdle {
            assertThat(total).isGreaterThan(0f)
            enabled.value = false
            total
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 100f, this.center.y),
                durationMillis = 100
            )
        }
        runOnIdle {
            assertThat(total).isEqualTo(prevTotal)
        }
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_startWithoutSlop_ifFlinging() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        val expected = prevAfterSomeFling + 115
        assertThat(total).isEqualTo(expected)
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_blocksDownEvents_ifFlingingCaught() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setContent {
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
                                error("Clickable shouldn't click when fling caught")
                            }
                    )
                }
            }
        }
        onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        val prevAfterSomeFling = total
        assertThat(prevAfterSomeFling).isGreaterThan(prev)
        // don't advance main clock anymore since we're in the middle of the fling. Now interrupt
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            up()
        }
        // shouldn't assert in clickable lambda
    }

    @Test
    fun scrollable_snappingScrolling() = runSkikoComposeUiTest {
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
        waitForIdle()
        assertThat(total).isEqualTo(0f)

        scope.launch {
            controller.animateScrollBy(1000f)
        }
        waitForIdle()
        assertThat(total).isEqualTo(1000f, 0.001f)

        scope.launch {
            controller.animateScrollBy(-200f)
        }
        waitForIdle()
        assertThat(total).isEqualTo(800f, 0.001f)
    }

    @Test
    fun scrollable_explicitDisposal() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false
        val emit = mutableStateOf(true)
        val expectEmission = mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                assertThat(expectEmission.value).isTrue()
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 100,
                endVelocity = 4000f
            )
        }
        assertThat(total).isGreaterThan(0f)

        // start the fling for a few frames
        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()
        // flip the emission
        runOnUiThread {
            emit.value = false
        }
        // propagate the emit flip and record the value
        mainClock.advanceTimeByFrame()
        val prevTotal = total
        // make sure we don't receive any deltas
        runOnUiThread {
            expectEmission.value = false
        }

        // pump the clock until idle
        mainClock.autoAdvance = true
        waitForIdle()

        // still same and didn't fail in onScrollConsumptionRequested.. lambda
        assertThat(total).isEqualTo(prevTotal)
    }

    @Test
    fun scrollable_nestedDrag() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300,
                endVelocity = 0f
            )
        }
        val lastEqualDrag = runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isGreaterThan(0f)
            // we consumed half delta in child, so exactly half should go to the parent
            assertThat(outerDrag).isEqualTo(innerDrag)
            innerDrag
        }
        runOnIdle {
            // values should be the same since no fling
            assertThat(innerDrag).isEqualTo(lastEqualDrag)
            assertThat(outerDrag).isEqualTo(lastEqualDrag)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun scrollable_nestedScroll_disabledForMouseWheel() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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
        onNodeWithTag(scrollableBoxTag).performMouseInput {
            this.scroll(-200f, ScrollWheel.Horizontal)
        }
        runOnIdle {
            assertThat(innerDrag).isGreaterThan(0f)
            assertThat(outerDrag).isEqualTo(0f)
            innerDrag
        }
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_nestedFling() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
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
        runOnIdle {
            assertThat(innerDrag).isGreaterThan(lastEqualDrag)
            assertThat(outerDrag).isGreaterThan(lastEqualDrag)
        }
    }

    @Test
    fun scrollable_nestedScrollAbove_respectsPreConsumption() = runSkikoComposeUiTest {
        var value = 0f
        var lastReceivedPreScrollAvailable = 0f
        val preConsumeFraction = 0.7f
        val controller = ScrollableState(
            consumeScrollDelta = {
                val expected = lastReceivedPreScrollAvailable * (1 - preConsumeFraction)
                assertThat(it).isEqualTo(expected, 0.01f)
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

        setContentAndGetScope {
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

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipe(
                start = this.center,
                end = Offset(this.center.x + 200f, this.center.y),
                durationMillis = 300
            )
        }

        val preFlingValue = runOnIdle { value }
        runOnIdle {
            // if scrollable respects pre-fling consumption, it should fling 0px since we
            // pre-consume all
            assertThat(preFlingValue).isEqualTo(value)
        }
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_nestedScrollAbove_proxiesPostCycles() = runSkikoComposeUiTest {
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
                return if (source == NestedScrollSource.Fling) Offset.Zero else available
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

        setContentAndGetScope {
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

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_nestedScrollAbove_reversed_proxiesPostCycles() = runSkikoComposeUiTest {
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
                return if (source == NestedScrollSource.Fling) Offset.Zero else available
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

        setContentAndGetScope {
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

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    fun scrollable_nestedScrollBelow_listensDispatches() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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

        val lastValueBeforeFling = runOnIdle {
            val preScrollConsumed = dispatcher
                .dispatchPreScroll(Offset(20f, 20f), NestedScrollSource.Drag)
            // scrollable is not interested in pre scroll
            assertThat(preScrollConsumed).isEqualTo(Offset.Zero)

            val consumed = dispatcher.dispatchPostScroll(
                Offset(20f, 20f),
                Offset(50f, 50f),
                NestedScrollSource.Drag
            )
            assertThat(consumed.x).isEqualTo(expectedConsumed, 0.001f)
            value
        }

        scope.launch {
            val preFlingConsumed = dispatcher.dispatchPreFling(Velocity(50f, 50f))
            // scrollable won't participate in the pre fling
            assertThat(preFlingConsumed).isEqualTo(Velocity.Zero)
        }
        waitForIdle()

        scope.launch {
            dispatcher.dispatchPostFling(
                Velocity(1000f, 1000f),
                Velocity(2000f, 2000f)
            )
        }

        runOnIdle {
            // catch that scrollable caught our post fling and flung
            assertThat(value).isGreaterThan(lastValueBeforeFling)
        }
    }

    @Test
    fun scrollable_nestedScroll_allowParentWhenDisabled() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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

        runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        runOnIdle {
            assertThat(childValue).isEqualTo(0f)
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    fun scrollable_nestedScroll_disabledConnectionNoOp() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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

        runOnIdle {
            assertThat(parentValue).isEqualTo(0f)
            assertThat(selfValue).isEqualTo(0f)
            assertThat(childValue).isEqualTo(0f)
        }

        onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                swipe(center, center.copy(x = center.x + 100f))
            }

        runOnIdle {
            assertThat(childValue).isGreaterThan(0f)
            // disabled middle node doesn't consume
            assertThat(selfValue).isEqualTo(0f)
            // but allow nested scroll to propagate up correctly
            assertThat(parentValue).isGreaterThan(0f)
        }
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_bothOrientations_proxiesPostFling() = runSkikoComposeUiTest {
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
                assertThat(available.x).isEqualTo(velocityFlung, 0.1f)
                return available
            }
        }

        setContentAndGetScope {
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

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            this.swipeWithVelocity(
                start = this.center,
                end = Offset(this.center.x + 500f, this.center.y),
                durationMillis = 300,
                endVelocity = velocityFlung
            )
        }

        // all assertions in callback above
        waitForIdle()
    }

    @Test
    fun scrollable_interactionSource() = runSkikoComposeUiTest {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions[0] is DragInteraction.Start }
        }

        onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                up()
            }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions[0] is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Stop }
            assertThat((interactions[1] as DragInteraction.Stop).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_interactionSource_resetWhenDisposed() = runSkikoComposeUiTest {
        val interactionSource = MutableInteractionSource()
        var emitScrollableBox by mutableStateOf(true)
        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )

        setContentAndGetScope {
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

        runOnIdle {
            assertThat(interactions).isEmpty()
        }

        onNodeWithTag(scrollableBoxTag)
            .performTouchInput {
                down(Offset(visibleSize.width / 4f, visibleSize.height / 2f))
                moveBy(Offset(visibleSize.width / 2f, 0f))
            }

        runOnIdle {
            assertThat(interactions).hasSize(1)
            assertTrue { interactions[0] is DragInteraction.Start }
        }

        // Dispose scrollable
        runOnIdle {
            emitScrollableBox = false
        }

        runOnIdle {
            assertThat(interactions).hasSize(2)
            assertTrue { interactions[0] is DragInteraction.Start }
            assertTrue { interactions[1] is DragInteraction.Cancel }
            assertThat((interactions[1] as DragInteraction.Cancel).start)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_whenVelocity0() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(this.center)
            moveBy(Offset(115f, 0f))
            up()
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isLessThan(0.01f)
        assertThat(flingVelocity).isGreaterThan(-0.01f)
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_flingBehaviourCalled() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isEqualTo(1000f, 5f)
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun scrollable_flingBehaviourCalled_reversed() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeWithVelocity(
                this.center,
                this.center + Offset(115f, 0f),
                endVelocity = 1000f
            )
        }
        assertThat(flingCalled).isEqualTo(1)
        assertThat(flingVelocity).isEqualTo(-1000f, 5f)
    }

    @Test
    fun scrollable_flingBehaviourCalled_correctScope() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = runOnIdle {
            assertThat(total).isGreaterThan(0f)
            total
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            up()
        }

        runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_flingBehaviourCalled_reversed_correctScope() = runSkikoComposeUiTest {
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
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            down(center)
            moveBy(Offset(x = 100f, y = 0f))
        }

        val prevTotal = runOnIdle {
            assertThat(total).isLessThan(0f)
            total
        }

        onNodeWithTag(scrollableBoxTag).performTouchInput {
            up()
        }

        runOnIdle {
            assertThat(total).isEqualTo(prevTotal + 123)
            assertThat(returned).isEqualTo(123f)
        }
    }

    @Test
    fun scrollable_setsModifierLocalScrollableContainer() = runSkikoComposeUiTest {
        val controller = ScrollableState { it }

        var isOuterInScrollableContainer: Boolean? = null
        var isInnerInScrollableContainer: Boolean? = null
        setContent {
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

        runOnIdle {
            assertThat(isOuterInScrollableContainer).isFalse()
            assertThat(isInnerInScrollableContainer).isTrue()
        }
    }

    @Test
    fun scrollable_scrollByWorksWithRepeatableAnimations() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        runOnIdle {
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

        mainClock.advanceTimeBy(250)
        runOnIdle {
            // in the middle of the first animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 750 ms
        runOnIdle {
            // first animation finished
            assertThat(total).isEqualTo(100f)
        }

        mainClock.advanceTimeBy(250) // 1250 ms
        runOnIdle {
            // in the middle of the second animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 1750 ms
        runOnIdle {
            // second animation finished
            assertThat(total).isEqualTo(0f)
        }

        mainClock.advanceTimeBy(500) // 2250 ms
        runOnIdle {
            // in the middle of the third animation
            assertThat(total).isGreaterThan(0f)
            assertThat(total).isLessThan(100f)
        }

        mainClock.advanceTimeBy(500) // 2750 ms
        runOnIdle {
            // third animation finished
            assertThat(total).isEqualTo(100f)
        }
    }

    @Test
    fun scrollable_cancellingAnimateScrollUpdatesIsScrollInProgress() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setContentAndGetScope {
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

        runOnIdle {
            animateJob = scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        mainClock.advanceTimeBy(500)
        runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        // Stop halfway through the animation
        animateJob.cancel()

        runOnIdle {
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    @Ignore // TODO: test is stuck on desktop
    fun scrollable_preemptingAnimateScrollUpdatesIsScrollInProgress() = runSkikoComposeUiTest {
        mainClock.autoAdvance = false

        var total = 0f
        val controller = ScrollableState(
            consumeScrollDelta = {
                total += it
                it
            }
        )
        setContentAndGetScope {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scrollable(
                        state = controller,
                        orientation = Orientation.Horizontal
                    )
            )
        }

        runOnIdle {
            scope.launch {
                controller.animateScrollBy(
                    100f,
                    tween(1000)
                )
            }
        }

        mainClock.advanceTimeBy(500)
        runOnIdle {
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

        runOnIdle {
            assertThat(controller.isScrollInProgress).isTrue()
        }

        mainClock.advanceTimeBy(1000)
        mainClock.advanceTimeByFrame()

        runOnIdle {
            assertThat(total).isGreaterThan(-75f)
            assertThat(total).isLessThan(0f)
            assertThat(controller.isScrollInProgress).isFalse()
        }
    }

    @Test
    fun scrollable_multiDirectionsShouldPropagateOrthogonalAxisToNextParentWithSameDirection() = runSkikoComposeUiTest {
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

        setContentAndGetScope {
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

        onNodeWithTag("innerScrollable").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }

        runOnIdle {
            assertThat(innerDelta).isGreaterThan(0)
            assertThat(middleDelta).isEqualTo(0f)
            assertThat(outerDelta).isEqualTo(innerDelta / 2f)
        }
    }

    @Test
    @Ignore // TODO: test failing on desktop
    fun nestedScrollable_shouldImmediateScrollIfChildIsFlinging() = runSkikoComposeUiTest {
        var innerDelta = 0f
        var middleDelta = 0f
        var outerDelta = 0f
        var touchSlop = 0f

        val outerStateController = ScrollableState {
            outerDelta += it
            0f
        }

        val middleController = ScrollableState {
            middleDelta += it
            0f
        }

        val innerController = ScrollableState {
            innerDelta += it
            it / 2f
        }

        setContentAndGetScope {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(
                modifier = Modifier
                    .testTag("outerScrollable")
                    .size(600.dp)
                    .background(Color.Red)
                    .scrollable(
                        outerStateController,
                        orientation = Orientation.Vertical
                    ),
                contentAlignment = Alignment.BottomStart
            ) {
                Box(
                    modifier = Modifier
                        .testTag("middleScrollable")
                        .size(300.dp)
                        .background(Color.Blue)
                        .scrollable(
                            middleController,
                            orientation = Orientation.Vertical
                        ),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = Modifier
                            .testTag("innerScrollable")
                            .size(50.dp)
                            .background(Color.Yellow)
                            .scrollable(
                                innerController,
                                orientation = Orientation.Vertical
                            )
                    )
                }
            }
        }

        mainClock.autoAdvance = false
        onNodeWithTag("innerScrollable").performTouchInput {
            swipeUp()
        }

        mainClock.advanceTimeByFrame()
        mainClock.advanceTimeByFrame()

        val previousOuter = outerDelta

        onNodeWithTag("outerScrollable").performTouchInput {
            down(topCenter)
            // Move less than touch slop, should start immediately
            moveBy(Offset(0f, touchSlop / 2))
        }

        mainClock.autoAdvance = true

        runOnIdle {
            assertThat(outerDelta).isEqualTo(previousOuter + touchSlop / 2)
        }
    }

    // b/179417109 Double checks that in a nested scroll cycle, the parent post scroll
    // consumption is taken into consideration.
    @Test
    @Ignore // TODO: test failing on desktop
    fun dispatchScroll_shouldReturnConsumedDeltaInNestedScrollChain() = runSkikoComposeUiTest {
        var consumedInner = 0f
        var consumedOuter = 0f
        var touchSlop = 0f

        var preScrollAvailable = Offset.Zero
        var consumedPostScroll = Offset.Zero
        var postScrollAvailable = Offset.Zero

        val outerStateController = ScrollableState {
            consumedOuter += it
            it
        }

        val innerController = ScrollableState {
            consumedInner += it / 2
            it / 2
        }

        val connection = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                preScrollAvailable += available
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                consumedPostScroll += consumed
                postScrollAvailable += available
                return Offset.Zero
            }
        }

        setContent {
            touchSlop = LocalViewConfiguration.current.touchSlop
            Box(modifier = Modifier.nestedScroll(connection)) {
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

        val scrollDelta = 200f

        onRoot().performTouchInput {
            down(center)
            moveBy(Offset(scrollDelta, 0f))
            up()
        }

        runOnIdle {
            assertThat(consumedInner).isGreaterThan(0f)
            assertThat(consumedOuter).isGreaterThan(0f)
            assertThat(touchSlop).isGreaterThan(0f)
            assertThat(postScrollAvailable.x).isEqualTo(0f)
            assertThat(consumedPostScroll.x).isEqualTo(scrollDelta - touchSlop)
            assertThat(preScrollAvailable.x).isEqualTo(scrollDelta - touchSlop)
            assertThat(scrollDelta).isEqualTo(consumedInner + consumedOuter + touchSlop)
        }
    }

    @Test
    fun testInspectorValue() = runSkikoComposeUiTest {
        val controller = ScrollableState(
            consumeScrollDelta = { it }
        )
        setContentAndGetScope {
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
    fun producingEqualMaterializedModifierAfterRecomposition() = runSkikoComposeUiTest {
        val state = ScrollableState { it }
        val counter = mutableStateOf(0)
        var materialized: Modifier? = null

        setContent {
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
        runOnIdle {
            first = requireNotNull(materialized)
            materialized = null
            counter.value++
        }

        runOnIdle {
            val second = requireNotNull(materialized)
            assertThat(first).isEqualTo(second)
        }
    }

    @Test
    fun focusStaysInScrollableEvenThoughThereIsACloserItemOutside() = runSkikoComposeUiTest {
        lateinit var focusManager: FocusManager
        val initialFocus = FocusRequester()
        var nextItemIsFocused = false
        setContent {
            focusManager = LocalFocusManager.current
            Column {
                Column(
                    Modifier
                        .size(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
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
                            .focusable())
                }
                Box(
                    Modifier
                        .size(10.dp)
                        .focusable()
                )
            }
        }

        runOnIdle { initialFocus.requestFocus() }
        runOnIdle { focusManager.moveFocus(FocusDirection.Down) }

        runOnIdle { assertThat(nextItemIsFocused).isTrue() }
    }

    @Test
    fun verticalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() = runSkikoComposeUiTest {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier
                .pointerInput(Unit) {
                    savePointerInputEvents(tracker, this)
                }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Vertical)
        }

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeUp()
        }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeDown()
        }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).y)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    @Test
    fun horizontalScrollable_assertVelocityCalculationIsSimilarInsideOutsideVelocityTracker() = runSkikoComposeUiTest {
        // arrange
        val tracker = VelocityTracker()
        var velocity = Velocity.Zero
        val capturingScrollConnection = object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                velocity += available
                return Velocity.Zero
            }
        }
        val controller = ScrollableState { _ -> 0f }

        setScrollableContent {
            Modifier
                .pointerInput(Unit) {
                    savePointerInputEvents(tracker, this)
                }
                .nestedScroll(capturingScrollConnection)
                .scrollable(controller, Orientation.Horizontal)
        }

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeLeft()
        }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
        tracker.resetTracking()
        velocity = Velocity.Zero

        // act
        onNodeWithTag(scrollableBoxTag).performTouchInput {
            swipeRight()
        }

        // assert
        runOnIdle {
            val diff = abs((velocity - tracker.calculateVelocity()).x)
            assertThat(diff).isLessThan(VelocityTrackerCalculationThreshold)
        }
    }

    private fun SkikoComposeUiTest.setScrollableContent(scrollableModifierFactory: @Composable () -> Modifier) {
        setContentAndGetScope {
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
        source: NestedScrollSource
    ): Offset = Offset.Zero

    override fun consumePostScroll(
        initialDragDelta: Offset,
        overscrollDelta: Offset,
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

// Very low tolerance on the difference
internal val VelocityTrackerCalculationThreshold = 1

@OptIn(ExperimentalComposeUiApi::class)
internal suspend fun savePointerInputEvents(
    tracker: VelocityTracker,
    pointerInputScope: PointerInputScope
) {
    with(pointerInputScope) {
        coroutineScope {
            awaitPointerEventScope {
                while (true) {
                    var event = awaitFirstDown()
                    tracker.addPosition(event.uptimeMillis, event.position)
                    while (!event.changedToUpIgnoreConsumed()) {
                        val currentEvent = awaitPointerEvent().changes
                            .firstOrNull()

                        if (currentEvent != null && !currentEvent.changedToUpIgnoreConsumed()) {
                            currentEvent.historical.fastForEach {
                                tracker.addPosition(it.uptimeMillis, it.position)
                            }
                            tracker.addPosition(
                                currentEvent.uptimeMillis,
                                currentEvent.position
                            )
                            event = currentEvent
                        }
                    }
                }
            }
        }
    }
}
