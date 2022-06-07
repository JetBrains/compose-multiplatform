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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Velocity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
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
class NestedScrollInteropViewHolderTest {
    @get:Rule
    val rule = createComposeRule()

    private val connection = InspectableNestedScrollConnection()
    private val recyclerViewConsumptionTracker = RecyclerViewConsumptionTracker()

    @Before
    fun setUp() {
        connection.reset()
        recyclerViewConsumptionTracker.reset()
    }

    @Test
    fun nestedScrollInteropIsOff_shouldNotPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = false,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(
            scrollToPosition<NestedScrollInteropAdapter.SimpleTextViewHolder>(20)
        )

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_shouldPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_layout)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isNotEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagate() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild)
                .isEqualTo(recyclerViewConsumptionTracker.deltaConsumed)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostScroll() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.notConsumedByChild).isEqualTo(Offset.Zero)
            assertThat(connection.consumedDownChain)
                .isEqualTo(recyclerViewConsumptionTracker.deltaConsumed)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_consumedUpChain_checkDeltasCorrectlyPropagatePostScroll() {
        // arrange
        rule.setContent {
            val controller = rememberScrollableState { it }

            Box(modifier = Modifier.scrollable(controller, Orientation.Vertical)) {
                NestedScrollInteropWithView(
                    modifier = Modifier.nestedScroll(connection),
                    enabled = true,
                    recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
                )
            }
        }

        // act
        Espresso.onView(withId(R.id.main_list)).perform(
            swipeUp()
        )

        // assert
        rule.runOnIdle {
            assertThat(recyclerViewConsumptionTracker.deltaConsumed).isEqualTo(Offset.Zero)
            assertThat(connection.notConsumedByChild).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePreFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())
        rule.waitForIdle()
        // assert
        rule.runOnIdle {
            assertThat(abs(connection.velocityOfferedFromChild))
                .isEqualTo(abs(recyclerViewConsumptionTracker.velocityConsumed))
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true,
                recyclerViewConsumptionTracker = recyclerViewConsumptionTracker
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.velocityNotConsumedByChild).isEqualTo(Velocity.Zero)
            assertThat(connection.velocityConsumedDownChain)
                .isEqualTo(recyclerViewConsumptionTracker.velocityConsumed)
        }
    }
}