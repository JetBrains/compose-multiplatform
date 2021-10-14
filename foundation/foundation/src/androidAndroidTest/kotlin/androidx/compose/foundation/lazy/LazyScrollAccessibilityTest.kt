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

package androidx.compose.foundation.lazy

import android.view.View
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicText
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
class LazyScrollAccessibilityTest(private val config: TestConfig) {
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
                .getAccessibilityNodeProvider(composeView)
                .provider as AccessibilityNodeProvider
        }

    @Test
    fun scrollForward() {
        testRelativeDirection(58, AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
    }

    @Test
    fun scrollBackward() {
        testRelativeDirection(41, AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
    }

    @Test
    fun scrollRight() {
        testAbsoluteDirection(58, android.R.id.accessibilityActionScrollRight, config.horizontal)
    }

    @Test
    fun scrollLeft() {
        testAbsoluteDirection(41, android.R.id.accessibilityActionScrollLeft, config.horizontal)
    }

    @Test
    fun scrollDown() {
        testAbsoluteDirection(58, android.R.id.accessibilityActionScrollDown, config.vertical)
    }

    @Test
    fun scrollUp() {
        testAbsoluteDirection(41, android.R.id.accessibilityActionScrollUp, config.vertical)
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
     * Creates a Row/Column with a viewport of 100.dp, containing 100 items each 17.dp in size.
     * The items have a text with their index (ASC), and the viewport starts in the middle of the
     * scrollable. All properties from [config] are applied. The viewport has padding around it
     * to make sure scroll distance doesn't include padding.
     */
    private fun createScrollableContent_StartInMiddle() {
        rule.setContent {
            composeView = LocalView.current
            val lazyContent: LazyListScope.() -> Unit = {
                items(100) {
                    Box(Modifier.requiredSize(21.dp).background(Color.Yellow)) {
                        BasicText("$it", Modifier.align(Alignment.Center))
                    }
                }
            }

            // Start at the middle:
            // Content size: 100 items * 21dp per item = 2100dp
            // Viewport size: 200dp rect - 50dp padding on both sides = 100dp
            // Content outside viewport: 2100dp - 100dp = 2000dp
            // -> centered when 1000dp on either side, which is 47 items + 13dp
            val state = rememberLazyListState(
                47,
                with(LocalDensity.current) { 13.dp.roundToPx() }
            )

            Box(Modifier.requiredSize(200.dp).background(Color.White)) {
                val direction = if (config.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    if (config.horizontal) {
                        LazyRow(
                            Modifier.testTag(scrollerTag).matchParentSize(),
                            state = state,
                            contentPadding = PaddingValues(50.dp),
                            reverseLayout = config.reversed,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            lazyContent()
                        }
                    } else {
                        LazyColumn(
                            Modifier.testTag(scrollerTag).matchParentSize(),
                            state = state,
                            contentPadding = PaddingValues(50.dp),
                            reverseLayout = config.reversed,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            lazyContent()
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
