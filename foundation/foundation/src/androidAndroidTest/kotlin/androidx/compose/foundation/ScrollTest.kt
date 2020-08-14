/*
 * Copyright 2019 The Android Open Source Project
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

import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.ExponentialDecay
import androidx.compose.animation.core.ManualAnimationClock
import androidx.compose.foundation.animation.FlingConfig
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.preferredHeight
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.ui.test.GestureScope
import androidx.ui.test.SemanticsNodeInteraction
import androidx.ui.test.StateRestorationTester
import androidx.ui.test.assertIsDisplayed
import androidx.ui.test.assertIsNotDisplayed
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.click
import androidx.ui.test.createComposeRule
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.onNodeWithText
import androidx.ui.test.performGesture
import androidx.ui.test.performScrollTo
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.test.swipeDown
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import androidx.ui.test.swipeUp
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ScrollTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val scrollerTag = "ScrollerTest"

    private val defaultCrossAxisSize = 45
    private val defaultMainAxisSize = 40
    private val defaultCellSize = 5

    private val colors = listOf(
        Color(red = 0xFF, green = 0, blue = 0, alpha = 0xFF),
        Color(red = 0xFF, green = 0xA5, blue = 0, alpha = 0xFF),
        Color(red = 0xFF, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0xA5, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0, green = 0xFF, blue = 0, alpha = 0xFF),
        Color(red = 0, green = 0xFF, blue = 0xA5, alpha = 0xFF),
        Color(red = 0, green = 0, blue = 0xFF, alpha = 0xFF),
        Color(red = 0xA5, green = 0, blue = 0xFF, alpha = 0xFF)
    )

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_SmallContent() {
        val height = 40

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @Test
    fun verticalScroller_SmallContent_Unscrollable() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        composeVerticalScroller(scrollState)

        runOnIdle {
            assertTrue(scrollState.maxValue == 0f)
        }
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_NoScroll() {
        val height = 30

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_ScrollToEnd() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val height = 30
        val scrollDistance = 10

        composeVerticalScroller(scrollState, height = height)

        validateVerticalScroller(height = height)

        runOnIdle {
            assertEquals(scrollDistance.toFloat(), scrollState.maxValue)
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateVerticalScroller(offset = scrollDistance, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_Reversed() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val height = 30
        val expectedOffset = defaultCellSize * colors.size - height

        composeVerticalScroller(scrollState, height = height, isReversed = true)

        validateVerticalScroller(offset = expectedOffset, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_Reversed_ScrollToEnd() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val height = 20
        val scrollDistance = 10
        val expectedOffset = defaultCellSize * colors.size - height - scrollDistance

        composeVerticalScroller(scrollState, height = height, isReversed = true)

        runOnIdle {
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateVerticalScroller(offset = expectedOffset, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_SmallContent() {
        val width = 40

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_SmallContent() {
        val width = 40

        composeHorizontalScroller(width = width, isRtl = true)

        validateHorizontalScroller(width = width, checkInRtl = true)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_NoScroll() {
        val width = 30

        composeHorizontalScroller(width = width)

        validateHorizontalScroller(width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_LargeContent_NoScroll() {
        val width = 30

        composeHorizontalScroller(width = width, isRtl = true)

        validateHorizontalScroller(width = width, checkInRtl = true)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        composeHorizontalScroller(scrollState, width = width)

        validateHorizontalScroller(width = width)

        runOnIdle {
            assertEquals(scrollDistance.toFloat(), scrollState.maxValue)
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateHorizontalScroller(offset = scrollDistance, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_LargeContent_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        composeHorizontalScroller(scrollState, width = width, isRtl = true)

        validateHorizontalScroller(width = width, checkInRtl = true)

        runOnIdle {
            assertEquals(scrollDistance.toFloat(), scrollState.maxValue)
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateHorizontalScroller(offset = scrollDistance, width = width, checkInRtl = true)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_reversed() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val width = 30
        val expectedOffset = defaultCellSize * colors.size - width

        composeHorizontalScroller(scrollState, width = width, isReversed = true)

        validateHorizontalScroller(offset = expectedOffset, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_reversed() {
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )
        val width = 30
        val expectedOffset = defaultCellSize * colors.size - width

        composeHorizontalScroller(scrollState, width = width, isReversed = true, isRtl = true)

        validateHorizontalScroller(offset = expectedOffset, width = width, checkInRtl = true)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_LargeContent_Reversed_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        val expectedOffset = defaultCellSize * colors.size - width - scrollDistance

        composeHorizontalScroller(scrollState, width = width, isReversed = true)

        runOnIdle {
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateHorizontalScroller(offset = expectedOffset, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_LargeContent_Reversed_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        )

        val expectedOffset = defaultCellSize * colors.size - width - scrollDistance

        composeHorizontalScroller(scrollState, width = width, isReversed = true, isRtl = true)

        runOnIdle {
            scrollState.scrollTo(scrollDistance.toFloat())
        }

        runOnIdle {} // Just so the block below is correct
        validateHorizontalScroller(offset = expectedOffset, width = width, checkInRtl = true)
    }

    @Test
    fun verticalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = true)

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = false)

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Ignore("Unignore when b/156389287 is fixed for proper reverse and rtl delegation")
    @Test
    fun horizontalScroller_rtl_scrollTo_scrollForward() {
        createScrollableContent(isVertical = false, isRtl = true)

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Ignore("Unignore when b/156389287 is fixed for proper reverse delegation")
    @Test
    fun verticalScroller_reversed_scrollTo_scrollForward() {
        createScrollableContent(
            isVertical = true,
            scrollState = ScrollState(
                initial = 0f,
                flingConfig = FlingConfig(ExponentialDecay()),
                animationClock = ManualAnimationClock(0)
            ),
            isReversed = true
        )

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Ignore("Unignore when b/156389287 is fixed for proper reverse and rtl delegation")
    @Test
    fun horizontalScroller_reversed_scrollTo_scrollForward() {
        createScrollableContent(
            isVertical = false,
            scrollState = ScrollState(
                initial = 0f,
                flingConfig = FlingConfig(ExponentialDecay()),
                animationClock = ManualAnimationClock(0)
            ),
            isReversed = true
        )

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Ignore("When b/157687898 is fixed, performScrollTo must be adjusted to use semantic bounds")
    fun verticalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = true)

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()

        onNodeWithText("20")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @Ignore("When b/157687898 is fixed, performScrollTo must be adjusted to use semantic bounds")
    fun horizontalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = false)

        onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()

        onNodeWithText("20")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verticalScroller_swipeUp_swipeDown() {
        swipeScrollerAndBack(true, GestureScope::swipeUp, GestureScope::swipeDown)
    }

    @Test
    fun horizontalScroller_swipeLeft_swipeRight() {
        swipeScrollerAndBack(false, GestureScope::swipeLeft, GestureScope::swipeRight)
    }

    @Test
    fun horizontalScroller_rtl_swipeLeft_swipeRight() {
        swipeScrollerAndBack(
            false,
            GestureScope::swipeRight,
            GestureScope::swipeLeft,
            isRtl = true
        )
    }

    @Test
    fun scroller_coerce_whenScrollTo() {
        val clock = ManualAnimationClock(0)
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )

        createScrollableContent(isVertical = true, scrollState = scrollState)

        runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
            assertThat(scrollState.maxValue).isGreaterThan(0f)
        }
        runOnUiThread {
            scrollState.scrollTo(-100f)
        }
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
        }
        runOnUiThread {
            scrollState.scrollBy(-100f)
        }
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
        }
        runOnUiThread {
            scrollState.scrollTo(scrollState.maxValue)
        }
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }
        runOnUiThread {
            scrollState.scrollTo(scrollState.maxValue + 1000)
        }
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }
        runOnUiThread {
            scrollState.scrollBy(100f)
        }
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
        }
    }

    @Test
    fun verticalScroller_LargeContent_coerceWhenMaxChanges() {
        val clock = ManualAnimationClock(0)
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )
        val itemCount = mutableStateOf(100)
        composeTestRule.setContent {
            Stack {
                ScrollableColumn(
                    scrollState = scrollState,
                    modifier = Modifier.preferredSize(100.dp).testTag(scrollerTag)
                ) {
                    for (i in 0..itemCount.value) {
                        Text(i.toString())
                    }
                }
            }
        }

        val max = runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
            assertThat(scrollState.maxValue).isGreaterThan(0f)
            scrollState.maxValue
        }

        runOnUiThread {
            scrollState.scrollTo(max)
        }
        runOnUiThread {
            itemCount.value -= 2
        }
        runOnIdle {
            val newMax = scrollState.maxValue
            assertThat(newMax).isLessThan(max)
            assertThat(scrollState.value).isEqualTo(newMax)
        }
    }

    @Test
    fun scroller_coerce_whenScrollSmoothTo() {
        val clock = ManualAnimationClock(0)
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )

        createScrollableContent(isVertical = true, scrollState = scrollState)

        val max = runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
            assertThat(scrollState.maxValue).isGreaterThan(0f)
            scrollState.maxValue
        }

        performWithAnimationWaitAndAssertPosition(0f, scrollState, clock) {
            scrollState.smoothScrollTo(-100f)
        }

        performWithAnimationWaitAndAssertPosition(0f, scrollState, clock) {
            scrollState.smoothScrollBy(-100f)
        }

        performWithAnimationWaitAndAssertPosition(max, scrollState, clock) {
            scrollState.smoothScrollTo(scrollState.maxValue)
        }

        performWithAnimationWaitAndAssertPosition(max, scrollState, clock) {
            scrollState.smoothScrollTo(scrollState.maxValue + 1000)
        }
        performWithAnimationWaitAndAssertPosition(max, scrollState, clock) {
            scrollState.smoothScrollBy(100f)
        }
    }

    @Test
    fun scroller_whenFling_stopsByTouchDown() {
        val clock = ManualAnimationClock(0)
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )

        createScrollableContent(isVertical = true, scrollState = scrollState)

        runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
            assertThat(scrollState.isAnimationRunning).isEqualTo(false)
        }

        onNodeWithTag(scrollerTag)
            .performGesture { swipeUp() }

        runOnIdle {
            clock.clockTimeMillis += 100
            assertThat(scrollState.isAnimationRunning).isEqualTo(true)
        }

        // TODO (matvei/jelle): this should be down, and not click to be 100% fair
        onNodeWithTag(scrollerTag)
            .performGesture { click() }

        runOnIdle {
            assertThat(scrollState.isAnimationRunning).isEqualTo(false)
        }
    }

    @Test
    fun scroller_restoresScrollerPosition() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var scrollState: ScrollState? = null

        restorationTester.setContent {
            scrollState = rememberScrollState()
            ScrollableColumn(scrollState = scrollState!!) {
                repeat(50) {
                    Box(Modifier.preferredHeight(100.dp))
                }
            }
        }

        runOnIdle {
            scrollState!!.scrollTo(70f)
            scrollState = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        runOnIdle {
            assertThat(scrollState!!.value).isEqualTo(70f)
        }
    }

    private fun performWithAnimationWaitAndAssertPosition(
        assertValue: Float,
        scrollState: ScrollState,
        clock: ManualAnimationClock,
        uiAction: () -> Unit
    ) {
        runOnUiThread {
            uiAction.invoke()
        }
        runOnIdle {
            clock.clockTimeMillis += 5000
        }

        onNodeWithTag(scrollerTag).awaitScrollAnimation(scrollState)
        runOnIdle {
            assertThat(scrollState.value).isEqualTo(assertValue)
        }
    }

    private fun swipeScrollerAndBack(
        isVertical: Boolean,
        firstSwipe: GestureScope.() -> Unit,
        secondSwipe: GestureScope.() -> Unit,
        isRtl: Boolean = false
    ) {
        val clock = ManualAnimationClock(0)
        val scrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = clock
        )

        createScrollableContent(isVertical, scrollState = scrollState, isRtl = isRtl)

        runOnIdle {
            assertThat(scrollState.value).isEqualTo(0f)
        }

        onNodeWithTag(scrollerTag)
            .performGesture { firstSwipe() }

        runOnIdle {
            clock.clockTimeMillis += 5000
        }

        onNodeWithTag(scrollerTag)
            .awaitScrollAnimation(scrollState)

        val scrolledValue = runOnIdle {
            scrollState.value
        }
        assertThat(scrolledValue).isGreaterThan(0f)

        onNodeWithTag(scrollerTag)
            .performGesture { secondSwipe() }

        runOnIdle {
            clock.clockTimeMillis += 5000
        }

        onNodeWithTag(scrollerTag)
            .awaitScrollAnimation(scrollState)

        runOnIdle {
            assertThat(scrollState.value).isLessThan(scrolledValue)
        }
    }

    private fun composeVerticalScroller(
        scrollState: ScrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        ),
        isReversed: Boolean = false,
        width: Int = defaultCrossAxisSize,
        height: Int = defaultMainAxisSize,
        rowHeight: Int = defaultCellSize
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                Stack {
                    ScrollableColumn(
                        scrollState = scrollState,
                        reverseScrollDirection = isReversed,
                        modifier = Modifier
                            .preferredSize(width.toDp(), height.toDp())
                            .testTag(scrollerTag)
                    ) {
                        colors.forEach { color ->
                            Box(
                                Modifier.preferredSize(width.toDp(), rowHeight.toDp()),
                                backgroundColor = color
                            )
                        }
                    }
                }
            }
        }
    }

    private fun composeHorizontalScroller(
        scrollState: ScrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        ),
        isReversed: Boolean = false,
        width: Int = defaultMainAxisSize,
        height: Int = defaultCrossAxisSize,
        isRtl: Boolean = false
    ) {
        // We assume that the height of the device is more than 45 px
        with(composeTestRule.density) {
            composeTestRule.setContent {
                val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                Providers(LayoutDirectionAmbient provides direction) {
                    Stack {
                        ScrollableRow(
                            reverseScrollDirection = isReversed,
                            scrollState = scrollState,
                            modifier = Modifier
                                .preferredSize(width.toDp(), height.toDp())
                                .testTag(scrollerTag)
                        ) {
                            colors.forEach { color ->
                                Box(
                                    Modifier.preferredSize(defaultCellSize.toDp(), height.toDp()),
                                    backgroundColor = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(api = 26)
    private fun validateVerticalScroller(
        offset: Int = 0,
        width: Int = 45,
        height: Int = 40,
        rowHeight: Int = 5
    ) {
        onNodeWithTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(width, height)) { pos ->
                val colorIndex = (offset + pos.y) / rowHeight
                colors[colorIndex]
            }
    }

    @RequiresApi(api = 26)
    private fun validateHorizontalScroller(
        offset: Int = 0,
        width: Int = 40,
        height: Int = 45,
        checkInRtl: Boolean = false
    ) {
        val scrollerWidth = colors.size * defaultCellSize
        val absoluteOffset = if (checkInRtl) scrollerWidth - width - offset else offset
        onNodeWithTag(scrollerTag)
            .captureToBitmap()
            .assertPixels(expectedSize = IntSize(width, height)) { pos ->
                val colorIndex = (absoluteOffset + pos.x) / defaultCellSize
                if (checkInRtl) colors[colors.size - 1 - colorIndex] else colors[colorIndex]
            }
    }

    private fun createScrollableContent(
        isVertical: Boolean,
        itemCount: Int = 100,
        width: Dp = 100.dp,
        height: Dp = 100.dp,
        isReversed: Boolean = false,
        scrollState: ScrollState = ScrollState(
            initial = 0f,
            flingConfig = FlingConfig(ExponentialDecay()),
            animationClock = ManualAnimationClock(0)
        ),
        isRtl: Boolean = false
    ) {
        composeTestRule.setContent {
            val content = @Composable {
                repeat(itemCount) {
                    Text(text = "$it")
                }
            }
            Stack {
                Box(
                    Modifier.preferredSize(width, height),
                    backgroundColor = Color.White
                ) {
                    if (isVertical) {
                        Box(Modifier.testTag(scrollerTag)) {
                            ScrollableColumn(
                                scrollState = scrollState,
                                reverseScrollDirection = isReversed
                            ) {
                                content()
                            }
                        }
                    } else {
                        val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        Providers(LayoutDirectionAmbient provides direction) {
                            Box(Modifier.testTag(scrollerTag)) {
                                ScrollableRow(
                                    scrollState = scrollState,
                                    reverseScrollDirection = isReversed
                                ) {
                                    content()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO(b/147291885): This should not be needed in the future.
    private fun SemanticsNodeInteraction.awaitScrollAnimation(
        scroller: ScrollState
    ): SemanticsNodeInteraction {
        val latch = CountDownLatch(1)
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                if (scroller.isAnimationRunning) {
                    handler.post(this)
                } else {
                    latch.countDown()
                }
            }
        })
        assertWithMessage("Scroll didn't finish after 20 seconds")
            .that(latch.await(20, TimeUnit.SECONDS)).isTrue()
        return this
    }
}
