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

import androidx.compose.foundation.text.ContextMenuArea
import androidx.compose.foundation.text.detectDownAndDragGesturesWithObserver
import androidx.compose.foundation.text.isInTouchMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.util.fastForEach

/**
 * Enables text selection for it's direct or indirection children.
 *
 * @sample androidx.compose.foundation.samples.SelectionSample
 */
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
 * @sample androidx.compose.foundation.samples.DisableSelectionSample
 */
@Composable
fun DisableSelection(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSelectionRegistrar provides null,
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
@Composable
internal fun SelectionContainer(
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

    manager.hapticFeedBack = LocalHapticFeedback.current
    manager.clipboardManager = LocalClipboardManager.current
    manager.textToolbar = LocalTextToolbar.current
    manager.onSelectionChange = onSelectionChange
    manager.selection = selection
    manager.touchMode = isInTouchMode

    ContextMenuArea(manager) {
        CompositionLocalProvider(LocalSelectionRegistrar provides registrarImpl) {
            // Get the layout coordinates of the selection container. This is for hit test of
            // cross-composable selection.
            SimpleLayout(modifier = modifier.then(manager.modifier)) {
                children()
                if (isInTouchMode && manager.hasFocus) {
                    manager.selection?.let {
                        listOf(true, false).fastForEach { isStartHandle ->
                            val observer = remember(isStartHandle) {
                                manager.handleDragObserver(isStartHandle)
                            }
                            val position = if (isStartHandle) {
                                manager.startHandlePosition
                            } else {
                                manager.endHandlePosition
                            }

                            val direction = if (isStartHandle) {
                                it.start.direction
                            } else {
                                it.end.direction
                            }

                            if (position != null) {
                                SelectionHandle(
                                    position = position,
                                    isStartHandle = isStartHandle,
                                    direction = direction,
                                    handlesCrossed = it.handlesCrossed,
                                    modifier = Modifier.pointerInput(observer) {
                                        detectDownAndDragGesturesWithObserver(observer)
                                    },
                                    content = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(manager) {
        onDispose {
            manager.hideSelectionToolbar()
        }
    }
}
