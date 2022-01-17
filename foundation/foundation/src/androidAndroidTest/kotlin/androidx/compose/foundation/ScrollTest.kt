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

import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ScrollTest {

    @get:Rule
    val rule = createComposeRule()

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

    private lateinit var scope: CoroutineScope

    @Composable
    private fun ExtractCoroutineScope() {
        val actualScope = rememberCoroutineScope()
        SideEffect { scope = actualScope }
    }

    @Before
    fun before() {
        isDebugInspectorInfoEnabled = true
    }

    @After
    fun after() {
        isDebugInspectorInfoEnabled = false
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_SmallContent() {
        val height = 40

        composeVerticalScroller(height = height)

        validateVerticalScroller(height = height)
    }

    @Test
    fun verticalScroller_SmallContent_Unscrollable() {
        val scrollState = ScrollState(initial = 0)

        composeVerticalScroller(scrollState)

        rule.runOnIdle {
            assertTrue(scrollState.maxValue == 0)
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
        val scrollState = ScrollState(initial = 0)
        val height = 30
        val scrollDistance = 10

        composeVerticalScroller(scrollState, height = height)

        validateVerticalScroller(height = height)

        rule.waitForIdle()
        assertEquals(scrollDistance, scrollState.maxValue)
        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

        validateVerticalScroller(offset = scrollDistance, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_Reversed() {
        val scrollState = ScrollState(initial = 0)
        val height = 30
        val expectedOffset = defaultCellSize * colors.size - height

        composeVerticalScroller(scrollState, height = height, isReversed = true)

        validateVerticalScroller(offset = expectedOffset, height = height)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun verticalScroller_LargeContent_Reversed_ScrollToEnd() {
        val scrollState = ScrollState(initial = 0)
        val height = 20
        val scrollDistance = 10
        val expectedOffset = defaultCellSize * colors.size - height - scrollDistance

        composeVerticalScroller(scrollState, height = height, isReversed = true)

        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

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

        val scrollState = ScrollState(initial = 0)

        composeHorizontalScroller(scrollState, width = width)

        validateHorizontalScroller(width = width)

        rule.waitForIdle()
        assertEquals(scrollDistance, scrollState.maxValue)
        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

        validateHorizontalScroller(offset = scrollDistance, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_LargeContent_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(initial = 0)

        composeHorizontalScroller(scrollState, width = width, isRtl = true)

        validateHorizontalScroller(width = width, checkInRtl = true)

        rule.waitForIdle()
        assertEquals(scrollDistance, scrollState.maxValue)
        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

        validateHorizontalScroller(offset = scrollDistance, width = width, checkInRtl = true)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_reversed() {
        val scrollState = ScrollState(initial = 0)
        val width = 30
        val expectedOffset = defaultCellSize * colors.size - width

        composeHorizontalScroller(scrollState, width = width, isReversed = true)

        validateHorizontalScroller(offset = expectedOffset, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_reversed() {
        val scrollState = ScrollState(initial = 0)
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

        val scrollState = ScrollState(initial = 0)

        val expectedOffset = defaultCellSize * colors.size - width - scrollDistance

        composeHorizontalScroller(scrollState, width = width, isReversed = true)

        rule.waitForIdle()
        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

        validateHorizontalScroller(offset = expectedOffset, width = width)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun horizontalScroller_rtl_LargeContent_Reversed_ScrollToEnd() {
        val width = 30
        val scrollDistance = 10

        val scrollState = ScrollState(initial = 0)

        val expectedOffset = defaultCellSize * colors.size - width - scrollDistance

        composeHorizontalScroller(scrollState, width = width, isReversed = true, isRtl = true)

        rule.waitForIdle()
        scope.launch {
            scrollState.scrollTo(scrollDistance)
        }

        validateHorizontalScroller(offset = expectedOffset, width = width, checkInRtl = true)
    }

    @Test
    fun verticalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = true)

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_scrollTo_scrollForward() {
        createScrollableContent(isVertical = false)

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_rtl_scrollTo_scrollForward() {
        createScrollableContent(isVertical = false, isRtl = true)

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verticalScroller_reversed_scrollTo_scrollForward() {
        createScrollableContent(
            isVertical = true,
            scrollState = ScrollState(initial = 0),
            isReversed = true
        )

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_reversed_scrollTo_scrollForward() {
        createScrollableContent(
            isVertical = false,
            scrollState = ScrollState(initial = 0),
            isReversed = true
        )

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun verticalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = true)

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()

        rule.onNodeWithText("20")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun horizontalScroller_scrollTo_scrollBack() {
        createScrollableContent(isVertical = false)

        rule.onNodeWithText("50")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()

        rule.onNodeWithText("20")
            .assertIsNotDisplayed()
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @LargeTest
    fun verticalScroller_swipeUp_swipeDown() {
        swipeScrollerAndBack(true, TouchInjectionScope::swipeUp, TouchInjectionScope::swipeDown)
    }

    @Test
    @LargeTest
    fun horizontalScroller_swipeLeft_swipeRight() {
        swipeScrollerAndBack(false, TouchInjectionScope::swipeLeft, TouchInjectionScope::swipeRight)
    }

    @Test
    @LargeTest
    fun horizontalScroller_rtl_swipeLeft_swipeRight() {
        swipeScrollerAndBack(
            false,
            TouchInjectionScope::swipeRight,
            TouchInjectionScope::swipeLeft,
            isRtl = true
        )
    }

    @Test
    fun scroller_coerce_whenScrollTo() {
        val scrollState = ScrollState(initial = 0)

        fun scrollBy(delta: Float) {
            scope.launch {
                scrollState.scrollBy(delta)
            }
            rule.waitForIdle()
        }

        fun scrollTo(position: Int) {
            scope.launch {
                scrollState.scrollTo(position)
            }
            rule.waitForIdle()
        }

        createScrollableContent(isVertical = true, scrollState = scrollState)

        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(scrollState.maxValue).isGreaterThan(0)

        scrollBy(-100f)
        assertThat(scrollState.value).isEqualTo(0)

        scrollBy(-100f)
        assertThat(scrollState.value).isEqualTo(0)

        scrollTo(scrollState.maxValue)
        assertThat(scrollState.value).isEqualTo(scrollState.maxValue)

        scrollTo(scrollState.maxValue + 1000)
        assertThat(scrollState.value).isEqualTo(scrollState.maxValue)

        scrollBy(100f)
        assertThat(scrollState.value).isEqualTo(scrollState.maxValue)
    }

    @Test
    fun verticalScroller_LargeContent_coerceWhenMaxChanges() {
        val scrollState = ScrollState(initial = 0)
        val itemCount = mutableStateOf(100)
        rule.setContent {
            ExtractCoroutineScope()
            Box {
                Column(
                    Modifier
                        .size(100.dp)
                        .testTag(scrollerTag)
                        .verticalScroll(scrollState)
                ) {
                    for (i in 0..itemCount.value) {
                        BasicText(i.toString())
                    }
                }
            }
        }

        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(scrollState.maxValue).isGreaterThan(0)
        val max = scrollState.maxValue

        scope.launch {
            scrollState.scrollTo(max)
        }
        rule.waitForIdle()
        itemCount.value -= 2

        rule.waitForIdle()
        val newMax = scrollState.maxValue
        assertThat(newMax).isLessThan(max)
        assertThat(scrollState.value).isEqualTo(newMax)
    }

    @Test
    fun scroller_coerce_whenScrollSmoothTo() {
        val scrollState = ScrollState(initial = 0)

        fun animateScrollTo(delta: Int) {
            scope.launch {
                scrollState.animateScrollTo(delta)
            }
            rule.waitForIdle()
        }

        fun animateScrollBy(delta: Float) {
            scope.launch {
                scrollState.animateScrollBy(delta)
            }
            rule.waitForIdle()
        }

        createScrollableContent(isVertical = true, scrollState = scrollState)

        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(scrollState.maxValue).isGreaterThan(0)
        val max = scrollState.maxValue

        animateScrollTo(-100)
        assertThat(scrollState.value).isEqualTo(0)

        animateScrollBy(-100f)
        assertThat(scrollState.value).isEqualTo(0)

        animateScrollTo(scrollState.maxValue)
        assertThat(scrollState.value).isEqualTo(max)

        animateScrollTo(scrollState.maxValue + 1000)
        assertThat(scrollState.value).isEqualTo(max)

        animateScrollBy(100f)
        assertThat(scrollState.value).isEqualTo(max)
    }

    @Test
    fun scroller_whenFling_stopsByTouchDown() {
        rule.mainClock.autoAdvance = false
        val scrollState = ScrollState(initial = 0)

        createScrollableContent(isVertical = true, scrollState = scrollState)

        assertThat(scrollState.value).isEqualTo(0)
        assertThat(scrollState.isScrollInProgress).isEqualTo(false)

        rule.onNodeWithTag(scrollerTag)
            .performTouchInput { swipeUp() }

        assertThat(scrollState.isScrollInProgress).isEqualTo(true)
        val scrollAtFlingStart = scrollState.value

        // Let the fling run for a bit
        rule.mainClock.advanceTimeBy(100)

        // Interrupt the fling
        val scrollWhenInterruptFling = scrollState.value
        assertThat(scrollWhenInterruptFling).isGreaterThan(scrollAtFlingStart)
        rule.onNodeWithTag(scrollerTag)
            .performTouchInput { down(center) }

        // The fling has been stopped:
        rule.mainClock.advanceTimeBy(100)
        assertThat(scrollState.value).isEqualTo(scrollWhenInterruptFling)
    }

    @Test
    fun scroller_restoresScrollerPosition() {
        val restorationTester = StateRestorationTester(rule)
        var scrollState: ScrollState? = null

        restorationTester.setContent {
            ExtractCoroutineScope()
            val actualState = rememberScrollState()
            SideEffect { scrollState = actualState }
            Column(Modifier.verticalScroll(actualState)) {
                repeat(50) {
                    Box(Modifier.height(100.dp))
                }
            }
        }

        rule.waitForIdle()
        scope.launch {
            scrollState!!.scrollTo(70)
        }
        rule.waitForIdle()
        scrollState = null

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(scrollState!!.value).isEqualTo(70)
        }
    }

    private fun swipeScrollerAndBack(
        isVertical: Boolean,
        firstSwipe: TouchInjectionScope.() -> Unit,
        secondSwipe: TouchInjectionScope.() -> Unit,
        isRtl: Boolean = false
    ) {
        rule.mainClock.autoAdvance = false
        val scrollState = ScrollState(initial = 0)

        createScrollableContent(isVertical, scrollState = scrollState, isRtl = isRtl)

        assertThat(scrollState.value).isEqualTo(0)

        rule.onNodeWithTag(scrollerTag)
            .performTouchInput { firstSwipe() }

        rule.mainClock.advanceTimeBy(5000)

        rule.onNodeWithTag(scrollerTag)
            .awaitScrollAnimation(scrollState)

        val scrolledValue = scrollState.value
        assertThat(scrolledValue).isGreaterThan(0)

        rule.onNodeWithTag(scrollerTag)
            .performTouchInput { secondSwipe() }

        rule.mainClock.advanceTimeBy(5000)

        rule.onNodeWithTag(scrollerTag)
            .awaitScrollAnimation(scrollState)

        assertThat(scrollState.value).isLessThan(scrolledValue)
    }

    @Test
    fun scroller_semanticsScroll_isAnimated() {
        rule.mainClock.autoAdvance = false
        val scrollState = ScrollState(initial = 0)

        createScrollableContent(isVertical = true, scrollState = scrollState)

        rule.waitForIdle()
        assertThat(scrollState.value).isEqualTo(0)
        assertThat(scrollState.maxValue).isGreaterThan(100) // If this fails, just add more items

        rule.onNodeWithTag(scrollerTag).performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, 100f)
        }

        // We haven't advanced time yet, make sure it's still zero
        assertThat(scrollState.value).isEqualTo(0)

        // Advance and make sure we're partway through
        // Note that we need two frames for the animation to actually happen
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertThat(scrollState.value).isGreaterThan(0)
        assertThat(scrollState.value).isLessThan(100)

        // Finish the scroll, make sure we're at the target
        rule.mainClock.advanceTimeBy(5000)
        assertThat(scrollState.value).isEqualTo(100)
    }

    @Test
    fun scroller_touchInputEnabled_shouldHaveSemanticsInfo() {
        val scrollState = ScrollState(initial = 0)
        val scrollNode = rule.onNodeWithTag(scrollerTag)
        createScrollableContent(isVertical = true, scrollState = scrollState)
        val yScrollState = scrollNode
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)

        scrollNode.performTouchInput { swipeUp() }

        assertThat(yScrollState?.value?.invoke()).isEqualTo(scrollState.value)
    }

    @Test
    fun scroller_touchInputDisabled_shouldHaveSemanticsInfo() {
        val scrollState = ScrollState(initial = 0)
        val scrollNode = rule.onNodeWithTag(scrollerTag)
        createScrollableContent(
            isVertical = true,
            scrollState = scrollState,
            touchInputEnabled = false
        )
        val yScrollState = scrollNode
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)

        scrollNode.performTouchInput { swipeUp() }

        assertThat(yScrollState?.value?.invoke()).isEqualTo(scrollState.value)
    }

    private fun composeVerticalScroller(
        scrollState: ScrollState? = null,
        isReversed: Boolean = false,
        width: Int = defaultCrossAxisSize,
        height: Int = defaultMainAxisSize,
        rowHeight: Int = defaultCellSize
    ) {
        val resolvedState = scrollState ?: ScrollState(initial = 0)
        // We assume that the height of the device is more than 45 px
        with(rule.density) {
            rule.setContent {
                ExtractCoroutineScope()
                Box {
                    Column(
                        modifier = Modifier
                            .size(width.toDp(), height.toDp())
                            .testTag(scrollerTag)
                            .verticalScroll(
                                resolvedState,
                                reverseScrolling = isReversed
                            )
                    ) {
                        colors.forEach { color ->
                            Box(
                                Modifier
                                    .size(width.toDp(), rowHeight.toDp())
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun composeHorizontalScroller(
        scrollState: ScrollState? = null,
        isReversed: Boolean = false,
        width: Int = defaultMainAxisSize,
        height: Int = defaultCrossAxisSize,
        isRtl: Boolean = false
    ) {
        val resolvedState = scrollState ?: ScrollState(initial = 0)
        // We assume that the height of the device is more than 45 px
        with(rule.density) {
            rule.setContent {
                ExtractCoroutineScope()
                val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Box {
                        Row(
                            modifier = Modifier
                                .size(width.toDp(), height.toDp())
                                .testTag(scrollerTag)
                                .horizontalScroll(
                                    resolvedState,
                                    reverseScrolling = isReversed
                                )
                        ) {
                            colors.forEach { color ->
                                Box(
                                    Modifier
                                        .size(defaultCellSize.toDp(), height.toDp())
                                        .background(color)
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
        rule.onNodeWithTag(scrollerTag)
            .captureToImage()
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
        rule.onNodeWithTag(scrollerTag)
            .captureToImage()
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
        scrollState: ScrollState? = null,
        isRtl: Boolean = false,
        touchInputEnabled: Boolean = true
    ) {
        val resolvedState = scrollState ?: ScrollState(initial = 0)
        rule.setContent {
            ExtractCoroutineScope()
            val content = @Composable {
                repeat(itemCount) {
                    BasicText(text = "$it")
                }
            }
            Box {
                Box(
                    Modifier.size(width, height).background(Color.White)
                ) {
                    if (isVertical) {
                        Column(
                            Modifier
                                .testTag(scrollerTag)
                                .verticalScroll(
                                    resolvedState,
                                    enabled = touchInputEnabled,
                                    reverseScrolling = isReversed
                                )
                        ) {
                            content()
                        }
                    } else {
                        val direction = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                        CompositionLocalProvider(LocalLayoutDirection provides direction) {
                            Row(
                                Modifier.testTag(scrollerTag)
                                    .horizontalScroll(
                                        resolvedState,
                                        enabled = touchInputEnabled,
                                        reverseScrolling = isReversed
                                    )
                            ) {
                                content()
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
                if (scroller.isScrollInProgress) {
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

    @Test
    fun testInspectorValue() {
        val state = ScrollState(initial = 0)
        rule.setContent {
            val modifier = Modifier.verticalScroll(state) as InspectableValue
            assertThat(modifier.nameFallback).isEqualTo("scroll")
            assertThat(modifier.valueOverride).isNull()
            assertThat(modifier.inspectableElements.map { it.name }.asIterable()).containsExactly(
                "state",
                "reverseScrolling",
                "flingBehavior",
                "isScrollable",
                "isVertical"
            )
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun horizontalScroller_doesNotClipVerticalOverdraw() {
        rule.setContent {
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                Row(
                    Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState(20))
                ) {
                    repeat(4) {
                        Box(Modifier.size(20.dp).drawOutsideOfBounds())
                    }
                }
            }
        }

        rule.onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 20.dp,
                verticalPadding = 0.dp
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun verticalScroller_doesNotClipHorizontalOverdraw() {
        rule.setContent {
            Box(Modifier.size(60.dp).testTag("container").background(Color.Gray)) {
                Column(
                    Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState(20))
                ) {
                    repeat(4) {
                        Box(Modifier.size(20.dp).drawOutsideOfBounds())
                    }
                }
            }
        }

        rule.onNodeWithTag("container")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Red,
                backgroundColor = Color.Gray,
                horizontalPadding = 0.dp,
                verticalPadding = 20.dp
            )
    }

    @Test
    fun intrinsicMeasurements() = with(rule.density) {
        rule.setContent {
            Layout(
                content = {
                    Layout(
                        {},
                        Modifier.verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState()),
                        object : MeasurePolicy {
                            override fun MeasureScope.measure(
                                measurables: List<Measurable>,
                                constraints: Constraints,
                            ) = layout(0, 0) {}

                            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                                measurables: List<IntrinsicMeasurable>,
                                height: Int,
                            ) = 10.dp.roundToPx()

                            override fun IntrinsicMeasureScope.minIntrinsicHeight(
                                measurables: List<IntrinsicMeasurable>,
                                width: Int,
                            ) = 20.dp.roundToPx()

                            override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                                measurables: List<IntrinsicMeasurable>,
                                height: Int,
                            ) = 30.dp.roundToPx()

                            override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                                measurables: List<IntrinsicMeasurable>,
                                width: Int,
                            ) = 40.dp.roundToPx()
                        }
                    )
                }
            ) { measurables, _ ->
                val measurable = measurables.first()
                assertEquals(10.dp.roundToPx(), measurable.minIntrinsicWidth(Constraints.Infinity))
                assertEquals(20.dp.roundToPx(), measurable.minIntrinsicHeight(Constraints.Infinity))
                assertEquals(30.dp.roundToPx(), measurable.maxIntrinsicWidth(Constraints.Infinity))
                assertEquals(40.dp.roundToPx(), measurable.maxIntrinsicHeight(Constraints.Infinity))
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    private fun Modifier.drawOutsideOfBounds() = drawBehind {
        val inflate = 20.dp.roundToPx().toFloat()
        drawRect(
            Color.Red,
            Offset(-inflate, -inflate),
            Size(size.width + inflate * 2, size.height + inflate * 2)
        )
    }
}
