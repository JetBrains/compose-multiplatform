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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.gesture.dragGestureFilter
import androidx.compose.ui.gesture.noConsumptionTapGestureFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AmbientClipboardManager
import androidx.compose.ui.platform.AmbientHapticFeedback
import androidx.compose.ui.platform.AmbientTextToolbar
import androidx.compose.ui.text.InternalTextApi

/**
 * Enables text selection for it's direct or indirection children.
 *
 * @sample androidx.compose.ui.samples.SelectionSample
 */
@Suppress("DEPRECATION")
@OptIn(InternalTextApi::class)
@Composable
fun SelectionContainer(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var selection by remember { mutableStateOf<Selection?>(null) }
    SelectionContainer(
        modifier = modifier,
        selection = selection,
        onSelectionChange = {
            selection = it
        },
        children = content
    )
}

/**
 * Disables text selection for it's direct or indirection children. To use this, simply add this
 * to wrap one or more text composables.
 *
 * @sample androidx.compose.ui.samples.DisableSelectionSample
 */
@Composable
fun DisableSelection(content: @Composable () -> Unit) {
    Providers(
        AmbientSelectionRegistrar provides null,
        content = content
    )
}

/**
 * Selection Composable.
 *
 * The selection composable wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@Suppress("ComposableLambdaParameterNaming")
@OptIn(InternalTextApi::class)
@Deprecated("Please use SelectionContainer with no callback")
@InternalTextApi
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

    manager.hapticFeedBack = AmbientHapticFeedback.current
    manager.clipboardManager = AmbientClipboardManager.current
    manager.textToolbar = AmbientTextToolbar.current
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

    Providers(AmbientSelectionRegistrar provides registrarImpl) {
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
            }
        }
    }

    onDispose {
        manager.selection = null
        manager.hideSelectionToolbar()
    }
}
