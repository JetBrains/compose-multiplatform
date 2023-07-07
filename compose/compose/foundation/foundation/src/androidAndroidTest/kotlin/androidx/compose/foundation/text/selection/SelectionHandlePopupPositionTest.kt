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

package androidx.compose.foundation.text.selection

import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.isPopupLayout
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectionHandlePopupPositionTest {
    @get:Rule
    val rule = createComposeRule()

    private val offset = Offset(120f, 120f)
    private val parentSizeWidth = 100.dp
    private val parentSizeHeight = 100.dp

    private var composeViewAbsolutePos = IntOffset(0, 0)

    @Test
    fun leftHandle_Ltr_correctPosition() {
        /* Expected TopEnd Position
           x = offset.x - HANDLE_WIDTH
           y = offset.y
        */
        with(rule.density) {
            val expectedAnchorPositionX = offset.x.toDp()
            val expectedPopupPositionX = expectedAnchorPositionX - HandleWidth
            val expectedPositionY = offset.y.toDp()

            createSelectionHandle(isStartHandle = true)

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPopupPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedAnchorPositionX, expectedPositionY)
        }
    }

    @Test
    fun leftHandle_Rtl_correctPosition() {
        /* Expected TopEnd Position
           x = offset.x - HANDLE_WIDTH
           y = offset.y
        */
        with(rule.density) {
            val expectedAnchorPositionX = offset.x.toDp()
            val expectedPopupPositionX = expectedAnchorPositionX - HandleWidth
            val expectedPositionY = offset.y.toDp()

            createSelectionHandle(isStartHandle = true, isRtl = true)

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPopupPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedAnchorPositionX, expectedPositionY)
        }
    }

    @Test
    fun rightHandle_Ltr_correctPosition() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        with(rule.density) {
            val expectedPositionX = offset.x.toDp()
            val expectedPositionY = offset.y.toDp()

            createSelectionHandle(isStartHandle = false)

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedPositionX, expectedPositionY)
        }
    }

    @Test
    fun rightHandle_Rtl_correctPosition() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        with(rule.density) {
            val expectedPositionX = offset.x.toDp()
            val expectedPositionY = offset.y.toDp()
            createSelectionHandle(isStartHandle = false, isRtl = true)

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedPositionX, expectedPositionY)
        }
    }

    @Test
    fun leftHandle_semanticsPosition_isRelativeToContainer() {
        with(rule.density) {
            val containerOffsetX = 20.dp
            val containerOffsetY = 30.dp
            // Since the semantics position is relative to the container, it shouldn't include the
            // container offset.
            val expectedSemanticsPositionX = offset.x.toDp()
            val expectedSemanticsPositionY = offset.y.toDp()
            // However, the popup position, which is in absolute screen coordinates, should include
            // the container offset.
            val expectedPopupPositionX = containerOffsetX + expectedSemanticsPositionX - HandleWidth
            val expectedPopupPositionY = containerOffsetY + expectedSemanticsPositionY

            createSelectionHandle(
                isStartHandle = true,
                containerModifier = Modifier.offset(containerOffsetX, containerOffsetY)
            )

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPopupPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPopupPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionStart))
                .assertHandlePositionMatches(expectedSemanticsPositionX, expectedSemanticsPositionY)
        }
    }

    @Test
    fun rightHandle_semanticsPosition_isRelativeToContainer() {
        with(rule.density) {
            val containerOffsetX = 20.dp
            val containerOffsetY = 30.dp
            // Since the semantics position is relative to the container, it shouldn't include the
            // container offset.
            val expectedSemanticsPositionX = offset.x.toDp()
            val expectedSemanticsPositionY = offset.y.toDp()
            // However, the popup position, which is in absolute screen coordinates, should include
            // the container offset.
            val expectedPopupPositionX = containerOffsetX + expectedSemanticsPositionX
            val expectedPopupPositionY = containerOffsetY + expectedSemanticsPositionY

            createSelectionHandle(
                isStartHandle = false,
                containerModifier = Modifier.offset(containerOffsetX, containerOffsetY)
            )

            rule.singleSelectionHandleMatches(
                matchesPosition(
                    composeViewAbsolutePos.x.toDp() + expectedPopupPositionX,
                    composeViewAbsolutePos.y.toDp() + expectedPopupPositionY
                )
            )
            rule.onNode(isSelectionHandle(Handle.SelectionEnd))
                .assertHandlePositionMatches(expectedSemanticsPositionX, expectedSemanticsPositionY)
        }
    }

    private fun createSelectionHandle(
        isStartHandle: Boolean,
        containerModifier: Modifier = Modifier,
        isRtl: Boolean = false
    ) {
        val measureLatch = CountDownLatch(1)

        rule.setContent {
            Box(containerModifier) {
                // Get the compose view position on screen
                val composeView = LocalView.current
                val positionArray = IntArray(2)
                composeView.getLocationOnScreen(positionArray)
                composeViewAbsolutePos = IntOffset(
                    positionArray[0],
                    positionArray[1]
                )

                // Align the parent of the popup on the top left corner, this results in the global
                // position of the parent to be (0, 0)
                val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    SimpleLayout {
                        Spacer(Modifier.size(parentSizeWidth, parentSizeHeight))
                        SelectionHandle(
                            position = offset,
                            isStartHandle = isStartHandle,
                            direction = ResolvedTextDirection.Ltr,
                            handlesCrossed = false,
                            modifier = Modifier.onGloballyPositioned {
                                measureLatch.countDown()
                            },
                            content = null
                        )
                    }
                }
            }
        }
        measureLatch.await(1, TimeUnit.SECONDS)
    }

    private fun matchesPosition(expectedPositionX: Dp, expectedPositionY: Dp):
        BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            // (-1, -1) no position found
            var positionFound = IntOffset(-1, -1)

            override fun matchesSafely(item: View?): Boolean {
                with(rule.density) {
                    val position = IntArray(2)
                    item?.getLocationOnScreen(position)
                    positionFound = IntOffset(position[0], position[1])

                    val expectedPositionXInt = expectedPositionX.value.toInt()
                    val expectedPositionYInt = expectedPositionY.value.toInt()
                    val positionFoundXInt = positionFound.x.toDp().value.toInt()
                    val positionFoundYInt = positionFound.y.toDp().value.toInt()
                    return expectedPositionXInt == positionFoundXInt &&
                        expectedPositionYInt == positionFoundYInt
                }
            }

            override fun describeTo(description: Description?) {
                with(rule.density) {
                    description?.appendText(
                        "with expected position: " +
                            "${expectedPositionX.value}, ${expectedPositionY.value} " +
                            "but position found:" +
                            "${positionFound.x.toDp().value}, ${positionFound.y.toDp().value}"
                    )
                }
            }
        }
    }
}

private fun ComposeTestRule.singleSelectionHandleMatches(viewMatcher: Matcher<in View>) {
    // Make sure that current measurement/drawing is finished
    waitForIdle()
    Espresso.onView(CoreMatchers.instanceOf(ViewRootForTest::class.java))
        .inRoot(SingleSelectionHandleMatcher())
        .check(ViewAssertions.matches(viewMatcher))
}

private class SingleSelectionHandleMatcher : TypeSafeMatcher<Root>() {

    var lastSeenWindowParams: WindowManager.LayoutParams? = null

    override fun describeTo(description: Description?) {
        description?.appendText("PopupLayoutMatcher")
    }

    override fun matchesSafely(item: Root?): Boolean {
        val matches = item != null && isPopupLayout(item.decorView)
        if (matches) {
            lastSeenWindowParams = item!!.windowLayoutParams.get()
        }
        return matches
    }
}