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

package androidx.compose.foundation.lazy.grid

import android.R.id.accessibilityActionScrollDown
import android.R.id.accessibilityActionScrollLeft
import android.R.id.accessibilityActionScrollRight
import android.R.id.accessibilityActionScrollUp
import android.view.View
import android.view.accessibility.AccessibilityNodeProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD
import androidx.test.filters.MediumTest
import com.google.common.truth.IterableSubject
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class LazyScrollAccessibilityTest(
    private val config: TestConfig
) : BaseLazyGridTestWithOrientation(config.orientation) {

    data class TestConfig(
        val orientation: Orientation,
        val rtl: Boolean,
        val reversed: Boolean
    ) {
        val horizontal = orientation == Orientation.Horizontal
        val vertical = !horizontal

        override fun toString(): String {
            return (if (orientation == Orientation.Horizontal) "horizontal" else "vertical") +
                (if (rtl) ",rtl" else ",ltr") +
                (if (reversed) ",reversed" else "")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            listOf(Orientation.Horizontal, Orientation.Vertical).flatMap { horizontal ->
                listOf(false, true).flatMap { rtl ->
                    listOf(false, true).map { reversed ->
                        TestConfig(horizontal, rtl, reversed)
                    }
                }
            }
    }

    private val scrollerTag = "ScrollerTest"
    private var composeView: View? = null
    private val accessibilityNodeProvider: AccessibilityNodeProvider
        get() = checkNotNull(composeView) {
            "composeView not initialized. Did `composeView = LocalView.current` not work?"
        }.let { composeView ->
            ViewCompat
                .getAccessibilityDelegate(composeView)!!
                .getAccessibilityNodeProvider(composeView)!!
                .provider as AccessibilityNodeProvider
        }

    @Test
    fun scrollForward() {
        testRelativeDirection(58, ACTION_SCROLL_FORWARD)
    }

    @Test
    fun scrollBackward() {
        testRelativeDirection(41, ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun scrollRight() {
        testAbsoluteDirection(58, accessibilityActionScrollRight, config.horizontal)
    }

    @Test
    fun scrollLeft() {
        testAbsoluteDirection(41, accessibilityActionScrollLeft, config.horizontal)
    }

    @Test
    fun scrollDown() {
        testAbsoluteDirection(58, accessibilityActionScrollDown, config.vertical)
    }

    @Test
    fun scrollUp() {
        testAbsoluteDirection(41, accessibilityActionScrollUp, config.vertical)
    }

    @Test
    fun verifyScrollActionsAtStart() {
        createScrollableContent_StartAtStart()
        verifyNodeInfoScrollActions(
            expectForward = !config.reversed,
            expectBackward = config.reversed
        )
    }

    @Test
    fun verifyScrollActionsInMiddle() {
        createScrollableContent_StartInMiddle()
        verifyNodeInfoScrollActions(
            expectForward = true,
            expectBackward = true
        )
    }

    @Test
    fun verifyScrollActionsAtEnd() {
        createScrollableContent_StartAtEnd()
        verifyNodeInfoScrollActions(
            expectForward = config.reversed,
            expectBackward = !config.reversed
        )
    }

    /**
     * Setup the test, run the given [accessibilityAction], and check if the [canonicalTarget]
     * has been reached. The canonical target is the item that we expect to see when moving
     * forward in a non-reversed scrollable (e.g. down in LazyColumn or right in LazyRow in LTR).
     * The actual target is either the canonical target or the target that is as far from the
     * middle of the lazy list as the canonical target, but on the other side of the middle,
     * depending on the [configuration][config].
     */
    private fun testRelativeDirection(canonicalTarget: Int, accessibilityAction: Int) {
        val target = if (!config.reversed) canonicalTarget else 100 - canonicalTarget - 1
        testScrollAction(target, accessibilityAction)
    }

    /**
     * Setup the test, run the given [accessibilityAction], and check if the [canonicalTarget]
     * has been reached (but only if we [expect][expectActionSuccess] the action to succeed).
     * The canonical target is the item that we expect to see when moving forward in a
     * non-reversed scrollable (e.g. down in LazyColumn or right in LazyRow in LTR). The actual
     * target is either the canonical target or the target that is as far from the middle of the
     * scrollable as the canonical target, but on the other side of the middle, depending on the
     * [configuration][config].
     */
    private fun testAbsoluteDirection(
        canonicalTarget: Int,
        accessibilityAction: Int,
        expectActionSuccess: Boolean
    ) {
        var target = canonicalTarget
        if (config.horizontal && config.rtl) {
            target = 100 - target - 1
        }
        if (config.reversed) {
            target = 100 - target - 1
        }
        testScrollAction(target, accessibilityAction, expectActionSuccess)
    }

    /**
     * Setup the test, run the given [accessibilityAction], and check if the [target] has been
     * reached (but only if we [expect][expectActionSuccess] the action to succeed).
     */
    private fun testScrollAction(
        target: Int,
        accessibilityAction: Int,
        expectActionSuccess: Boolean = true
    ) {
        createScrollableContent_StartInMiddle()
        rule.onNodeWithText("$target").assertDoesNotExist()

        val returnValue = rule.onNodeWithTag(scrollerTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, accessibilityAction, null)
        }

        assertThat(returnValue).isEqualTo(expectActionSuccess)
        if (expectActionSuccess) {
            rule.onNodeWithText("$target").assertIsDisplayed()
        } else {
            rule.onNodeWithText("$target").assertDoesNotExist()
        }
    }

    /**
     * Checks if all of the scroll actions are present or not according to what we expect based on
     * [expectForward] and [expectBackward]. The scroll actions that are checked are forward,
     * backward, left, right, up and down. The expectation parameters must already account for
     * [reversing][TestConfig.reversed].
     */
    private fun verifyNodeInfoScrollActions(expectForward: Boolean, expectBackward: Boolean) {
        val nodeInfo = rule.onNodeWithTag(scrollerTag).withSemanticsNode {
            rule.runOnUiThread {
                accessibilityNodeProvider.createAccessibilityNodeInfo(id)!!
            }
        }

        val actions = nodeInfo.actionList.map { it.id }

        assertThat(actions).contains(expectForward, ACTION_SCROLL_FORWARD)
        assertThat(actions).contains(expectBackward, ACTION_SCROLL_BACKWARD)

        if (config.horizontal) {
            val expectLeft = if (config.rtl) expectForward else expectBackward
            val expectRight = if (config.rtl) expectBackward else expectForward
            assertThat(actions).contains(expectLeft, accessibilityActionScrollLeft)
            assertThat(actions).contains(expectRight, accessibilityActionScrollRight)
            assertThat(actions).contains(false, accessibilityActionScrollDown)
            assertThat(actions).contains(false, accessibilityActionScrollUp)
        } else {
            assertThat(actions).contains(false, accessibilityActionScrollLeft)
            assertThat(actions).contains(false, accessibilityActionScrollRight)
            assertThat(actions).contains(expectForward, accessibilityActionScrollDown)
            assertThat(actions).contains(expectBackward, accessibilityActionScrollUp)
        }
    }

    private fun IterableSubject.contains(expectPresent: Boolean, element: Any) {
        if (expectPresent) {
            contains(element)
        } else {
            doesNotContain(element)
        }
    }

    /**
     * Creates a Row/Column that starts at the first item, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartAtStart() {
        createScrollableContent {
            // Start at the start:
            // -> pretty basic
            rememberLazyGridState(0, 0)
        }
    }

    /**
     * Creates a Row/Column that starts in the middle, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartInMiddle() {
        createScrollableContent {
            // Start at the middle:
            // Content size: 100 items * 21dp per item = 2100dp
            // Viewport size: 200dp rect - 50dp padding on both sides = 100dp
            // Content outside viewport: 2100dp - 100dp = 2000dp
            // -> centered when 1000dp on either side, which is 47 items + 13dp
            rememberLazyGridState(
                47,
                with(LocalDensity.current) { 13.dp.roundToPx() }
            )
        }
    }

    /**
     * Creates a Row/Column that starts at the last item, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartAtEnd() {
        createScrollableContent {
            // Start at the end:
            // Content size: 100 items * 21dp per item = 2100dp
            // Viewport size: 200dp rect - 50dp padding on both sides = 100dp
            // Content outside viewport: 2100dp - 100dp = 2000dp
            // -> at the end when offset at 2000dp, which is 95 items + 5dp
            rememberLazyGridState(
                95,
                with(LocalDensity.current) { 5.dp.roundToPx() }
            )
        }
    }

    /**
     * Creates a grid with a viewport of 100.dp, containing 100 items each 17.dp in size.
     * The items have a text with their index (ASC), and where the viewport starts is determined
     * by the given [lambda][rememberLazyGridState]. All properties from [config] are applied.
     * The viewport has padding around it to make sure scroll distance doesn't include padding.
     */
    private fun createScrollableContent(rememberLazyGridState: @Composable () -> LazyGridState) {
        rule.setContent {
            composeView = LocalView.current

            val state = rememberLazyGridState()

            Box(Modifier.requiredSize(200.dp).background(Color.White)) {
                val direction = if (config.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    LazyGrid(
                        cells = 1,
                        modifier = Modifier.testTag(scrollerTag).matchParentSize(),
                        state = state,
                        contentPadding = PaddingValues(50.dp),
                        reverseLayout = config.reversed
                    ) {
                        items(100) {
                            Box(Modifier.requiredSize(21.dp).background(Color.Yellow)) {
                                BasicText("$it", Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> SemanticsNodeInteraction.withSemanticsNode(block: SemanticsNode.() -> T): T {
        return block.invoke(fetchSemanticsNode())
    }
}
