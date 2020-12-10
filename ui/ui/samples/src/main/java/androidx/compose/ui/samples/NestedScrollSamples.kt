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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.ScrollCallback
import androidx.compose.ui.gesture.nestedscroll.NestedScrollConnection
import androidx.compose.ui.gesture.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.gesture.nestedscroll.NestedScrollSource
import androidx.compose.ui.gesture.nestedscroll.nestedScroll
import androidx.compose.ui.gesture.scrollGestureFilter
import androidx.compose.ui.gesture.scrollorientationlocking.Orientation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.minus
import kotlin.math.roundToInt

@Sampled
@Composable
fun NestedScrollSample() {
    // constructing the box with next that scrolls as long as text within 0 .. 300
    // to support nested scrolling, we need to scroll ourselves, dispatch nested scroll events
    // as we scroll, and listen to potential children when we're scrolling.
    val maxValue = 300f
    val minValue = 0f
    // our state that we update as scroll
    var value by remember { mutableStateOf(maxValue / 2) }
    // create dispatch to dispatch scroll events up to the nested scroll parents
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }
    // we're going to scroll vertically, so set the orientation to vertical
    val orientation = Orientation.Vertical

    // callback to listen to scroll events and dispatch nested scroll events
    val scrollCallback = remember {
        object : ScrollCallback {
            override fun onScroll(scrollDistance: Float): Float {
                // dispatch prescroll with Y axis since we're going vertical scroll
                val aboveConsumed = nestedScrollDispatcher.dispatchPreScroll(
                    Offset(x = 0f, y = scrollDistance),
                    NestedScrollSource.Drag
                )
                // adjust what we can consume according to pre-scroll
                val available = scrollDistance - aboveConsumed.y
                // let's calculate how much we want to consume and how much is left
                val newTotal = value + available
                val newValue = newTotal.coerceIn(minValue, maxValue)
                val toConsume = newValue - value
                val leftAfterUs = available - toConsume
                // consume ourselves what we need and dispatch "scroll" phase of nested scroll
                value += toConsume
                nestedScrollDispatcher.dispatchPostScroll(
                    Offset(x = 0f, y = toConsume),
                    Offset(x = 0f, y = leftAfterUs),
                    NestedScrollSource.Drag
                )
                // indicate to the old pointer that we handled everything by returning same value
                return scrollDistance
            }

            override fun onStop(velocity: Float) {
                // for simplicity we won't fling ourselves, but we need to respect nested scroll
                // dispatch pre fling
                val velocity2d = Velocity(Offset(x = 0f, y = velocity))
                val consumed = nestedScrollDispatcher.dispatchPreFling(velocity2d)
                // now, since we don't fling, we consume 0 (Offset.Zero).
                // Adjust what's left after prefling and dispatch post fling
                val left = velocity2d - consumed
                nestedScrollDispatcher.dispatchPostFling(Velocity.Zero, left)
            }
        }
    }

    // we also want to participate in the nested scrolling, not only dispatching. create connection
    val connection = remember {
        object : NestedScrollConnection {
            // let's assume we want to consume children's delta before them if we can
            // we should do it in pre scroll
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // calculate how much we can take from child
                val oldValue = value
                val newTotal = value + available.y
                val newValue = newTotal.coerceIn(minValue, maxValue)
                val toConsume = newValue - oldValue
                // consume what we want and report back co children can adjust
                value += toConsume
                return Offset(x = 0f, y = toConsume)
            }
        }
    }

    // scrollable parent to which we will dispatch our nested scroll events
    // Since we properly support scrolling above, this parent will scroll even if we scroll inner
    // box (with White background)
    LazyColumn(Modifier.background(Color.Red)) {
        // our box we constructed
        item {
            Box(
                Modifier
                    .size(width = 300.dp, height = 100.dp)
                    .background(Color.White)
                    // add scrolling listening and dispatching
                    .scrollGestureFilter(orientation = orientation, scrollCallback = scrollCallback)
                    // connect self connection and dispatcher to the nested scrolling system
                    .nestedScroll(connection, dispatcher = nestedScrollDispatcher)
            ) {
                // hypothetical scrollable child which we will listen in connection above
                LazyColumn {
                    items(listOf(1, 2, 3, 4, 5)) {
                        Text(
                            "Magenta text above will change first when you scroll me",
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                }
                // simply show our value. It will change when we scroll child list above, taking
                // child's scroll delta until we reach maxValue or minValue
                Text(
                    text = value.roundToInt().toString(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Magenta)
                )
            }
        }
        repeat(100) {
            item {
                Text(
                    "Outer scroll items are Yellow on Red parent",
                    modifier = Modifier.background(Color.Yellow).padding(5.dp)
                )
            }
        }
    }
}