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

package androidx.compose.ui.viewinterop

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.ModifierLocalNestedScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.modifier.modifierLocalProvider
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.R
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class NestedScrollInteropConnectionTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()
    private val deltaCollectorNestedScrollConnection = InspectableNestedScrollConnection()

    private val nestedScrollParentView by lazy {
        rule.activity.findViewById<TestNestedScrollParentView>(R.id.main_layout)
    }

    private val items = (1..300).map { it.toString() }
    private val topItemTag = items.first()
    private val appBarExpandedSize = 240.dp
    private val appBarCollapsedSize = 54.dp
    private val appBarScrollDelta = appBarExpandedSize - appBarCollapsedSize
    private val completelyCollapsedScroll = 600.dp

    @Before
    fun setUp() {
        deltaCollectorNestedScrollConnection.reset()
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldScrollViewToo() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp()
        }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldPropagateConsumedDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection)
            )
        }

        // act: small scroll, everything will be consumed by view side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y + 100),
                endVelocity = 0f
            )
        }

        // assert: check delta on view side
        rule.runOnIdle {
            assertThat(
                abs(
                    nestedScrollParentView.offeredToParentOffset.y -
                        deltaCollectorNestedScrollConnection.offeredFromChild.y
                )
            ).isAtMost(ScrollRoundingErrorTolerance)

            assertThat(deltaCollectorNestedScrollConnection.consumedDownChain)
                .isEqualTo(Offset.Zero)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun swipeNoOpComposeScrollable_insideNestedScrollingParentView_shouldNotScrollView() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items, Modifier.modifierLocalProvider(ModifierLocalNestedScroll) { null })
        }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp()
        }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
        // assert: toolbar  not collapsed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun swipeTurnOffNestedInterop_insideNestedScrollingParentView_shouldNotScrollView() {
        // arrange
        createViewComposeActivity(enableInterop = false) { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp()
        }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertDoesNotExist()
        // assert: toolbar  not collapsed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun swipeComposeUpAndDown_insideNestedScrollingParentView_shouldPutViewToStartingPosition() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp()
            swipeDown()
        }

        // assert: compose list is scrolled
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(isDisplayed()))
    }

    @Test
    fun swipeComposeScrollable_byAppBarSize_shouldCollapseToolbar() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act: scroll compose side by the height of the toolbar
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - appBarExpandedSize.roundToPx()),
                endVelocity = 0f
            )
        }

        // assert: compose list is scrolled just enough to collapse toolbar
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
    }

    @Test
    fun swipeNestedScrollingParentView_hasComposeScrollable_shouldNotScrollElement() {
        // arrange
        createViewComposeActivity { TestListWithNestedScroll(items) }

        // act
        onView(withId(R.id.app_bar)).perform(click(), swipeUp())

        // assert: toolbar is collapsed
        onView(withId(R.id.fab)).check(matches(not(isDisplayed())))
        // assert: compose list is not scrolled
        rule.onNodeWithTag(topItemTag).assertIsDisplayed()
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollingParentView_shouldPropagateCorrectPreDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection)
            )
        }

        // act: split scroll, some will be consumed by view and the rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - completelyCollapsedScroll.roundToPx()),
                endVelocity = 0f
            )
        }

        // assert: check that compose presented the correct delta values to view
        rule.runOnIdle {
            val appBarScrollDeltaPixels = appBarScrollDelta.value * rule.density.density * -1
            val offeredToParent = nestedScrollParentView.offeredToParentOffset.y
            val availableToParent = deltaCollectorNestedScrollConnection.offeredFromChild.y +
                appBarScrollDeltaPixels
            assertThat(offeredToParent - availableToParent).isAtMost(ScrollRoundingErrorTolerance)
        }
    }

    @Test
    fun swipingComposeScrollable_insideNestedScrollingParentView_shouldPropagateCorrectPostDelta() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection)
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeWithVelocity(
                start = center,
                end = Offset(center.x, center.y - completelyCollapsedScroll.roundToPx()),
                endVelocity = 0f
            )
        }

        // assert: check that whatever that is unconsumed by view was consumed by children
        val unconsumedInView = nestedScrollParentView.unconsumedOffset.round().y
        val consumedByChildren = deltaCollectorNestedScrollConnection.consumedDownChain.round().y
        rule.runOnIdle {
            assertThat(abs(unconsumedInView - consumedByChildren))
                .isAtMost(ScrollRoundingErrorTolerance)
        }
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldPropagateCorrectPreVelocity() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection)
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(
                startY = center.y,
                endY = center.y - completelyCollapsedScroll.roundToPx(),
                durationMillis = 200
            )
        }

        // assert: check that whatever that is unconsumed by view was consumed by children
        val velocityOfferedInView = abs(nestedScrollParentView.velocityOfferedToParentOffset.y)
        val velocityAvailableInCompose =
            abs(deltaCollectorNestedScrollConnection.velocityOfferedFromChild.y)
        rule.runOnIdle {
            assertThat(velocityOfferedInView - velocityAvailableInCompose).isEqualTo(
                VelocityRoundingErrorTolerance
            )
        }
    }

    @Test
    fun swipeComposeScrollable_insideNestedScrollParentView_shouldPropagateCorrectPostVelocity() {
        // arrange
        createViewComposeActivity {
            TestListWithNestedScroll(
                items,
                Modifier.nestedScroll(deltaCollectorNestedScrollConnection)
            )
        }

        // act: split scroll, some will be consumed by view rest by compose
        rule.onNodeWithTag(MainListTestTag).performTouchInput {
            swipeUp(
                startY = center.y,
                endY = center.y - completelyCollapsedScroll.roundToPx(),
                durationMillis = 200
            )
        }

        // assert: check that whatever that is unconsumed by view was consumed by children
        val velocityUnconsumedOffset = abs(nestedScrollParentView.velocityUnconsumedOffset.y)
        val velocityConsumedByChildren =
            abs(deltaCollectorNestedScrollConnection.velocityConsumedDownChain.y)
        rule.runOnIdle {
            assertThat(abs(velocityUnconsumedOffset - velocityConsumedByChildren)).isAtMost(
                VelocityRoundingErrorTolerance
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun createViewComposeActivity(
        enableInterop: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule
            .activityRule
            .scenario
            .createActivityWithComposeContent(
                layout = R.layout.test_nested_scroll_coordinator_layout,
                enableInterop = enableInterop,
                content = content
            )
    }
}

private const val ScrollRoundingErrorTolerance = 10
private const val VelocityRoundingErrorTolerance = 0
private const val MainListTestTag = "MainListTestTag"

@Composable
private fun TestListWithNestedScroll(items: List<String>, modifier: Modifier = Modifier) {
    Box(modifier) {
        LazyColumn(Modifier.testTag(MainListTestTag)) {
            items(items) { TestItem(it) }
        }
    }
}

@Composable
private fun TestItem(item: String) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .height(56.dp)
            .fillMaxWidth()
            .testTag(item), contentAlignment = Alignment.Center
    ) {
        BasicText(item)
    }
}
