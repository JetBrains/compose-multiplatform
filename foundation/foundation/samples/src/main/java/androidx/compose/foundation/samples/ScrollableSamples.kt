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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Sampled
@Composable
fun ScrollableSample() {
    // actual composable state that we will show on UI and update in `Scrollable`
    val offset = remember { mutableStateOf(0f) }
    Box(
        Modifier
            .size(150.dp)
            .scrollable(
                orientation = Orientation.Vertical,
                // state for Scrollable, describes how consume scroll amount
                state = rememberScrollableState { delta ->
                    // use the scroll data and indicate how much this element consumed.
                    // unconsumed deltas will be propagated to nested scrollables (if present)
                    offset.value = offset.value + delta // update the state
                    delta // indicate that we consumed all the pixels available
                }
            )
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        // Modifier.scrollable is not opinionated about its children's layouts. It will however
        // promote nested scrolling capabilities if those children also use the modifier.
        // The modifier will not change any layouts so one must handle any desired changes through
        // the delta values in the scrollable state
        Text(offset.value.roundToInt().toString(), style = TextStyle(fontSize = 32.sp))
    }
}

@Sampled
@Composable
fun CanScrollSample() {
    val state = rememberLazyListState()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.KeyboardArrowUp,
            null,
            Modifier.graphicsLayer {
                // Hide the icon if we cannot scroll backward (we are the start of the list)
                // We use graphicsLayer here to control the alpha so that we only redraw when this
                // value changes, instead of recomposing
                alpha = if (state.canScrollBackward) 1f else 0f
            },
            Color.Red
        )
        val items = (1..100).toList()
        LazyColumn(
            Modifier
                .weight(1f)
                .fillMaxWidth(), state
        ) {
            items(items) {
                Text("Item is $it")
            }
        }
        Icon(
            Icons.Filled.KeyboardArrowDown,
            null,
            Modifier.graphicsLayer {
                // Hide the icon if we cannot scroll forward (we are the end of the list)
                // We use graphicsLayer here to control the alpha so that we only redraw when this
                // value changes, instead of recomposing
                alpha = if (state.canScrollForward) 1f else 0f
            },
            Color.Red
        )
    }
}
