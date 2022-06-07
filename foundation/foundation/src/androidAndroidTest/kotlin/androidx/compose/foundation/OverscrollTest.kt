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

package androidx.compose.foundation

import android.os.Build
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.AnimationDurationScaleRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class OverscrollTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val animationScaleRule: AnimationDurationScaleRule =
        AnimationDurationScaleRule.createForAllTests(1f)

    private val boxTag = "box"

    @Test
    fun overscrollEffect_scrollable_drag() {
        testDrag(reverseDirection = false)
    }

    @Test
    fun overscrollEffect_scrollable_drag_reverseDirection() {
        // same asserts for `reverseDirection = true`, but that's the point
        // we don't want overscroll to depend on reverseLayout, it's coordinate-driven logic
        testDrag(reverseDirection = true)
    }

    @Test
    fun overscrollEffect_scrollable_fling() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            if (acummulatedScroll > 1000f) {
                0f
            } else {
                acummulatedScroll += delta
                delta
            }
        }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(
                center,
                Offset(centerX + 10800, centerY),
                endVelocity = 30000f
            )
        }

        rule.runOnIdle {
            assertThat(controller.lastVelocity.x).isGreaterThan(0f)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.Fling)
        }
    }

    @Test
    fun overscrollEffect_scrollable_preDrag_respectsConsumption() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val viewConfig = rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        var centerXAxis = 0f
        rule.onNodeWithTag(boxTag).performTouchInput {
            centerXAxis = centerX
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            val slop = viewConfig.touchSlop
            // since we consume 1/10 of the delta in the pre scroll during overscroll, expect 9/10
            assertThat(abs(acummulatedScroll - 1000f * 9 / 10)).isWithin(0.1f)

            assertThat(controller.preScrollDelta).isEqualTo(Offset(1000f - slop, 0f))
            assertThat(controller.preScrollPointerPosition?.x)
                .isEqualTo(centerXAxis + slop)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.Drag)
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            up()
        }
    }

    @Test
    fun overscrollEffect_scrollable_skipsDeltasIfDisabled() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val viewConfig = rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        var centerXAxis = 0f
        rule.onNodeWithTag(boxTag).performTouchInput {
            centerXAxis = centerX
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        val lastControlledConsumed = rule.runOnIdle {
            val slop = viewConfig.touchSlop
            // since we consume 1/10 of the delta in the pre scroll during overscroll, expect 9/10
            assertThat(abs(acummulatedScroll - 1000f * 9 / 10)).isWithin(0.1f)

            assertThat(controller.preScrollDelta).isEqualTo(Offset(1000f - slop, 0f))
            assertThat(controller.preScrollPointerPosition?.x)
                .isEqualTo(centerXAxis + slop)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.Drag)
            controller.isEnabled = false
            controller.preScrollDelta
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            centerXAxis = centerX
            down(center)
            moveBy(Offset(1000f, 0f))
            up()
        }

        rule.runOnIdle {
            // still there because we are disabled
            assertThat(controller.preScrollDelta).isEqualTo(lastControlledConsumed)
        }
    }

    @Test
    fun overscrollEffect_scrollable_preFling_respectsConsumption() {
        var acummulatedScroll = 0f
        var lastFlingReceived = 0f
        val controller = TestOverscrollEffect(consumePreCycles = true)
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val flingBehavior = object : FlingBehavior {
            override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                lastFlingReceived = initialVelocity
                return initialVelocity
            }
        }
        rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller,
            flingBehavior = flingBehavior
        )

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        rule.onNodeWithTag(boxTag).assertExists()

        rule.onNodeWithTag(boxTag).performTouchInput {
            swipeWithVelocity(
                center,
                Offset(centerX + 10800, centerY),
                endVelocity = 30000f
            )
        }

        rule.runOnIdle {
            assertThat(abs(controller.preFlingVelocity.x - 30000f)).isWithin(0.1f)
            assertThat(abs(lastFlingReceived - 30000f * 9 / 10)).isWithin(0.1f)
        }
    }

    @Test
    fun overscrollEffect_scrollable_attemptsToStopAnimation() {
        var acummulatedScroll = 0f
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            acummulatedScroll += delta
            delta
        }
        val viewConfiguration = rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller
        )

        rule.runOnIdle {
            // no down events, hence 0 animation stops
            assertThat(controller.isInProgressCallCount).isEqualTo(0)
        }

        // This actually produces a velocity of 100,000 (500 px moved in 5 ms), but we are
        // relying on the compatibility behaviour that 2 points produce a zero velocity.
        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(500f, 0f))
            up()
        }

        val lastAccScroll = rule.runOnIdle {
            assertThat(controller.isInProgressCallCount).isEqualTo(1)
            // respect touch slop if overscroll animation is not running
            assertThat(acummulatedScroll)
                .isEqualTo(500f - viewConfiguration.touchSlop)
            // pretend we're settling the overscroll animation
            controller.animationRunning = true
            acummulatedScroll
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(500f, 0f))
            up()
        }

        // ignores touch slop if overscroll animation is on progress while pointer goes down
        assertThat(acummulatedScroll - lastAccScroll).isEqualTo(500f)

        rule.runOnIdle {
            assertThat(controller.isInProgressCallCount).isEqualTo(2)
        }
    }

    @Test
    fun modifierIsProducingEqualsModifiersForTheSameInput() {
        var overscrollEffect: OverscrollEffect? = null
        rule.setContent {
            overscrollEffect = AndroidEdgeEffectOverscrollEffect(
                LocalView.current.context,
                OverscrollConfiguration(Color.Gray)
            )
        }

        val first = Modifier.overscroll(overscrollEffect!!)
        val second = Modifier.overscroll(overscrollEffect!!)
        assertThat(first).isEqualTo(second)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun overscrollIsNotClippingTheContentWhenPulled() {
        // if we don't do it the overscroll effect will not even start.
        animationScaleRule.setAnimationDurationScale(1f)

        lateinit var controller: AndroidEdgeEffectOverscrollEffect
        val tag = "container"
        rule.setContent {
            Box {
                controller = rememberOverscrollEffect() as AndroidEdgeEffectOverscrollEffect
                Box(
                    Modifier
                        .background(Color.Red)
                        .testTag(tag)
                ) {
                    Box(
                        Modifier
                            .padding(horizontal = 10.dp)
                            .size(10.dp)
                            .clipScrollableContainer(Orientation.Vertical)
                            .overscroll(controller)
                            .drawBehind {
                                val extraOffset = 10.dp
                                    .roundToPx()
                                    .toFloat()
                                // we fill the whole parent container so we can test that
                                // there is no clipping
                                drawRect(
                                    Color.Green,
                                    Offset(-extraOffset, -extraOffset),
                                    size = Size(
                                        size.width + extraOffset * 2,
                                        size.height + extraOffset * 2
                                    )
                                )
                            }
                    )
                }
            }
        }

        rule.runOnIdle {
            controller.isEnabled = true
            val offset = Offset(0f, 5f)
            controller.consumePostScroll(
                initialDragDelta = offset,
                overscrollDelta = offset,
                pointerPosition = null,
                source = NestedScrollSource.Drag
            )
            // we have to disable further invalidation requests as otherwise while the overscroll
            // effect is considered active (as it is in a pulled state) this will infinitely
            // schedule next invalidation right from the drawing. this will make our test infra
            // to never be switched into idle state so this fill freeze instead of proceeding
            // to the next step in the test.
            controller.invalidationEnabled = false
        }

        rule.onNodeWithTag(tag)
            .captureToImage()
            // if there is not clipping then the red parent is not visible
            // but we also don't want to assert that the bg is Green as some overscroll
            // effects can draw something else on top of this plain green background
            .assertHasNoColor(Color.Red)
    }

    class TestOverscrollEffect(
        private val consumePreCycles: Boolean = false,
        var animationRunning: Boolean = false
    ) : OverscrollEffect {
        var drawCallsCount = 0
        var isInProgressCallCount = 0
        var isContentScrolls = true

        var lastVelocity = Velocity.Zero
        var lastInitialDragDelta = Offset.Zero
        var lastOverscrollDelta = Offset.Zero
        var lastPointerPosition: Offset? = Offset.Zero
        var lastNestedScrollSource: NestedScrollSource? = null

        var preScrollDelta = Offset.Zero
        var preScrollPointerPosition: Offset? = Offset.Zero
        var preScrollSource: NestedScrollSource? = null

        var preFlingVelocity = Velocity.Zero

        override fun consumePreScroll(
            scrollDelta: Offset,
            pointerPosition: Offset?,
            source: NestedScrollSource
        ): Offset {
            preScrollDelta = scrollDelta
            preScrollPointerPosition = pointerPosition
            preScrollSource = source

            return if (consumePreCycles) scrollDelta / 10f else Offset.Zero
        }

        override fun consumePostScroll(
            initialDragDelta: Offset,
            overscrollDelta: Offset,
            pointerPosition: Offset?,
            source: NestedScrollSource
        ) {
            lastInitialDragDelta = initialDragDelta
            lastOverscrollDelta = overscrollDelta
            lastPointerPosition = pointerPosition
            lastNestedScrollSource = source
        }

        override suspend fun consumePreFling(velocity: Velocity): Velocity {
            preFlingVelocity = velocity
            return if (consumePreCycles) velocity / 10f else Velocity.Zero
        }

        override suspend fun consumePostFling(velocity: Velocity) {
            lastVelocity = velocity
        }

        override val isInProgress: Boolean
            get() {
                isInProgressCallCount += 1
                return animationRunning
            }

        override var isEnabled: Boolean
            get() = isContentScrolls
            set(value) {
                isContentScrolls = value
            }

        override val effectModifier: Modifier = Modifier.drawBehind { drawCallsCount += 1 }
    }

    fun testDrag(reverseDirection: Boolean) {
        var consumeOnlyHalf = false
        val controller = TestOverscrollEffect()
        val scrollableState = ScrollableState { delta ->
            if (consumeOnlyHalf) {
                delta / 2
            } else {
                delta
            }
        }
        val viewConfig = rule.setOverscrollContentAndReturnViewConfig(
            scrollableState = scrollableState,
            overscrollEffect = controller,
            reverseDirection = reverseDirection
        )

        rule.runOnIdle {
            // we passed isContentScrolls = 1, so initial draw must occur
            assertThat(controller.drawCallsCount).isEqualTo(1)
        }

        rule.onNodeWithTag(boxTag).assertExists()

        var centerXAxis = 0f
        rule.onNodeWithTag(boxTag).performTouchInput {
            centerXAxis = centerX
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            assertThat(controller.lastInitialDragDelta.y).isZero()
            // there was only one pointer position coming from the center + 1000, let's check
            assertThat(controller.lastPointerPosition?.x)
                .isEqualTo(centerXAxis + viewConfig.touchSlop)
            // consuming all, so overscroll is 0
            assertThat(controller.lastOverscrollDelta).isEqualTo(Offset.Zero)
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            up()
        }

        rule.runOnIdle {
            consumeOnlyHalf = true
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            down(center)
            moveBy(Offset(1000f, 0f))
        }

        rule.runOnIdle {
            assertThat(controller.lastInitialDragDelta.x).isGreaterThan(0f)
            assertThat(controller.lastInitialDragDelta.y).isZero()
            assertThat(controller.lastOverscrollDelta.x)
                .isEqualTo(controller.lastInitialDragDelta.x / 2)
            assertThat(controller.lastNestedScrollSource).isEqualTo(NestedScrollSource.Drag)
        }

        rule.onNodeWithTag(boxTag).performTouchInput {
            up()
        }

        rule.runOnIdle {
            assertThat(controller.lastVelocity).isEqualTo(Velocity.Zero)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun ComposeContentTestRule.setOverscrollContentAndReturnViewConfig(
    scrollableState: ScrollableState,
    overscrollEffect: OverscrollEffect,
    flingBehavior: FlingBehavior? = null,
    reverseDirection: Boolean = false
): ViewConfiguration {
    var viewConfiguration: ViewConfiguration? = null
    setContent {
        viewConfiguration = LocalViewConfiguration.current
        Box {
            Box(
                Modifier
                    .testTag("box")
                    .size(300.dp)
                    .overscroll(overscrollEffect)
                    .scrollable(
                        state = scrollableState,
                        orientation = Orientation.Horizontal,
                        overscrollEffect = overscrollEffect,
                        flingBehavior = flingBehavior ?: ScrollableDefaults.flingBehavior(),
                        reverseDirection = reverseDirection
                    )
            )
        }
    }
    return viewConfiguration!!
}

private fun ImageBitmap.assertHasNoColor(color: Color) {
    val pixel = toPixelMap()
    for (x in 0 until width) {
        for (y in 0 until height) {
            assertWithMessage(
                "Pixel at [$x,$y] was equal to $color"
            ).that(pixel[x, y]).isNotEqualTo(color)
        }
    }
}
