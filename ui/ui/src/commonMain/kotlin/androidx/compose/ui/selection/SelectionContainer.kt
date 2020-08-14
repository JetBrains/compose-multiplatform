/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.longPressDragGestureFilter
import androidx.compose.ui.gesture.noConsumptionTapGestureFilter
import androidx.compose.ui.onPositioned
import androidx.compose.ui.platform.ClipboardManagerAmbient
import androidx.compose.ui.platform.HapticFeedBackAmbient
import androidx.compose.ui.platform.TextToolbarAmbient
import androidx.compose.ui.text.InternalTextApi

/**
 * Default SelectionContainer to be used in order to make composables selectable by default.
 */
@Composable
internal fun SelectionContainer(modifier: Modifier, children: @Composable () -> Unit) {
    val selection = remember { mutableStateOf<Selection?>(null) }
    SelectionContainer(
        modifier = modifier,
        selection = selection.value,
        onSelectionChange = { selection.value = it },
        children = children
    )
}

/**
 * Selection Composable.
 *
 * The selection composable wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@OptIn(InternalTextApi::class)
@Composable
fun SelectionContainer(
    /** A [Modifier] for SelectionContainer. */
    modifier: Modifier = Modifier,
    /** Current Selection status.*/
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    children: @Composable () -> Unit
) {
    val registrarImpl = remember { SelectionRegistrarImpl() }
    val manager = remember { SelectionManager(registrarImpl) }

    manager.hapticFeedBack = HapticFeedBackAmbient.current
    manager.clipboardManager = ClipboardManagerAmbient.current
    manager.textToolbar = TextToolbarAmbient.current
    manager.onSelectionChange = onSelectionChange
    manager.selection = selection

    val gestureModifiers = remember {
        // Get the layout coordinates of the selection container. This is for hit test of
        // cross-composable selection.
        Modifier
            .noConsumptionTapGestureFilter { manager.onRelease() }
            .longPressDragGestureFilter(manager.longPressDragObserver)
            .onPositioned { manager.containerLayoutCoordinates = it }
    }

    Providers(SelectionRegistrarAmbient provides registrarImpl) {
        // Get the layout coordinates of the selection container. This is for hit test of
        // cross-composable selection.
        SelectionLayout(
            modifier = modifier.then(gestureModifiers)
        ) {
            children()
            manager.selection?.let {
                for (isStartHandle in listOf(true, false)) {
                    SelectionHandleLayout(
                        startHandlePosition = manager.startHandlePosition,
                        endHandlePosition = manager.endHandlePosition,
                        isStartHandle = isStartHandle,
                        directions = Pair(it.start.direction, it.end.direction),
                        handlesCrossed = it.handlesCrossed
                    ) {
                        SelectionHandle(
                            modifier =
                            Modifier.dragGestureFilter(manager.handleDragObserver(isStartHandle)),
                            isStartHandle = isStartHandle,
                            directions = Pair(it.start.direction, it.end.direction),
                            handlesCrossed = it.handlesCrossed
                        )
                    }
                }
                SelectionFloatingToolBar(manager = manager)
            }
        }
    }
}

@Composable
private fun SelectionFloatingToolBar(manager: SelectionManager) {
    manager.showSelectionToolbar()
}
