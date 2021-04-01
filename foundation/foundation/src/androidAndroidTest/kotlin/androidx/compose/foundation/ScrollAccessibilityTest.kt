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
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.filters.MediumTest
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
        get() = ViewCompat
            .getAccessibilityDelegate(composeView!!)!!
            .getAccessibilityNodeProvider(composeView)
            .provider as AccessibilityNodeProvider

    @Test
    fun scrollForward() {
        testRelativeDirection(55, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
    }

    @Test
    fun scrollBackward() {
        testRelativeDirection(45, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun scrollRight() {
        testAbsoluteDirection(55, android.R.id.accessibilityActionScrollRight, config.horizontal)
    }

    @Test
    fun scrollLeft() {
        testAbsoluteDirection(45, android.R.id.accessibilityActionScrollLeft, config.horizontal)
    }

    @Test
    fun scrollDown() {
        testAbsoluteDirection(55, android.R.id.accessibilityActionScrollDown, config.vertical)
    }

    @Test
    fun scrollUp() {
        testAbsoluteDirection(45, android.R.id.accessibilityActionScrollUp, config.vertical)
    }

    /**
     * Setup the test, run the given [accessibilityAction], and check if the [canonicalTarget]
     * has been reached. The canonical target is the item that we expect to see when moving
     * forward in a non-reversed scrollable (e.g. down in vertical orientation or right in
     * horizontal orientation in LTR). The actual target is either the canonical target or the
     * target that is as far from the middle of the scrollable as the canonical target, but on
     * the other side of the middle. For testing relative directions, this mirroring is done if
     * the scroll is [reversed][TestConfig.reversed].
     */
    private fun testRelativeDirection(canonicalTarget: Int, accessibilityAction: Int) {
        val target = if (!config.reversed) canonicalTarget else 100 - canonicalTarget - 1
        testScrollAction(target, accessibilityAction)
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

        waitForSubtreeEventToSend()
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
     * Creates a Row/Column with a viewport of 100.dp, containing 100 items each 17.dp in size.
     * The items have a text with their index (ASC), and the viewport starts in the middle of the
     * scrollable. All properties from [config] are applied. The viewport has padding around it
     * to make sure scroll distance doesn't include padding.
     */
    private fun createScrollableContent_StartInMiddle() {
        rule.setContent {
            composeView = LocalView.current
            val content = @Composable {
                repeat(100) {
                    Box(Modifier.requiredSize(17.dp)) {
                        BasicText("$it", Modifier.align(Alignment.Center))
                    }
                }
            }

            // Start at the middle:
            // Content size: 100 boxes * 17dp per box = 1700dp
            // Viewport size: 300dp box - 100dp padding on both sides = 100dp
            // Content outside viewport: 1700dp - 100dp = 1600dp
            // -> centered when 800dp on either side
            val state = rememberScrollState(with(LocalDensity.current) { 800.dp.roundToPx() })

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

    private fun waitForSubtreeEventToSend() {
        // When the subtree events are sent, we will also update our previousSemanticsNodes,
        // which will affect our next accessibility events from semantics tree comparison.
        rule.mainClock.advanceTimeBy(1000)
        rule.waitForIdle()
    }
}
