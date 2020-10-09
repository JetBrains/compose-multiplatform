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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.noConsumptionTapGestureFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ClipboardManagerAmbient
import androidx.compose.ui.platform.HapticFeedBackAmbient
import androidx.compose.ui.platform.TextToolbarAmbient
import androidx.compose.ui.text.InternalTextApi

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

    val selectionContainerModifier = Modifier.composed {
        val gestureModifiers = Modifier.noConsumptionTapGestureFilter { manager.onRelease() }

        val positionedModifier = remember {
            // Get the layout coordinates of the selection container. This is for hit test of
            // cross-composable selection.
            Modifier.onGloballyPositioned { manager.containerLayoutCoordinates = it }
        }

        if (selection != null) {
            this.then(gestureModifiers).then(positionedModifier)
        } else {
            this.then(positionedModifier)
        }
    }

    Providers(SelectionRegistrarAmbient provides registrarImpl) {
        // Get the layout coordinates of the selection container. This is for hit test of
        // cross-composable selection.
        SimpleLayout(
            modifier = modifier.then(selectionContainerModifier)
        ) {
            children()
            manager.selection?.let {
                for (isStartHandle in listOf(true, false)) {
                    SelectionHandle(
                        startHandlePosition = manager.startHandlePosition,
                        endHandlePosition = manager.endHandlePosition,
                        isStartHandle = isStartHandle,
                        directions = Pair(it.start.direction, it.end.direction),
                        handlesCrossed = it.handlesCrossed,
                        modifier = Modifier.dragGestureFilter(
                            manager.handleDragObserver(
                                isStartHandle
                            )
                        ),
                        handle = null
                    )
                }
                SelectionFloatingToolBar(manager = manager)
            }
        }
    }
}

/**
 * This is for disabling selection for text when the text is inside a SelectionContainer.
 *
 * To use this, simply add this to wrap one or more text composables.
 */
@Composable
fun DisableSelection(content: @Composable () -> Unit) {
    Providers(
        SelectionRegistrarAmbient provides null,
        children = content
    )
}

@Composable
private fun SelectionFloatingToolBar(manager: SelectionManager) {
    manager.showSelectionToolbar()
}
