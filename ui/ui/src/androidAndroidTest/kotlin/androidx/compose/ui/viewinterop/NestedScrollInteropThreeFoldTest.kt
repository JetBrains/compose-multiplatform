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

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.round
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RequiresApi(Build.VERSION_CODES.M)
@RunWith(AndroidJUnit4::class)
class NestedScrollInteropThreeFoldTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val nestedScrollParentView by lazy {
        rule.activity.findViewById<TestNestedScrollParentView>(R.id.main_layout)
    }

    // Connection that sits after (below) the AndroidView
    private val connection = InspectableNestedScrollConnection()
    private val allConsumingConnection = AllConsumingInspectableConnection()

    // CVC = Compose + View + Compose
    // VCV = View + Compose + View
    @Before
    fun setUp() {
        connection.reset()
        allConsumingConnection.reset()
    }

    @Test
    fun nestedScrollInteropIsOff_CVC_shouldNotPropagateCorrectly() {
        // arrange
        rule.setContent {
            NestedScrollDeepNested(
                modifier = Modifier.nestedScroll(connection),
                enabled = false
            )
        }

        // act
        rule.onNodeWithTag(MainTestList).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOff_VCV_shouldNotPropagateCorrectly() {
        // arrange
        createViewComposeActivity(
            outerModifier = Modifier.nestedScroll(connection),
            enableInterop = false
        ) {
            RecyclerViewAndroidView(interopEnabled = false)
        }

        // act
        rule.onNodeWithTag(AndroidViewContainer).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_CVC_shouldPropagateCorrectly() {
        // arrange
        rule.setContent {
            NestedScrollDeepNested(
                modifier = Modifier.nestedScroll(connection),
                enabled = true
            )
        }

        // act
        rule.onNodeWithTag(MainTestList).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_VCV_shouldPropagateCorrectly() {
        // arrange
        createViewComposeActivity(
            outerModifier = Modifier.nestedScroll(connection),
            enableInterop = true
        ) {
            RecyclerViewAndroidView(interopEnabled = true)
        }

        // act
        rule.onNodeWithTag(AndroidViewContainer).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun threeFoldNestedScrollCVC_composeConsumes_shouldPropagateCorrectly() {
        // arrange
        rule.setContent {
            NestedScrollDeepNested(
                modifier = Modifier.nestedScroll(allConsumingConnection),
                enabled = true,
                connection = connection
            )
        }

        // act
        rule.onNodeWithTag(MainTestList).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(allConsumingConnection.offeredFromChild).isNotEqualTo(Offset.Zero)
            assertThat(connection.consumedDownChain.round()).isEqualTo(Offset.Zero.round())
        }
    }

    @Test
    fun threeFoldNestedScrollVCV_composeConsumes_shouldPropagateCorrectly() {
        // arrange
        createViewComposeActivity(
            outerModifier = Modifier.nestedScroll(allConsumingConnection),
            enableInterop = true
        ) {
            RecyclerViewAndroidView(interopEnabled = true)
        }

        // act
        rule.onNodeWithTag(AndroidViewContainer).performTouchInput {
            swipeUp()
        }

        // assert
        rule.waitForIdle()
        assertThat(allConsumingConnection.offeredFromChild).isNotEqualTo(Offset.Zero)
        onView(withId(R.id.fab))
            .check(matches((isDisplayed())))
    }

    @Test
    fun threeFoldNestedScrollCVC_composeDoesNotConsumes_shouldPropagateCorrectly() {
        // arrange
        val secondaryInspectableConnection = InspectableNestedScrollConnection()
        rule.setContent {
            NestedScrollDeepNested(
                modifier = Modifier
                    .nestedScroll(secondaryInspectableConnection),
                enabled = true,
                connection = connection
            )
        }

        // act
        rule.onNodeWithTag(MainTestList).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(secondaryInspectableConnection.offeredFromChild).isNotEqualTo(Offset.Zero)
            assertThat(connection.consumedDownChain).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun threeFoldNestedScrollCVC_composeDoesNotConsumes_checkDeltasAreCorrectForVelocity() {
        // arrange
        val secondaryInspectableConnection = InspectableNestedScrollConnection()
        rule.setContent {
            NestedScrollDeepNested(
                modifier = Modifier
                    .nestedScroll(secondaryInspectableConnection),
                enabled = true,
                connection = connection
            )
        }

        // act
        rule.onNodeWithTag(MainTestList).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(secondaryInspectableConnection.velocityOfferedFromChild).isEqualTo(
                connection.velocityConsumedDownChain
            )
        }
    }

    @Test
    fun threeFoldNestedScrollVCV_composeDoesNotConsumes_checkDeltasAreCorrectForVelocity() {
        // arrange
        createViewComposeActivity(
            outerModifier = Modifier.nestedScroll(connection),
            enableInterop = true
        ) {
            RecyclerViewAndroidView(interopEnabled = true)
        }

        // act
        rule.onNodeWithTag(AndroidViewContainer).performTouchInput {
            swipeUp()
        }

        // assert
        rule.runOnIdle {
            assertThat(abs(nestedScrollParentView.velocityOfferedToParentOffset)).isEqualTo(
                abs(connection.velocityConsumedDownChain)
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    private fun createViewComposeActivity(
        enableInterop: Boolean = true,
        outerModifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        rule
            .activityRule
            .scenario
            .createActivityWithComposeContent(
                layout = R.layout.test_nested_scroll_coordinator_layout,
                enableInterop = enableInterop,
                content = content,
                modifier = outerModifier
            )
    }
}