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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Sampled
@Composable
fun NestedScrollConnectionSample() {
    // here we use LazyColumn that has build-in nested scroll, but we want to act like a
    // parent for this LazyColumn and participate in its nested scroll.
    // Let's make a collapsing toolbar for LazyColumn
    val toolbarHeight = 48.dp
    val toolbarHeightPx = with(LocalDensity.current) { toolbarHeight.roundToPx().toFloat() }
    // our offset to collapse toolbar
    val toolbarOffsetHeightPx =

        remember { mutableStateOf(0f) }
    // now, let's create connection to the nested scroll system and listen to the scroll
    // happening inside child LazyColumn
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // try to consume before LazyColumn to collapse toolbar if needed, hence pre-scroll
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value = newOffset.coerceIn(-toolbarHeightPx, 0f)
                // here's the catch: let's pretend we consumed 0 in any case, since we want
                // LazyColumn to scroll anyway for good UX
                // We're basically watching scroll without taking it
                return Offset.Zero
            }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            // attach as a parent to the nested scroll system
            .nestedScroll(nestedScrollConnection)
    ) {
        // our list with build in nested scroll support that will notify us about its scroll
        LazyColumn(contentPadding = PaddingValues(top = toolbarHeight)) {
            items(100) { index ->
                Text("I'm item $index", modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        TopAppBar(
            modifier = Modifier
                .height(toolbarHeight)
                .offset { IntOffset(x = 0, y = toolbarOffsetHeightPx.value.roundToInt()) },
            title = { Text("toolbar offset is ${toolbarOffsetHeightPx.value}") }
        )
    }
}

@Sampled
@Composable
fun NestedScrollDispatcherSample() {
    // Let's take Modifier.draggable (which doesn't have nested scroll build in, unlike Modifier
    // .scrollable) and add nested scroll support our component that contains draggable

    // this will be a generic components that will work inside other nested scroll components.
    // put it inside LazyColumn or / Modifier.verticalScroll to see how they will interact

    // first, state and it's bounds
    val basicState = remember { mutableStateOf(0f) }
    val minBound = -100f
    val maxBound = 100f
    // lambda to update state and return amount consumed
    val onNewDelta: (Float) -> Float = { delta ->
        val oldState = basicState.value
        val newState = (basicState.value + delta).coerceIn(minBound, maxBound)
        basicState.value = newState
        newState - oldState
    }
    // create a dispatcher to dispatch nested scroll events (participate like a nested scroll child)
    val nestedScrollDispatcher = remember { NestedScrollDispatcher() }

    // create nested scroll connection to react to nested scroll events (participate like a parent)
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // we have no fling, so we're interested in the regular post scroll cycle
                // let's try to consume what's left if we need and return the amount consumed
                val vertical = available.y
                val weConsumed = onNewDelta(vertical)
                return Offset(x = 0f, y = weConsumed)
            }
        }
    }
    Box(
        Modifier
            .size(100.dp)
            .background(Color.LightGray)
            // attach ourselves to nested scroll system
            .nestedScroll(connection = nestedScrollConnection, dispatcher = nestedScrollDispatcher)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // here's regular drag. Let's be good citizens and ask parents first if they
                    // want to pre consume (it's a nested scroll contract)
                    val parentsConsumed = nestedScrollDispatcher.dispatchPreScroll(
                        available = Offset(x = 0f, y = delta),
                        source = NestedScrollSource.Drag
                    )
                    // adjust what's available to us since might have consumed smth
                    val adjustedAvailable = delta - parentsConsumed.y
                    // we consume
                    val weConsumed = onNewDelta(adjustedAvailable)
                    // dispatch as a post scroll what's left after pre-scroll and our consumption
                    val totalConsumed = Offset(x = 0f, y = weConsumed) + parentsConsumed
                    val left = adjustedAvailable - weConsumed
                    nestedScrollDispatcher.dispatchPostScroll(
                        consumed = totalConsumed,
                        available = Offset(x = 0f, y = left),
                        source = NestedScrollSource.Drag
                    )
                    // we won't dispatch pre/post fling events as we have no flinging here, but the
                    // idea is very similar:
                    // 1. dispatch pre fling, asking parents to pre consume
                    // 2. fling (while dispatching scroll events like above for any fling tick)
                    // 3. dispatch post fling, allowing parent to react to velocity left
                }
            )
    ) {
        Text(
            "State: ${basicState.value.roundToInt()}",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}