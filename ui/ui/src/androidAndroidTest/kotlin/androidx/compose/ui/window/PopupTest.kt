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
import androidx.compose.foundation.layout.Stack
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.compose.ui.selection.SimpleContainer
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.compose.ui.unit.IntBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Owner
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.platform.ViewAmbient
import com.google.common.truth.Truth
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(JUnit4::class)
@FlakyTest(bugId = 150214184)
class PopupTest {
    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)
    private val testTag = "testedPopup"

    private val parentBounds = IntBounds(50, 50, 150, 150)
    private val windowBounds = IntBounds(0, 0, 1000, 1000)
    private val offset = IntOffset(10, 10)
    private val parentSize = IntSize(parentBounds.width, parentBounds.height)
    private val popupSize = IntSize(40, 20)

    private var composeViewAbsolutePosition = IntOffset(0, 0)

    // TODO(b/140215440): Some tests are calling the OnChildPosition method inside the Popup too
    //  many times
    private fun createPopupWithAlignmentRule(
        alignment: Alignment,
        isRtl: Boolean = false
    ) {
        val measureLatch = CountDownLatch(1)

        with(composeTestRule.density) {
            val popupWidthDp = popupSize.width.toDp()
            val popupHeightDp = popupSize.height.toDp()
            val parentWidthDp = parentSize.width.toDp()
            val parentHeightDp = parentSize.height.toDp()

            composeTestRule.setContent {
                // Get the compose view position on screen
                val composeView = ViewAmbient.current
                val positionArray = IntArray(2)
                composeView.getLocationOnScreen(positionArray)
                composeViewAbsolutePosition = IntOffset(
                    positionArray[0],
                    positionArray[1]
                )

                // Align the parent of the popup on the top left corner, this results in the global
                // position of the parent to be (0, 0)
                TestAlign {
                    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                    Providers(LayoutDirectionAmbient provides layoutDirection) {
                        SimpleContainer(width = parentWidthDp, height = parentHeightDp) {
                            PopupTestTag(testTag) {
                                Popup(alignment = alignment, offset = offset) {
                                    // This is called after the OnChildPosition method in Popup() which
                                    // updates the popup to its final position
                                    SimpleContainer(
                                        width = popupWidthDp,
                                        height = popupHeightDp,
                                        modifier = Modifier.onPositioned {
                                            measureLatch.countDown()
                                        },
                                        children = emptyContent()
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

    // TODO(b/139861182): Remove all of this and provide helpers on ComposeTestRule
    private fun popupMatches(viewMatcher: Matcher<in View>) {
        // Make sure that current measurement/drawing is finished
        runOnIdle { }
        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(PopupLayoutMatcher())
            .check(matches(viewMatcher))
    }

    private inner class PopupLayoutMatcher : TypeSafeMatcher<Root>() {
        override fun describeTo(description: Description?) {
            description?.appendText("PopupLayoutMatcher")
        }

        // TODO(b/141101446): Find a way to match the window used by the popup
        override fun matchesSafely(item: Root?): Boolean {
            return item != null && isPopupLayout(
                item.decorView,
                testTag
            )
        }
    }

    private inner class PopupsCounterMatcher : TypeSafeMatcher<Root>() {
        var popupsFound = 0

        override fun describeTo(description: Description?) {
            description?.appendText("PopupLayoutMatcher")
        }

        // TODO(b/141101446): Find a way to match the window used by the popup
        override fun matchesSafely(item: Root?): Boolean {
            val isPopup = item != null && isPopupLayout(
                item.decorView,
                testTag
            )
            if (isPopup) {
                popupsFound++
            }
            return isPopup
        }
    }

    @Test
    fun popup_isShowing() {
        composeTestRule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(Modifier.preferredSize(50.dp), children = emptyContent())
                    }
                }
            }
        }

        popupMatches(isDisplayed())
    }

    @Test
    fun popup_hasActualSize() {
        val popupWidthDp = with(composeTestRule.density) {
            popupSize.width.toDp()
        }
        val popupHeightDp = with(composeTestRule.density) {
            popupSize.height.toDp()
        }

        composeTestRule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(
                            width = popupWidthDp,
                            height = popupHeightDp,
                            children = emptyContent()
                        )
                    }
                }
            }
        }

        popupMatches(matchesSize(popupSize.width, popupSize.height))
    }

    @Test
    fun changeParams_assertNoLeaks() {
        val measureLatch = CountDownLatch(1)
        var isFocusable by mutableStateOf(false)
        composeTestRule.setContent {
            Stack {
                PopupTestTag(testTag) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                        isFocusable = isFocusable
                    ) {
                        // This is called after the OnChildPosition method in Popup() which
                        // updates the popup to its final position
                        Stack(
                            modifier = Modifier.width(200.dp).height(200.dp).onPositioned {
                                measureLatch.countDown()
                            }
                        ) {}
                    }
                }
            }
        }
        measureLatch.await(1, TimeUnit.SECONDS)

        fun assertSinglePopupExists() {
            runOnIdle { }
            val counterMatcher = PopupsCounterMatcher()
            Espresso.onView(instanceOf(Owner::class.java))
                .inRoot(counterMatcher)
                .check(matches(isDisplayed()))

            Truth.assertThat(counterMatcher.popupsFound).isEqualTo(1)
        }

        assertSinglePopupExists()

        runOnUiThread {
            isFocusable = true
        }

        // If we have a leak, this will crash on multiple popups found
        assertSinglePopupExists()
    }

    @Test
    fun popup_correctPosition_alignmentTopStart() {
        /* Expected TopStart Position
           x = offset.x
           y = offset.y
        */
        val expectedPositionTopStart = IntOffset(10, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopStart)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopStart))
    }

    @Test
    fun popup_correctPosition_alignmentTopStart_rtl() {
        /* Expected TopStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopStart = IntOffset(50, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopStart, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopStart))
    }

    @Test
    fun popup_correctPosition_alignmentTopCenter() {
        /* Expected TopCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPositionTopCenter = IntOffset(40, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopCenter))
    }

    @Test
    fun popup_correctPosition_alignmentTopCenter_rtl() {
        /* Expected TopCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y
        */
        val expectedPositionTopCenter = IntOffset(20, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopCenter, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopCenter))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd() {
        /* Expected TopEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y
        */
        val expectedPositionTopEnd = IntOffset(70, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopEnd)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopEnd))
    }

    @Test
    fun popup_correctPosition_alignmentTopEnd_rtl() {
        /* Expected TopEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y
        */
        val expectedPositionTopEnd = IntOffset(0, 10)

        createPopupWithAlignmentRule(alignment = Alignment.TopEnd, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionTopEnd))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd() {
        /* Expected CenterEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntOffset(70, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterEnd)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterEnd))
    }

    @Test
    fun popup_correctPosition_alignmentCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntOffset(0, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterEnd, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterEnd))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd() {
        /* Expected BottomEnd Position
           x = offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntOffset(70, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomEnd)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomEnd))
    }

    @Test
    fun popup_correctPosition_alignmentBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = -offset.x falls back to zero if outside the screen
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntOffset(0, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomEnd, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomEnd))
    }

    @Test
    fun popup_correctPosition_alignmentBottomCenter() {
        /* Expected BottomCenter Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntOffset(40, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomCenter)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomCenter))
    }

    @Test
    fun popup_correctPosition_alignmentBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntOffset(20, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomCenter, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomCenter))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart() {
        /* Expected BottomStart Position
           x = offset.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntOffset(10, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomStart)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomStart))
    }

    @Test
    fun popup_correctPosition_alignmentBottomStart_rtl() {
        /* Expected BottomStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntOffset(50, 90)

        createPopupWithAlignmentRule(alignment = Alignment.BottomStart, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionBottomStart))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart() {
        /* Expected CenterStart Position
           x = offset.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntOffset(10, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterStart)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterStart))
    }

    @Test
    fun popup_correctPosition_alignmentCenterStart_rtl() {
        /* Expected CenterStart Position
           x = -offset.x + parentSize.x - popupSize.x
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntOffset(50, 50)

        createPopupWithAlignmentRule(alignment = Alignment.CenterStart, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenterStart))
    }

    @Test
    fun popup_correctPosition_alignmentCenter() {
        /* Expected Center Position
           x = offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntOffset(40, 50)

        createPopupWithAlignmentRule(alignment = Alignment.Center)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenter))
    }

    @Test
    fun popup_correctPosition_alignmentCenter_rtl() {
        /* Expected Center Position
           x = -offset.x + parentSize.x / 2 - popupSize.x / 2
           y = offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntOffset(20, 50)

        createPopupWithAlignmentRule(alignment = Alignment.Center, isRtl = true)

        popupMatches(matchesPosition(composeViewAbsolutePosition + expectedPositionCenter))
    }

    @Test
    fun popup_calculateGlobalPositionTopStart() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopStart = IntOffset(60, 60)

        val positionTopStart =
            AlignmentOffsetPositionProvider(
                Alignment.TopStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopStart).isEqualTo(expectedPositionTopStart)
    }

    @Test
    fun popup_calculateGlobalPositionTopStart_rtl() {
        /* Expected TopStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopStart = IntOffset(100, 60)

        val positionTopStart =
            AlignmentOffsetPositionProvider(
                Alignment.TopStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopStart).isEqualTo(expectedPositionTopStart)
    }

    @Test
    fun popup_calculateGlobalPositionTopCenter() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopCenter = IntOffset(90, 60)

        val positionTopCenter =
            AlignmentOffsetPositionProvider(
                Alignment.TopCenter,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopCenter).isEqualTo(expectedPositionTopCenter)
    }

    @Test
    fun popup_calculateGlobalPositionTopCenter_rtl() {
        /* Expected TopCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopCenter = IntOffset(70, 60)

        val positionTopCenter =
            AlignmentOffsetPositionProvider(
                Alignment.TopCenter,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopCenter).isEqualTo(expectedPositionTopCenter)
    }

    @Test
    fun popup_calculateGlobalPositionTopEnd() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopEnd = IntOffset(120, 60)

        val positionTopEnd =
            AlignmentOffsetPositionProvider(
                Alignment.TopEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionTopEnd).isEqualTo(expectedPositionTopEnd)
    }

    @Test
    fun popup_calculateGlobalPositionTopEnd_rtl() {
        /* Expected TopEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y
        */
        val expectedPositionTopEnd = IntOffset(40, 60)

        val positionTopEnd =
            AlignmentOffsetPositionProvider(
                Alignment.TopEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionTopEnd).isEqualTo(expectedPositionTopEnd)
    }

    @Test
    fun popup_calculateGlobalPositionCenterEnd() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + offset.x + parentSize.x - popupSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntOffset(120, 100)

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(
                Alignment.CenterEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenterEnd).isEqualTo(expectedPositionCenterEnd)
    }

    @Test
    fun popup_calculateGlobalPositionCenterEnd_rtl() {
        /* Expected CenterEnd Position
           x = parentGlobalPosition.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterEnd = IntOffset(40, 100)

        val positionCenterEnd =
            AlignmentOffsetPositionProvider(
                Alignment.CenterEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenterEnd).isEqualTo(expectedPositionCenterEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomEnd() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntOffset(120, 140)

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(
                Alignment.BottomEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomEnd).isEqualTo(expectedPositionBottomEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomEnd_rtl() {
        /* Expected BottomEnd Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomEnd = IntOffset(40, 140)

        val positionBottomEnd =
            AlignmentOffsetPositionProvider(
                Alignment.BottomEnd,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomEnd).isEqualTo(expectedPositionBottomEnd)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntOffset(90, 140)

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(
                Alignment.BottomCenter,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomCenter).isEqualTo(expectedPositionBottomCenter)
    }

    @Test
    fun popup_calculateGlobalPositionBottomCenter_rtl() {
        /* Expected BottomCenter Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomCenter = IntOffset(70, 140)

        val positionBottomCenter =
            AlignmentOffsetPositionProvider(
                Alignment.BottomCenter,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomCenter).isEqualTo(expectedPositionBottomCenter)
    }

    @Test
    fun popup_calculateGlobalPositionBottomStart() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntOffset(60, 140)

        val positionBottomStart =
            AlignmentOffsetPositionProvider(
                Alignment.BottomStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionBottomStart).isEqualTo(expectedPositionBottomStart)
    }

    @Test
    fun popup_calculateGlobalPositionBottomStart_rtl() {
        /* Expected BottomStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y - popupSize.y
        */
        val expectedPositionBottomStart = IntOffset(100, 140)

        val positionBottomStart =
            AlignmentOffsetPositionProvider(
                Alignment.BottomStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionBottomStart).isEqualTo(expectedPositionBottomStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenterStart() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntOffset(60, 100)

        val positionCenterStart =
            AlignmentOffsetPositionProvider(
                Alignment.CenterStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenterStart).isEqualTo(expectedPositionCenterStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenterStart_rtl() {
        /* Expected CenterStart Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenterStart = IntOffset(100, 100)

        val positionCenterStart =
            AlignmentOffsetPositionProvider(
                Alignment.CenterStart,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenterStart).isEqualTo(expectedPositionCenterStart)
    }

    @Test
    fun popup_calculateGlobalPositionCenter() {
        /* Expected Center Position
           x = parentGlobalPosition.x + offset.x + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntOffset(90, 100)

        val positionCenter =
            AlignmentOffsetPositionProvider(
                Alignment.Center,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    @Test
    fun popup_calculateGlobalPositionCenter_rtl() {
        /* Expected Center Position
           x = parentGlobalPosition.x + (-offset.x) + parentSize.x / 2 - popupSize.x / 2
           y = parentGlobalPosition.y + offset.y + parentSize.y / 2 - popupSize.y / 2
        */
        val expectedPositionCenter = IntOffset(70, 100)

        val positionCenter =
            AlignmentOffsetPositionProvider(
                Alignment.Center,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionCenter).isEqualTo(expectedPositionCenter)
    }

    @Test
    fun popup_hasViewTreeLifecycleOwner() {
        composeTestRule.setContent {
            PopupTestTag(testTag) {
                Popup {}
            }
        }

        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(PopupLayoutMatcher())
            .check(matches(object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description?) {
                    description?.appendText("ViewTreeLifecycleOwner.get(view) != null")
                }

                override fun matchesSafely(item: View): Boolean {
                    return ViewTreeLifecycleOwner.get(item) != null
                }
            }))
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionStart() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + offset.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionLeft = IntOffset(60, 160)

        val positionLeft =
            DropdownPositionProvider(
                DropDownAlignment.Start,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionLeft).isEqualTo(expectedPositionLeft)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionStart_rtl() {
        /* Expected Dropdown Start Position
           x = parentGlobalPosition.x + parentSize.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPosition = IntOffset(100, 160)

        val positionLeft =
            DropdownPositionProvider(
                DropDownAlignment.Start,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionLeft).isEqualTo(expectedPosition)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionEnd() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x + offset.x + parentSize.x
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionRight = IntOffset(160, 160)

        val positionRight =
            DropdownPositionProvider(
                DropDownAlignment.End,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Ltr,
                popupSize
            )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    @Test
    fun dropdownAlignment_calculateGlobalPositionEnd_rtl() {
        /* Expected Dropdown End Position
           x = parentGlobalPosition.x - popupSize.x + (-offset.x)
           y = parentGlobalPosition.y + offset.y + parentSize.y
        */
        val expectedPositionRight = IntOffset(0, 160)

        val positionRight =
            DropdownPositionProvider(
                DropDownAlignment.End,
                offset
            ).calculatePosition(
                parentBounds,
                windowBounds,
                LayoutDirection.Rtl,
                popupSize
            )

        Truth.assertThat(positionRight).isEqualTo(expectedPositionRight)
    }

    @Test
    fun popup_preservesAmbients() {
        val ambient = ambientOf<Float>()
        var value = 0f
        composeTestRule.setContent {
            Providers(ambient provides 1f) {
                Popup {
                    value = ambient.current
                }
            }
        }
        runOnIdle {
            Truth.assertThat(value).isEqualTo(1f)
        }
    }

    private fun matchesAndroidComposeView(): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return (item is Owner)
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with no AndroidComposeView")
            }
        }
    }

    private fun matchesSize(width: Int, height: Int): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return item?.width == width && item.height == height
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with width = $width height = $height")
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

@Composable
private fun TestAlign(children: @Composable () -> Unit) {
    Layout(children) { measurables, constraints ->
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
                    IntSize(layoutWidth - placeable.width, layoutHeight - placeable.height)
                )
                placeable.placeRelative(position.x, position.y)
            }
        }
    }
}
