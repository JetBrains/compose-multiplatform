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

import android.R.id.accessibilityActionScrollDown
import android.R.id.accessibilityActionScrollLeft
import android.R.id.accessibilityActionScrollRight
import android.R.id.accessibilityActionScrollUp
import android.view.View
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class ScrollAccessibilityTest(private val config: TestConfig) {
    data class TestConfig(
        val horizontal: Boolean,
        val rtl: Boolean,
        val reversed: Boolean
    ) {
        val vertical = !horizontal

        override fun toString(): String {
            return (if (horizontal) "horizontal" else "vertical") +
                (if (rtl) ",rtl" else ",ltr") +
                (if (reversed) ",reversed" else "")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() =
            listOf(true, false).flatMap { horizontal ->
                listOf(false, true).flatMap { rtl ->
                    listOf(false, true).map { reversed ->
                        TestConfig(horizontal, rtl, reversed)
                    }
                }
            }
    }

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

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
        testScrollAction(55, ACTION_SCROLL_FORWARD)
    }

    @Test
    fun scrollBackward() {
        testScrollAction(45, ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun scrollRight() {
        testAbsoluteDirection(55, accessibilityActionScrollRight, config.horizontal)
    }

    @Test
    fun scrollLeft() {
        testAbsoluteDirection(45, accessibilityActionScrollLeft, config.horizontal)
    }

    @Test
    fun scrollDown() {
        testAbsoluteDirection(55, accessibilityActionScrollDown, config.vertical)
    }

    @Test
    fun scrollUp() {
        testAbsoluteDirection(45, accessibilityActionScrollUp, config.vertical)
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
     * has been reached (but only if we [expect][expectActionSuccess] the action to succeed).
     * The canonical target is the item that we expect to see when moving forward in a
     * non-reversed scrollable (e.g. down in vertical orientation or right in horizontal
     * orientation in LTR). The actual target is either the canonical target or the target that
     * is as far from the middle of the scrollable as the canonical target, but on the other side
     * of the middle. For testing absolute directions, this mirroring is done for
     * [horizontal][TestConfig.horizontal] [RTL][TestConfig.rtl] tests.
     */
    private fun testAbsoluteDirection(
        canonicalTarget: Int,
        accessibilityAction: Int,
        expectActionSuccess: Boolean
    ) {
        val inverse = config.horizontal && config.rtl
        val target = if (!inverse) canonicalTarget else 100 - canonicalTarget - 1
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
        rule.onNodeWithText("$target").assertIsNotDisplayed()

        val returnValue = rule.onNodeWithTag(scrollerTag).withSemanticsNode {
            accessibilityNodeProvider.performAction(id, accessibilityAction, null)
        }

        assertThat(returnValue).isEqualTo(expectActionSuccess)
        if (expectActionSuccess) {
            rule.onNodeWithText("$target").assertIsDisplayed()
        } else {
            rule.onNodeWithText("$target").assertIsNotDisplayed()
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
     * Creates a Row/Column that starts at offset 0, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartAtStart() {
        createScrollableContent {
            // Start at the start:
            // -> pretty basic
            rememberScrollState(0)
        }
    }

    /**
     * Creates a Row/Column that starts in the middle, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartInMiddle() {
        createScrollableContent {
            with(LocalDensity.current) {
                // Start at the middle:
                // Content size: 100 boxes * 17dp per box = 1700dp
                val contentSize = 100 * 17.dp.roundToPx()
                // Viewport size: 300dp box - 100dp padding on both sides = 100dp
                val viewportSize = 300.dp.roundToPx() - 2 * 100.dp.roundToPx()
                // Content outside viewport: 1700dp - 100dp = 1600dp
                // -> centered when 800dp on either side
                rememberScrollState((contentSize - viewportSize) / 2)
            }
        }
    }

    /**
     * Creates a Row/Column that starts at the max offset, according to [createScrollableContent]
     */
    private fun createScrollableContent_StartAtEnd() {
        createScrollableContent {
            with(LocalDensity.current) {
                // Start at the end:
                // Content size: 100 boxes * 17dp per box = 1700dp
                val contentSize = 100 * 17.dp.roundToPx()
                // Viewport size: 300dp box - 100dp padding on both sides = 100dp
                val viewportSize = 300.dp.roundToPx() - 2 * 100.dp.roundToPx()
                // Content outside viewport: 1700dp - 100dp = 1600dp
                // -> at the end when offset at 1600dp
                rememberScrollState(contentSize - viewportSize)
            }
        }
    }

    /**
     * Creates a Row/Column with a viewport of 100.dp, containing 100 items each 17.dp in size.
     * The items have a text with their index (ASC), and where the viewport starts is determined
     * by the given [lambda][rememberScrollState]. All properties from [config] are applied. The
     * viewport has padding around it to make sure scroll distance doesn't include padding.
     */
    private fun createScrollableContent(rememberScrollState: @Composable () -> ScrollState) {
        rule.setContent {
            composeView = LocalView.current
            val content = @Composable {
                repeat(100) {
                    Box(Modifier.requiredSize(17.dp)) {
                        BasicText("$it", Modifier.align(Alignment.Center))
                    }
                }
            }

            val state = rememberScrollState()

            Box(Modifier.requiredSize(300.dp).background(Color.White)) {
                if (config.horizontal) {
                    val direction = if (config.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        Row(
                            Modifier.testTag(scrollerTag).align(Alignment.Center).padding(100.dp)
                                .horizontalScroll(state, reverseScrolling = config.reversed)
                        ) {
                            content()
                        }
                    }
                } else {
                    Column(
                        Modifier.testTag(scrollerTag).align(Alignment.Center).padding(100.dp)
                            .verticalScroll(state, reverseScrolling = config.reversed)
                    ) {
                        content()
                    }
                }
            }
        }
    }

    private fun <T> SemanticsNodeInteraction.withSemanticsNode(block: SemanticsNode.() -> T): T {
        return block.invoke(fetchSemanticsNode())
    }
}
