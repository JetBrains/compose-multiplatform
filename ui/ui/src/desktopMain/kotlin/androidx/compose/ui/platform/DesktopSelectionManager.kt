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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.gesture.DragObserver
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.selection.Selectable
import androidx.compose.ui.selection.Selection

internal class DesktopSelectionManager(private val selectionRegistrar: DesktopSelectionRegistrar) {
    private var dragBeginPosition = Offset.Zero

    private var dragTotalDistance = Offset.Zero

    var containerLayoutCoordinates: LayoutCoordinates? = null

    var clipboardManager: ClipboardManager? = null
    var onSelectionChange: (Selection?) -> Unit = {}
    var selection: Selection? = null

    val observer = Observer()

    inner class Observer : DragObserver {
        override fun onStart(downPosition: Offset) {
            mergeSelections(
                startPosition = Offset(-1f, -1f),
                endPosition = Offset(-1f, -1f),
                previousSelection = selection
            )
            if (selection != null) onSelectionChange(null)
            dragBeginPosition = downPosition
            dragTotalDistance = Offset.Zero
        }

        override fun onDrag(dragDistance: Offset): Offset {
            dragTotalDistance += dragDistance
            val newSelection = mergeSelections(
                startPosition = dragBeginPosition,
                endPosition = dragBeginPosition + dragTotalDistance,
                previousSelection = selection
            )

            if (newSelection != selection) onSelectionChange(newSelection)
            return dragDistance
        }
    }

    internal fun mergeSelections(
        startPosition: Offset,
        endPosition: Offset,
        longPress: Boolean = false,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Selection? {

        val newSelection = selectionRegistrar.sort(requireContainerCoordinates())
            .fold(null) { mergedSelection: Selection?,
                          handler: Selectable ->
                merge(
                    mergedSelection,
                    handler.getSelection(
                        startPosition = startPosition,
                        endPosition = endPosition,
                        containerLayoutCoordinates = requireContainerCoordinates(),
                        longPress = longPress,
                        previousSelection = previousSelection,
                        isStartHandle = isStartHandle
                    )
                )
            }
        return newSelection
    }

    internal fun requireContainerCoordinates(): LayoutCoordinates {
        val coordinates = containerLayoutCoordinates
        require(coordinates != null)
        require(coordinates.isAttached)
        return coordinates
    }
}

private fun merge(lhs: Selection?, rhs: Selection?): Selection? {
    return lhs?.merge(rhs) ?: rhs
}