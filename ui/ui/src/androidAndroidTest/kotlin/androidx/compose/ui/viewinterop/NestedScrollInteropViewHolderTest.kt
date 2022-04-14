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

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.R
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnFlingListener
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

    private var consumedByRecyclerView = intArrayOf(0, 0)
    private var velocityConsumedByRecyclerView = intArrayOf(0, 0)
    private val connection = InspectableNestedScrollConnection()

    @Before
    fun setUp() {
        connection.reset()
        consumedByRecyclerView.fill(0)
        velocityConsumedByRecyclerView.fill(0)
    }

    @Test
    fun nestedScrollInteropIsOff_shouldNotPropagateDeltas() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = false
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
                enabled = true
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
                enabled = true
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.offeredFromChild).isEqualTo(consumedByRecyclerView.toOffset())
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostScroll() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.notConsumedByChild).isEqualTo(Offset.Zero)
            assertThat(connection.consumedDownChain).isEqualTo(consumedByRecyclerView.toOffset())
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
                    enabled = true
                )
            }
        }

        // act
        Espresso.onView(withId(R.id.main_list)).perform(
            swipeUp()
        )

        // assert
        rule.runOnIdle {
            assertThat(consumedByRecyclerView.toOffset()).isEqualTo(Offset.Zero)
            assertThat(connection.notConsumedByChild).isEqualTo(Offset.Zero)
        }
    }

    @Test
    fun nestedScrollInteropIsOff_threeFoldNestedScroll_shouldNotPropagateCorrectly() {
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
    fun nestedScrollInteropIsOn_threeFoldNestedScroll_shouldPropagateCorrectly() {
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
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePreFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())
        rule.waitForIdle()
        // assert
        rule.runOnIdle {
            assertThat(abs(connection.velocityOfferedFromChild))
                .isEqualTo(abs(velocityConsumedByRecyclerView.toComposeVelocity()))
        }
    }

    @Test
    fun nestedScrollInteropIsOn_checkDeltasCorrectlyPropagatePostFling() {
        // arrange
        rule.setContent {
            NestedScrollInteropWithView(
                modifier = Modifier.nestedScroll(connection),
                enabled = true
            )
        }

        // act
        onView(withId(R.id.main_list)).perform(swipeUp())

        // assert
        rule.runOnIdle {
            assertThat(connection.velocityNotConsumedByChild).isEqualTo(Velocity.Zero)
            assertThat(connection.velocityConsumedDownChain)
                .isEqualTo(velocityConsumedByRecyclerView.toComposeVelocity())
        }
    }

    @Composable
    private fun NestedScrollInteropWithView(
        modifier: Modifier = Modifier,
        enabled: Boolean
    ) {
        NestedScrollInteropTestApp(modifier) { context ->
            LayoutInflater.from(context)
                .inflate(R.layout.android_in_compose_nested_scroll_interop, null)
                .apply {
                    with(findViewById<RecyclerView>(R.id.main_list)) {
                        layoutManager = LinearLayoutManager(
                            context,
                            RecyclerView.VERTICAL,
                            false
                        )
                        adapter = NestedScrollInteropAdapter()
                        setOnScrollChangeListener { _, _, _, oldX, oldY ->
                            consumedByRecyclerView[0] += oldX
                            consumedByRecyclerView[1] += oldY
                        }
                        onFlingListener = object : OnFlingListener() {
                            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                                velocityConsumedByRecyclerView[0] += velocityX
                                velocityConsumedByRecyclerView[1] += velocityY
                                return false
                            }
                        }
                    }
                }.also {
                    ViewCompat.setNestedScrollingEnabled(it, enabled)
                }
        }
    }

    @Composable
    private fun NestedScrollDeepNested(modifier: Modifier, enabled: Boolean) {
        // Box (Compose) + AndroidView (View) +
        // ComposeInCooperatingViewNestedScrollInterop (Compose)
        NestedScrollInteropTestApp(modifier) { context ->
            LayoutInflater.from(context)
                .inflate(R.layout.test_nested_scroll_coordinator_layout_without_toolbar, null)
                .apply {
                    with(findViewById<ComposeView>(R.id.compose_view)) {
                        setContent {
                            ComposeInCooperatingViewNestedScrollInterop(this)
                        }
                    }
                }.also {
                    ViewCompat.setNestedScrollingEnabled(it, enabled)
                }
        }
    }
}

private const val MainTestList = "mainList"
private const val AndroidViewContainer = "androidView"

@Composable
internal fun NestedScrollInteropTestApp(
    modifier: Modifier = Modifier,
    content: (Context) -> View
) {
    Box(modifier.fillMaxSize()) {
        AndroidView(content, modifier = Modifier.testTag(AndroidViewContainer))
    }
}

private class NestedScrollInteropAdapter :
    RecyclerView.Adapter<NestedScrollInteropAdapter.SimpleTextViewHolder>() {
    val items = (1..200).map { it.toString() }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SimpleTextViewHolder {
        return SimpleTextViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.android_in_compose_nested_scroll_interop_list_item,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: SimpleTextViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class SimpleTextViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(item: String) {
            itemView.findViewById<TextView>(R.id.list_item).text = item
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ComposeInCooperatingViewNestedScrollInterop(composeView: ComposeView) {
    with(composeView) {
        LazyColumn(
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection(this))
                .testTag(MainTestList)
        ) {
            items(200) { item ->
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .height(56.dp)
                        .fillMaxWidth()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.toString())
                }
            }
        }
    }
}

private fun IntArray.toOffset() = Offset(this[0].toFloat(), this[1].toFloat())
private fun IntArray.toComposeVelocity() =
    Velocity((this[0] * -1).toFloat(), (this[1] * -1).toFloat())

private fun abs(velocity: Velocity) = Velocity(
    kotlin.math.abs(velocity.x),
    kotlin.math.abs(velocity.y)
)

class InspectableNestedScrollConnection() : NestedScrollConnection {
    var offeredFromChild = Offset.Zero
    var velocityOfferedFromChild = Velocity.Zero
    var consumedDownChain = Offset.Zero
    var velocityConsumedDownChain = Velocity.Zero
    var notConsumedByChild = Offset.Zero
    var velocityNotConsumedByChild = Velocity.Zero

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        offeredFromChild += available
        return Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        consumedDownChain += consumed
        notConsumedByChild += available
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        velocityOfferedFromChild += available
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        velocityConsumedDownChain += consumed
        velocityNotConsumedByChild += available
        return Velocity.Zero
    }

    fun reset() {
        offeredFromChild = Offset.Zero
        velocityOfferedFromChild = Velocity.Zero
        consumedDownChain = Offset.Zero
        velocityConsumedDownChain = Velocity.Zero
        notConsumedByChild = Offset.Zero
        velocityNotConsumedByChild = Velocity.Zero
    }
}