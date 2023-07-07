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

package androidx.compose.ui.window

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.hamcrest.Description
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupAlignmentTest {

    @get:Rule
    val rule = createComposeRule()

    private val testTag = "testedPopup"
    private val offset = IntOffset(10, 10)
    private val popupSize = IntSize(40, 20)
    private val parentBounds = IntRect(50, 50, 150, 150)
    private val parentSize = IntSize(parentBounds.width, parentBounds.height)

    private var composeViewAbsolutePos = IntOffset(0, 0)

    @Test
    fun popup_correctPosition_alignmentTopStart() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        val expectedPosition = IntOffset(10, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopStart)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentTopStart_rtl() {
        /* Expected TopStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPosition = IntOffset(50, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopStart, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentTopCenter() {
        /* Expected TopCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPosition = IntOffset(40, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentTopCenter_rtl() {
        /* Expected TopCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPosition = IntOffset(20, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd() {
        /* Expected TopEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPosition = IntOffset(70, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopEnd)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd_rtl() {
        /* Expected TopEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y
        */
        val expectedPosition = IntOffset(0, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopEnd, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd() {
        /* Expected CenterEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(70, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterEnd)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(0, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterEnd, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd() {
        /* Expected BottomEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(70, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomEnd)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(0, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomEnd, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomCenter() {
        /* Expected BottomCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(40, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomCenter)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(20, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomCenter, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart() {
        /* Expected BottomStart Position
           x = offset.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(10, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomStart)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart_rtl() {
        /* Expected BottomStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPosition = IntOffset(50, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomStart, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart() {
        /* Expected CenterStart Position
           x = offset.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(10, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterStart)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart_rtl() {
        /* Expected CenterStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(50, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterStart, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenter() {
        /* Expected Center Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(40, 50)

        createPopupWithAlignmentRule(alignment = Alignment.Center)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    @Test
    fun popup_correctPosition_alignmentCenter_rtl() {
        /* Expected Center Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPosition = IntOffset(20, 50)

        createPopupWithAlignmentRule(alignment = Alignment.Center, isRtl = true)

        rule.popupMatches(testTag, matchesPosition(composeViewAbsolutePos + expectedPosition))
    }

    // TODO(b/140215440): Some tests are calling the OnChildPosition method inside the Popup too
    //  many times
    private fun createPopupWithAlignmentRule(
        alignment: Alignment,
        isRtl: Boolean = false
    ) {
        val measureLatch = CountDownLatch(1)

        with(rule.density) {
            val popupWidthDp = popupSize.width.toDp()
            val popupHeightDp = popupSize.height.toDp()
            val parentWidthDp = parentSize.width.toDp()
            val parentHeightDp = parentSize.height.toDp()

            rule.setContent {
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
                TestAlign {
                    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        SimpleContainer(width = parentWidthDp, height = parentHeightDp) {
                            PopupTestTag(testTag) {
                                Popup(alignment = alignment, offset = offset) {
                                    // This is called after the OnChildPosition method in Popup() which
                                    // updates the popup to its final position
                                    SimpleContainer(
                                        width = popupWidthDp,
                                        height = popupHeightDp,
                                        modifier = Modifier.onGloballyPositioned {
                                            measureLatch.countDown()
                                        },
                                        content = {}
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        measureLatch.await(1, TimeUnit.SECONDS)
    }

    @Composable
    private fun TestAlign(content: @Composable () -> Unit) {
        Layout(content) { measurables, constraints ->
            val measurable = measurables.firstOrNull()
            // The child cannot be larger than our max constraints, but we ignore min constraints.
            val placeable = measurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

            // The layout is as large as possible for bounded constraints,
            // or wrap content otherwise.
            val layoutWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                placeable?.width ?: constraints.minWidth
            }
            val layoutHeight = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                placeable?.height ?: constraints.minHeight
            }

            layout(layoutWidth, layoutHeight) {
                if (placeable != null) {
                    val position = Alignment.TopStart.align(
                        IntSize(placeable.width, placeable.height),
                        IntSize(layoutWidth, layoutHeight),
                        layoutDirection
                    )
                    placeable.placeRelative(position.x, position.y)
                }
            }
        }
    }

    private fun matchesPosition(expectedPosition: IntOffset): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            // (-1, -1) no position found
            var positionFound = IntOffset(-1, -1)

            override fun matchesSafely(item: View?): Boolean {
                val position = IntArray(2)
                item?.getLocationOnScreen(position)
                positionFound = IntOffset(position[0], position[1])

                return expectedPosition == positionFound
            }

            override fun describeTo(description: Description?) {
                description?.appendText(
                    "with expected position: $expectedPosition but position found: $positionFound"
                )
            }
        }
    }
}