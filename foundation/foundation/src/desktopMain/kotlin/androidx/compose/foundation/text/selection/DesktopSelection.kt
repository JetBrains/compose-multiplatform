/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.compose.foundation.text.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.gesture.rawDragGestureFilter
import androidx.compose.ui.gesture.rawPressStartGestureFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.SelectionTrackerAmbient
import kotlin.math.max

@Composable
private fun Wrap(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        val width = placeables.fold(0) { maxWidth, placeable ->
            max(maxWidth, (placeable.width))
        }

        val height = placeables.fold(0) { minWidth, placeable ->
            max(minWidth, (placeable.height))
        }

        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.place(0, 0)
            }
        }
    }
}

@Composable
fun DesktopSelectionContainer(content: @Composable () -> Unit) {
    val selection = remember { mutableStateOf<Selection?>(null) }
    DesktopSelectionContainer(
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        content = content
    )
}

private fun Modifier.selectionFilter(observer: DragObserver): Modifier = composed {
    val glue = remember { DragGlue(observer) }
    rawDragGestureFilter(glue, glue::started)
        .rawPressStartGestureFilter(glue::startDrag, true)
}

private class DragGlue(val observer: DragObserver) : DragObserver by observer {
    var started = false

    fun startDrag(downPosition: Offset) {
        started = true
        observer.onStart(downPosition)
    }

    override fun onStop(velocity: Offset) {
        started = false
        observer.onStop(velocity)
    }

    override fun onCancel() {
        started = false
        observer.onCancel()
    }
}

@Composable
internal fun DesktopSelectionContainer(
    selection: Selection?,
    onSelectionChange: (Selection?) -> Unit,
    content: @Composable () -> Unit
) {
    val registrarImpl = remember { SelectionRegistrarImpl() }
    val manager = remember { DesktopSelectionManager(registrarImpl) }

    val selectionTracker = SelectionTrackerAmbient.current

    manager.onSelectionChange = {
        selectionTracker.getSelectedText = { manager.getSelectedText() }
        onSelectionChange(it)
    }
    manager.selection = selection

    val gestureModifiers =
        Modifier.selectionFilter(manager.observer)

    val modifier = remember {
        gestureModifiers.onGloballyPositioned {
            manager.containerLayoutCoordinates = it
        }
    }

    Providers(AmbientSelectionRegistrar provides registrarImpl) {
        Wrap(modifier) {
            content()
        }
    }
}