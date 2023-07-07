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

package androidx.compose.ui.demos.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputFilter
import androidx.compose.ui.input.pointer.PointerInputModifier
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * Demonstration of how various press/tap gesture interact together in a nested fashion.
 */
@Composable
fun PointerInputDuringSubComp() {
    Column {
        Text(
            "Demonstrates that PointerInputFilters that are currently receiving pointer input " +
                "events can be removed from the hierarchy by sub composition with no difficulty"
        )
        Text(
            "Below is an AdapterList with many touchable items.  Each item keeps track of the " +
                "number of pointers touching it.  If you touch an item and then scroll so " +
                "that it goes out of the viewport and then back into the viewport, you will" +
                " see that it no longer knows that a finger is touching it.  That is because " +
                "it is actually a new item that has not been hit tested yet.  If you keep " +
                "your finger there and then add more fingers, it will track those new fingers."
        )
        LazyColumn(
            Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .requiredSize(200.dp)
                .background(color = Color.White)
        ) {
            items(100) {
                val pointerCount = remember { mutableStateOf(0) }

                Box(
                    Modifier.fillParentMaxSize()
                        .border(width = 1.dp, color = Color.Black)
                        .pointerCounterGestureFilter { newCount -> pointerCount.value = newCount },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${pointerCount.value}",
                        fontSize = 16.em,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

fun Modifier.pointerCounterGestureFilter(
    onPointerCountChanged: (Int) -> Unit
): Modifier =
    composed {
        val filter = remember { PointerCounterGestureFilter() }
        filter.onPointerCountChanged = onPointerCountChanged
        PointerInputModifierImpl(filter)
    }

internal class PointerInputModifierImpl(override val pointerInputFilter: PointerInputFilter) :
    PointerInputModifier

internal class PointerCounterGestureFilter : PointerInputFilter() {

    lateinit var onPointerCountChanged: (resultingPointerCount: Int) -> Unit

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        val changes = pointerEvent.changes

        if (pass == PointerEventPass.Main) {
            if (changes.any {
                it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
            }
            ) {
                onPointerCountChanged.invoke(changes.count { it.pressed })
            }
        }
    }

    override fun onCancel() {}
}
