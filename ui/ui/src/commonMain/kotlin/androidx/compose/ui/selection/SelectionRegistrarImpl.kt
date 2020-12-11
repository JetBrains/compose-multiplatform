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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.ExperimentalTextApi

@OptIn(ExperimentalTextApi::class)
internal class SelectionRegistrarImpl : SelectionRegistrar {
    /**
     * A flag to check if the [Selectable]s have already been sorted.
     */
    internal var sorted: Boolean = false

    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    private val _selectables = mutableListOf<Selectable>()

    /**
     * Getter for handlers that returns an List.
     */
    internal val selectables: List<Selectable>
        get() = _selectables

    /**
     * The callback to be invoked when the position change was triggered.
     */
    internal var onPositionChangeCallback: (() -> Unit)? = null

    /**
     * The callback to be invoked when the selection is initiated.
     */
    internal var onSelectionUpdateStartCallback: ((LayoutCoordinates, Offset) -> Unit)? = null

    /**
     * The callback to be invoked when the selection is updated.
     */
    internal var onSelectionUpdateCallback: ((LayoutCoordinates, Offset, Offset) -> Unit)? = null

    /**
     * The callback to be invoked when selection update finished.
     */
    internal var onSelectionUpdateEndCallback: (() -> Unit)? = null

    /**
     * The callback to be invoked when one of the selectable has changed.
     */
    internal var onSelectableChangeCallback: ((Selectable) -> Unit)? = null

    override fun subscribe(selectable: Selectable): Selectable {
        _selectables.add(selectable)
        sorted = false
        return selectable
    }

    override fun unsubscribe(selectable: Selectable) {
        _selectables.remove(selectable)
    }

    /**
     * Sort the list of registered [Selectable]s in [SelectionRegistrar]. Currently the order of
     * selectables is geometric-based.
     */
    fun sort(containerLayoutCoordinates: LayoutCoordinates): List<Selectable> {
        if (!sorted) {
            // Sort selectables by y-coordinate first, and then x-coordinate, to match English
            // hand-writing habit.
            _selectables.sortWith(
                Comparator { a: Selectable, b: Selectable ->
                    val layoutCoordinatesA = a.getLayoutCoordinates()
                    val layoutCoordinatesB = b.getLayoutCoordinates()

                    val positionA =
                        if (layoutCoordinatesA != null) containerLayoutCoordinates.localPositionOf(
                            layoutCoordinatesA,
                            Offset.Zero
                        )
                        else Offset.Zero
                    val positionB =
                        if (layoutCoordinatesB != null) containerLayoutCoordinates.localPositionOf(
                            layoutCoordinatesB,
                            Offset.Zero
                        )
                        else Offset.Zero

                    if (positionA.y == positionB.y) compareValues(positionA.x, positionB.x)
                    else compareValues(positionA.y, positionB.y)
                }
            )
            sorted = true
        }
        return selectables
    }

    override fun notifyPositionChange() {
        // Set the variable sorted to be false, when the global position of a registered
        // selectable changes.
        sorted = false
        onPositionChangeCallback?.invoke()
    }

    override fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset
    ) {
        onSelectionUpdateStartCallback?.invoke(layoutCoordinates, startPosition)
    }

    override fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        endPosition: Offset
    ) {
        onSelectionUpdateCallback?.invoke(layoutCoordinates, startPosition, endPosition)
    }

    override fun notifySelectionUpdateEnd() {
        onSelectionUpdateEndCallback?.invoke()
    }

    override fun notifySelectableChange(selectable: Selectable) {
        onSelectableChangeCallback?.invoke(selectable)
    }
}